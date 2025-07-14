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

package org.qubership.atp.mia.config;

import java.util.UUID;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.qubership.atp.mia.controllers.MiaProofOfTestingController;
import org.qubership.atp.mia.model.impl.request.ExecutionRequest;
import org.qubership.atp.mia.repo.ContextRepository;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ContextInterceptor {

    private final MiaContext miaContext;
    private final ContextRepository contextRepository;
    private final MetricsAggregateService metricsService;

    /**
     * Remove MIA context for any Controller.
     *
     * @param joinPoint joinPoint
     */
    @After("execution(* org.qubership.atp.mia.controllers..*Controller.*(..))")
    public void afterController(JoinPoint joinPoint) {
        log.debug("Remove ThreadLocal in afterCompletion");
        contextRepository.removeContext();
    }

    /**
     * Remove MIA context for any Controller in case Exception.
     *
     * @param joinPoint joinPoint
     * @param exception Exception
     */
    @AfterThrowing(
            value = "execution(* org.qubership.atp.mia.controllers..*Controller.*(..))",
            throwing = "exception")
    public void afterExceptionInController(JoinPoint joinPoint, Exception exception) {
        log.debug("Remove ThreadLocal in afterException");
        contextRepository.removeContext();
    }

    /**
     * Set MIA context for MiaConfigurationController.
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.qubership.atp.mia.controllers.MiaConfigurationController.*(..))"
            + "|| execution(* org.qubership.atp.mia.controllers.MiaCacheController.*(..))"
            + "|| execution(* org.qubership.atp.mia.controllers.MiaServiceController.*(..))")
    public void beforeController(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length > 0) {
            String projectId = joinPoint.getArgs()[0].toString();
            log.debug("Set mia context with project ID '{}'", projectId);
            miaContext.setContext(UUID.fromString(projectId), null);
        } else {
            throw new IllegalArgumentException("No projectId found as parameter");
        }
    }

    /**
     * Set MIA context for MiaFileController.
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.qubership.atp.mia.controllers.MiaFileController.*(..))")
    public void beforeFileController(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length > 1) {
            String projectId = joinPoint.getArgs()[0].toString();
            UUID sessionId;
            try {
                sessionId = UUID.fromString(joinPoint.getArgs()[1].toString());
            } catch (Exception e) {
                sessionId = UUID.randomUUID();
            }
            log.debug("Set mia context with project ID '{}' and sessionID '{}'", projectId, sessionId);
            miaContext.setContext(UUID.fromString(projectId), sessionId);
        } else {
            throw new IllegalArgumentException("No projectId or sessionId found as parameter");
        }
    }

    /**
     * Set MIA context for MiaExecutionController.
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.qubership.atp.mia.controllers.MiaExecutionController.*(..))")
    public void beforeMiaExecutionController(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length > 2) {
            UUID projectId = (UUID) joinPoint.getArgs()[0];
            String env = joinPoint.getArgs()[1].toString();
            ExecutionRequest executionRequest = (ExecutionRequest) joinPoint.getArgs()[2];
            log.debug("Set mia context with project ID '{}'", projectId);
            miaContext.setContext(executionRequest, projectId, env);
            if (executionRequest.getFlowData() != null && executionRequest.getFlowData().getParameters() != null) {
                metricsService.requestContextSize(executionRequest.getFlowData().getParameters().size());
            }
        } else {
            throw new IllegalArgumentException("No projectId found as parameter");
        }
    }

    /**
     * Set MIA context for {@link MiaProofOfTestingController}.
     *
     * @param joinPoint joinPoint
     */
    @Before("execution(* org.qubership.atp.mia.controllers.MiaProofOfTestingController.*(..))")
    public void beforeMiaProofOfTestingController(JoinPoint joinPoint) {
        if (joinPoint.getArgs().length >= 2) {
            UUID projectId = (UUID) joinPoint.getArgs()[0];
            UUID sessionId = (UUID) joinPoint.getArgs()[1];
            log.debug("Set mia context with project ID #{} and session ID {}", projectId, sessionId);
            miaContext.setContext(projectId, sessionId);
            if (joinPoint.getArgs().length > 2) {
                miaContext.setFlowDataFromRequest((ExecutionRequest) joinPoint.getArgs()[2]);
            }
        } else {
            throw new IllegalArgumentException("No projectId or sessionId found as parameter");
        }
    }
}
