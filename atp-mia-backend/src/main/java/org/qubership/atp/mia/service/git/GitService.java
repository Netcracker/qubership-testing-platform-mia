/*
 *  Copyright 2024-2025 NetCracker Technology Corporation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.qubership.atp.mia.service.git;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.RemoteRefUpdate;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.qubership.atp.mia.exceptions.fileservice.IoException;
import org.qubership.atp.mia.exceptions.git.GitException;
import org.qubership.atp.mia.exceptions.git.GitRepoBuildException;
import org.qubership.atp.mia.exceptions.git.GitResetException;
import org.qubership.atp.mia.exceptions.git.GitValidationProjectNotFoundException;
import org.qubership.atp.mia.exceptions.git.GitValidationUserNotFoundException;
import org.qubership.atp.mia.exceptions.git.RepoMailNotSetException;
import org.qubership.atp.mia.exceptions.git.RepoPassNotSetException;
import org.qubership.atp.mia.exceptions.git.RepoUserNotSetException;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.exception.MiaException;
import org.qubership.atp.mia.model.impl.GitInfoError;
import org.qubership.atp.mia.model.impl.GitInfoResponse;
import org.qubership.atp.mia.service.execution.RestClientService;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GitService {

    private static final String GITFILE = ".git";

    @Value("${git.reposUserId:2202}")
    private String gitUserId;
    @Value("${git.reposUrl:https://git.somedomain.com/}")
    private String gitUrlPrefix;
    @Value("${git.reposApiPath:api/v4/projects/%s/members/%s}")
    private String gitUrlPath;
    @Value("${git.reposUser}")
    private String reposUser;
    @Value("${git.reposPass}")
    private String reposPass;
    @Value("${git.reposEmail}")
    private String reposEmail;
    @Value("${git.reposToken}")
    private String gitToken;
    @Value("${git.defaultAccessLevel:40}")
    private String defaultAccessLevel;

    private Pattern repositoryNamePattern;

    private Git git;

    /**
     * Check whether x_kube2vcs user is present in git repository members
     * with maintainer rights.
     *
     * @param pathToGit url to git
     * @return empty string when success, otherwise validation error description.
     */
    public String checkXKubeUserRights(String pathToGit) {
        if (!Strings.isNullOrEmpty(pathToGit) && pathToGit.contains("http") && !Strings.isNullOrEmpty(gitToken)) {
            try {
                Optional<GitInfoResponse> response = executeGetAndParseGitInfoResponse(pathToGit, gitToken);
                if (response.isPresent()) {
                    int accessLevel = response.get().getAccessLevel();
                    int level = (int) Utils.parseLongValueOrDefault(defaultAccessLevel, 40, "accessLevel");
                    if (accessLevel < level) {
                        return ErrorCodes.MIA_0115_GIT_VALIDATION_USER_NO_RIGHTS.getMessage(pathToGit);
                    }
                } else {
                    return ErrorCodes.MIA_0116_GIT_VALIDATION_UNEXPECTED_ERROR.getMessage(pathToGit);
                }
            } catch (MiaException e) {
                return e.getMessage();
            }
        } else {
            log.debug("Can't check x_kube2vcs user rights, because url or private token is empty");
        }
        return "";
    }

    /**
     * Downloads GIT repository.
     *
     * @param repoUrl repository URL
     * @param toPath  local path
     */
    public void downloadGitRepo(String repoUrl, Path toPath) {
        File fileToPath = toPath.toFile();
        try (final Repository repository = getGitRepo(toPath).getRepository()) {
            final String gitRepoUrl = repository.getConfig().getString("remote", "origin", "url");
            if (toPath.resolve(GITFILE).toFile().exists() && gitRepoUrl != null && gitRepoUrl.equals(repoUrl)) {
                gitPull(toPath);
            } else {
                if (fileToPath.exists()) {
                    FileUtils.deleteDirectory(fileToPath);
                }
                FileUtils.forceMkdir(fileToPath);
                gitClone(repoUrl, fileToPath);
            }
        } catch (IOException e) {
            throw new IoException(fileToPath, e);
        }
    }

    /**
     * Makes http request to git repository with config and return x_kube2vcs user info.
     *
     * @param urlSrc       to git
     * @param privateToken private token for git API, need to be generated in x_kube2vcs account.
     * @return parsed JSON object as {@link GitInfoResponse}.
     * @throws MiaException - when git API returns error.
     */
    public Optional<GitInfoResponse> executeGetAndParseGitInfoResponse(String urlSrc, String privateToken) {
        HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setSSLContext(RestClientService.getSslContext())
                .disableRedirectHandling()
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        final String url = getGitEncodedUrl(urlSrc.endsWith("/")
                ? urlSrc.substring(0, urlSrc.length() - 1)
                : urlSrc);
        HttpGet request = new HttpGet(url);
        request.setHeader("PRIVATE-TOKEN", privateToken);
        try {
            HttpEntity entity = httpClient.build().execute(request).getEntity();
            String entityString = EntityUtils.toString(entity);
            ObjectMapper mapper = new ObjectMapper().configure(JsonParser.Feature.IGNORE_UNDEFINED, true);
            try {
                GitInfoResponse result = mapper.readValue(entityString, GitInfoResponse.class);
                return Optional.of(result);
            } catch (IOException e) {
                try {
                    log.error("Can't parse response in executeGetAndParseGitInfoResponse. Url: {}, Entity: {}",
                            url, entityString, e);
                    String gitMsg = mapper.readValue(entityString, GitInfoError.class).getMessage();
                    if (GitInfoError.GIT_API_PROJECT_NOT_FOUND.equals(gitMsg)) {
                        throw new GitValidationProjectNotFoundException();
                    } else if (GitInfoError.GIT_API_USER_NOT_FOUND.equals(gitMsg)) {
                        throw new GitValidationUserNotFoundException();
                    }
                } catch (IOException ioe) {
                    log.error("Can't transform entity to GitInfoError");
                }
            }
        } catch (IOException e) {
            log.error("Can't execute http request in executeGetAndParseGitInfoResponse. Url: {}", url, e);
        }
        return Optional.empty();
    }

    public String getGitEmail() {
        return Optional.ofNullable(reposEmail)
                .orElseThrow(() -> new RepoMailNotSetException());
    }

    /**
     * Retrieves url from git path.
     *
     * @param pathToGit url to git
     * @return empty string if no success otherwise url.
     */
    public String getGitEncodedUrl(String pathToGit) {
        String url = "";
        String urlTemplate = gitUrlPrefix + gitUrlPath;
        if (repositoryNamePattern == null) {
            repositoryNamePattern = Pattern.compile(gitUrlPrefix + "(.+/.+)$");
        }
        Matcher m = repositoryNamePattern.matcher(pathToGit);
        if (m.matches()) {
            String repoName = m.group(1);
            try {
                String repository = URLEncoder.encode(repoName, StandardCharsets.UTF_8.toString());
                url = String.format(urlTemplate, repository, gitUserId);
            } catch (UnsupportedEncodingException e) {
                log.error(ErrorCodes.MIA_0108_REPO_ENCODE_FAIL.getMessage(repoName, pathToGit));
            }
        }
        return url;
    }

    public String getGitPass() {
        return Optional.ofNullable(reposPass)
                .orElseThrow(() -> new RepoPassNotSetException());
    }

    public String getGitUser() {
        return Optional.ofNullable(reposUser)
                .orElseThrow(() -> new RepoUserNotSetException());
    }

    /**
     * Git clone.
     *
     * @param repoUrl git URL
     * @param toPath  folder to clone
     */
    public void gitClone(String repoUrl, File toPath) {
        try {
            Git git = Git.cloneRepository().setURI(repoUrl).setDirectory(toPath).setCredentialsProvider(getGitCred())
                    .call();
            git.getRepository().close();
            git.close();
            log.info("Repository {} has been cloned into {}", repoUrl, toPath);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    /**
     * Git commit.
     *
     * @param pathToGit path to GIT
     */
    public void gitCommitAndPush(Path pathToGit, String commitMessagePrefix) {
        try {
            git = getGitRepo(pathToGit);
            try {
                Ref head = git.getRepository().getAllRefs().get("HEAD");
                log.info("HEAD after clone: {}: {} - {}", head, head.getName(), head.getObjectId().getName());
            } catch (Exception e) {
                //skip
            }
            git.add().addFilepattern(".").call(); // apply added and updated files
            git.add().setUpdate(true).addFilepattern(".").call(); // apply removed files
            CommitCommand commit = git.commit().setAuthor(getAuthor())
                    .setMessage(commitMessagePrefix + "(from MIA toolbox)");
            commit.setCredentialsProvider(getGitCred());
            commit.call();
            Iterable<PushResult> pushResults = git.push()
                    .setRemote("origin")
                    .setPushAll()
                    .setCredentialsProvider(getGitCred())
                    .call();
            pushResults.forEach(r -> {
                String message = "Repository " + pathToGit + " has been pushed [Message: " + r.getMessages() + "; "
                        + "URI: " + r.getURI() + "; Updates: " + r.getRemoteUpdates() + "]";
                if (!r.getRemoteUpdates().stream().allMatch(rU -> rU.getStatus() == RemoteRefUpdate.Status.OK)) {
                    throw new GitException(message);
                }
                log.info(message);
            });
        } catch (GitAPIException e) {
            throw new GitException(e);
        } finally {
            git.close();
            git.getRepository().close();
            git = null;
        }
    }

    /**
     * Git pull.
     *
     * @param pathToGit path to GIT
     */
    public void gitPull(Path pathToGit) {
        try {
            final Git git = getGitRepo(pathToGit);
            git.reset().setMode(ResetType.HARD).call();
            git.pull().setCredentialsProvider(getGitCred()).call();
            git.getRepository().close();
            git.close();
            log.info("Repository {} has been pulled", pathToGit);
        } catch (CheckoutConflictException e) {
            throw new GitResetException(e);
        } catch (GitAPIException e) {
            throw new GitException(e);
        }
    }

    @PostConstruct
    public void init() {
        //git = Git.init();
    }

    private PersonIdent getAuthor() {
        String user = getGitUser();
        String mail = getGitEmail();
        return new PersonIdent(user, mail);
    }

    /**
     * Get GIT credentials.
     *
     * @return UsernamePasswordCredentialsProvider instance
     */
    private UsernamePasswordCredentialsProvider getGitCred() {
        String user = getGitUser();
        String pass = getGitPass();
        return new UsernamePasswordCredentialsProvider(user, pass);
    }

    /**
     * Get GIT repository.
     *
     * @param pathToGit path to GIT
     */
    private Git getGitRepo(Path pathToGit) {
        try {
            return Git.wrap(new FileRepositoryBuilder().setGitDir(pathToGit.resolve(GITFILE).toFile()).build());
        } catch (IOException e) {
            throw new GitRepoBuildException(e);
        }
    }
}
