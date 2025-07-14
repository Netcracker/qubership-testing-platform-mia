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

package org.qubership.atp.mia.utils.gitutilstest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.ConfigTestBean;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.impl.GitInfoResponse;
import org.qubership.atp.mia.service.execution.SqlExecutionHelperService;
import org.qubership.atp.mia.service.git.GitService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ExtendWith(SkipTestInJenkins.class)
public class GitUtilsTestsCheckXKubeUser extends ConfigTestBean {

    private final String token = "sometoken";
    private final ThreadLocal<GitService> gitService = new ThreadLocal<>();

    @MockBean
    protected SqlExecutionHelperService sqlService;

    @BeforeEach
    public void initGitUtilsTestsCheckXKubeUser() {
        gitService.set(spy(new GitService()));
    }

    @Test
    public void getEncodedGitUrl_whenCorrectUrl_thenOk() {
        // mocks
        mockGetHttpResponse(40);
        // init
        String expected = "";
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, token);
        Assert.assertEquals("Validation result should be empty (without errors)", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenLongUrl_thenOk() {
        // mocks
        mockGetHttpResponse(40);
        // init
        String expected = "";
        String pathToGit = "https://git.somedomain.com/Personal/Custom_projects/PROJ/project_mia";
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, token);
        Assert.assertEquals("Validation result should be empty (without errors)", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenUrlNotGitButDirectory_thenSkip() {
        // mocks
        mockGetHttpResponse(40);
        // init
        String expected = "";
        String pathToGit = "atp-mia-backend/src/main/config\\project\\322eda51-47b5-414a-951a-27221fa374aa/";
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, "");
        Assert.assertEquals("Validation result should be empty (without errors)", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenTokenEmpty_thenSkip() {
        // mocks
        mockGetHttpResponse(40);
        // init
        String expected = "";
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, "");
        Assert.assertEquals("Validation result should be empty (without errors)", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenxKubeHaveNoRights_thenError() {
        // mocks
        mockGetHttpResponse(20);
        // init
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        String expected = ErrorCodes.MIA_0115_GIT_VALIDATION_USER_NO_RIGHTS.getMessage(pathToGit);
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, token);
        Assert.assertEquals("Validation result should have errors", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenUserNotFound_thenError() {
        // mocks
        mockGetHttpResponse(null);
        // init
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        String expected = ErrorCodes.MIA_0116_GIT_VALIDATION_UNEXPECTED_ERROR.getMessage(pathToGit);
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, token);
        Assert.assertEquals("Validation result should have errors", expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenHttpError_thenError() {
        // mocks
        mockGetHttpResponse(new GitInfoResponse());
        // init
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        String expected = ErrorCodes.MIA_0115_GIT_VALIDATION_USER_NO_RIGHTS.getMessage(pathToGit);
        // do
        String actual = gitServiceCheckXKubeUserRights(pathToGit, token);
        Assert.assertEquals("Validation result should have errors", expected, actual);
    }

    private String gitServiceCheckXKubeUserRights(String pathToGit, String token) {
        ReflectionTestUtils.setField(gitService.get(), "gitToken", token);
        return gitService.get().checkXKubeUserRights(pathToGit);
    }

    private void mockGetHttpResponse(int accessLevel) {
        GitInfoResponse response = new GitInfoResponse();
        response.setAccessLevel(accessLevel);
        mockGetHttpResponse(response);
    }

    private void mockGetHttpResponse(GitInfoResponse response) {
        when(gitService.get().getGitEncodedUrl(anyString())).thenCallRealMethod();
        when(gitService.get().executeGetAndParseGitInfoResponse(anyString(), eq(token)))
                .thenReturn(Optional.ofNullable(response));
    }
}
