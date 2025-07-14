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

package org.qubership.atp.mia.controllers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;
import org.qubership.atp.mia.SkipTestInJenkins;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.cache.MiaCacheService;
import org.qubership.atp.mia.service.execution.ProcessService;
import org.qubership.atp.mia.service.execution.SshExecutionHelperService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@ContextConfiguration(classes = {MiaCacheController.class})
@ExtendWith(SkipTestInJenkins.class)
@Isolated
public class MiaCacheControllerTests {

    @MockBean
    private CacheManager cacheManager;

    @Autowired
    private MiaCacheController miaCacheController;

    @MockBean
    private MiaCacheService miaCacheService;

    @MockBean
    private MiaContext miaContext;

    @MockBean
    private ProcessService processService;

    @MockBean
    private SshExecutionHelperService sshExecutionHelperService;

    /**
     * Method under test: {@link MiaCacheController#resetDbCache(UUID, String)}
     */
    @Test
    public void testResetDbCache() {
        when(processService.resetDbCache()).thenReturn(true);
        doNothing().when(miaContext)
                .setContext(Mockito.<ExecutionRequest>any(), Mockito.<UUID>any(), Mockito.<String>any());
        ResponseEntity<Boolean> actualResetDbCacheResult = miaCacheController.resetDbCache(UUID.randomUUID(), "Env");
        verify(miaContext).setContext(Mockito.<ExecutionRequest>any(), Mockito.<UUID>any(), Mockito.<String>any());
        //verify(processService).resetDbCache();
        assertEquals(HttpStatus.OK, actualResetDbCacheResult.getStatusCode());
        assertTrue(actualResetDbCacheResult.getBody());
        assertTrue(actualResetDbCacheResult.getHeaders().isEmpty());
    }

    /**
     * Method under test: {@link MiaCacheController#resetEnvironmentCaches(UUID)}
     */
    @Test
    public void testResetEnvironmentCaches() {
        doNothing().when(miaCacheService).clearEnvironmentsCache(Mockito.<CacheManager>any(), Mockito.<UUID>any());
        doNothing().when(sshExecutionHelperService).resetCache();
        ResponseEntity<Boolean> actualResetEnvironmentCachesResult = miaCacheController
                .resetEnvironmentCaches(UUID.randomUUID());
        verify(miaCacheService).clearEnvironmentsCache(Mockito.<CacheManager>any(), Mockito.<UUID>any());
        //verify(sshExecutionHelperService).resetCache();
        assertEquals(HttpStatus.OK, actualResetEnvironmentCachesResult.getStatusCode());
        assertTrue(actualResetEnvironmentCachesResult.getBody());
        assertTrue(actualResetEnvironmentCachesResult.getHeaders().isEmpty());
    }

    /**
     * Method under test: {@link MiaCacheController#resetPoolCache(UUID)}
     */
    @Test
    public void testResetPoolCache() {
        doNothing().when(sshExecutionHelperService).resetCache();
        ResponseEntity<Boolean> actualResetPoolCacheResult = miaCacheController.resetPoolCache(UUID.randomUUID());
        //verify(sshExecutionHelperService).resetCache();
        assertEquals(HttpStatus.OK, actualResetPoolCacheResult.getStatusCode());
        assertTrue(actualResetPoolCacheResult.getBody());
        assertTrue(actualResetPoolCacheResult.getHeaders().isEmpty());
    }

    /**
     * Method under test: {@link MiaCacheController#resetConfigurationCache(UUID)}
     */
    @Test
    public void testResetConfigurationCache() {
        doNothing().when(miaCacheService).clearConfigurationCache(Mockito.<UUID>any());
        ResponseEntity<Boolean> actualResetConfigurationCacheResult = miaCacheController
                .resetConfigurationCache(UUID.randomUUID());
        verify(miaCacheService).clearConfigurationCache(Mockito.<UUID>any());
        assertEquals(HttpStatus.OK, actualResetConfigurationCacheResult.getStatusCode());
        assertTrue(actualResetConfigurationCacheResult.getBody());
        assertTrue(actualResetConfigurationCacheResult.getHeaders().isEmpty());
    }
}
