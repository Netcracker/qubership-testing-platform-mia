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

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.service.git.GitService;
import org.springframework.boot.test.mock.mockito.SpyBean;

@ExtendWith(SkipTestInJenkins.class)
public class GitUtilsTestGetEncodedUrl {

    private static final String urlTemplate = "https://git.somedomain.com/api/v4/projects/%s/members/%s";
    private static final String x_cube2vcsId = "2202";
    @SpyBean
    GitService gitService;

    /**
     * Check git encoding.
     */
    @Test
    public void getEncodedGitUrl_whenUrl_thenOk() {
        String expected = String.format(urlTemplate, "user_name%2Fatp-mia-project", x_cube2vcsId);
        String pathToGit = "https://git.somedomain.com/user_name/atp-mia-project";
        // do
        String actual = getGitEncodedUrl(pathToGit);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenLongUrl_thenOk() {
        String expected = String.format(urlTemplate, "Personal%2FCustom_projects%2FPROJ%2Fproject_mia", x_cube2vcsId);
        String pathToGit = "https://git.somedomain.com/Personal/Custom_projects/PROJ/project_mia";
        // do
        String actual = getGitEncodedUrl(pathToGit);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenConfig_thenEmptyResult() {
        String errMsg = "Result should be empty, when we use local directory instead git url";
        String expected = "";
        String pathToGit = "";
        // do
        String actual = getGitEncodedUrl(pathToGit);
        Assert.assertEquals(errMsg, expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenEmpty_thenEmptyResult() {
        String errMsg = "Result should be empty, when we use empty input instead git url";
        String expected = "";
        String pathToGit = "atp-mia-backend/src/main/config\\project\\323eda51-47b5-414a-951a-27221fa374a2/";
        // do
        String actual = getGitEncodedUrl(pathToGit);
        Assert.assertEquals(errMsg, expected, actual);
    }

    @Test
    public void getEncodedGitUrl_whenWord_thenEmptyResult() {
        String errMsg = "Result should be empty, when we use incorrect word instead git url";
        String expected = "";
        String pathToGit = "someWord";
        // do
        String actual = getGitEncodedUrl(pathToGit);
        Assert.assertEquals(errMsg, expected, actual);
    }
    private String getGitEncodedUrl(String pathToGit) {
        return gitService.getGitEncodedUrl(pathToGit);
    }
}
