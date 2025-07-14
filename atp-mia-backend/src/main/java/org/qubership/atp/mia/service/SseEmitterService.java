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

package org.qubership.atp.mia.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.qubership.atp.integration.configuration.model.notification.Notification;
import org.qubership.atp.integration.configuration.service.NotificationService;
import org.qubership.atp.mia.config.SseProperties;
import org.qubership.atp.mia.exceptions.MiaException;
import org.qubership.atp.mia.kafka.producers.MiaExecutionFinishProducer;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.qubership.atp.mia.model.sse.SseEventType;
import org.qubership.atp.mia.model.sse.SsePingRunnable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SseEmitterService {

    private final AtpUserService atpUserService;
    private final MiaExecutionFinishProducer miaExecutionFinishProducer;
    private final NotificationService notificationService;
    private final Map<UUID, SseEmitter> sseEmitters = new ConcurrentHashMap<>();
    private final Map<UUID, SsePingRunnable> ssePings = new ConcurrentHashMap<>();
    private final ExecutorService ssePingsExecutorService = Executors.newCachedThreadPool();
    private final SseProperties sseProperties;

    /**
     * Generates and configures emitter for sseId.
     *
     * @param sseId sse id
     * @param token token
     * @return configured sse emitter with connection event
     */
    public SseEmitter generateAndConfigureEmitter(UUID sseId, String token) {
        SseEmitter emitter = new SseEmitter(sseProperties.getSseEmitterTimeout());
        sseEmitters.put(sseId, emitter);
        emitter.onError((throwable) -> {
            log.error("Error while executing emitter", throwable);
            complete(sseId);
        });
        emitter.onCompletion(() -> complete(sseId));
        emitter.onTimeout(() -> {
            complete(sseId);
            prepareAndSendSseEmitterExpiredNotification(token);
        });
        SsePingRunnable ping = new SsePingRunnable(emitter, sseId, sseProperties.getSseEmitterPingTimeout());
        ssePings.put(sseId, ping);
        ssePingsExecutorService.execute(ping);
        return emitter;
    }

    /**
     * Checks that emitter exists and returns emitter.
     *
     * @param sseId sse id
     */
    public SseEmitter getEmitter(UUID sseId) {
        return sseEmitters.getOrDefault(sseId, null);
    }

    /**
     * Completes emitter with error.
     *
     * @param sseId sse ID
     * @param e     Exception
     */
    public void sendError(UUID sseId, MiaException e) {
        String message = e.getMessage();
        log.error("Exception occurred while sending a response throw emitter: {}", message, e);
        ExecutionResponse response = new ExecutionResponse();
        response.setSseId(sseId);
        response.setFinalMessage(true);
        response.setError(e);
        sendEventWithExecutionResult(response);
    }

    /**
     * Sends event about execution finish.
     *
     * @param executionResponse request execution response
     */
    public void sendEventWithExecutionResult(SseEmitter emitter, ExecutionResponse executionResponse) {
        log.debug("Sending sse event for sseId = {}", executionResponse.getSseId());
        SseEmitter.SseEventBuilder executionEvent = SseEmitter.event()
                .name(SseEventType.EXECUTION_FINISHED.name())
                .data(executionResponse, MediaType.APPLICATION_JSON);
        try {
            log.info("Send response:\n{}", executionResponse);
            emitter.send(executionEvent);
        } catch (Exception e) {
            log.error("ERROR during sending sse event for sseId {}", executionResponse.getSseId(), e);
        } finally {
            if (executionResponse.isFinalMessage()) {
                emitter.complete();
            }
        }
    }

    /**
     * Sends event about execution finish.
     *
     * @param response request execution response
     */
    public void sendEventWithExecutionResult(ExecutionResponse response) {
        int tryNumber = 1;
        do {
            SseEmitter sseEmitter = getEmitter(response.getSseId());
            if (sseEmitter != null) {
                sendEventWithExecutionResult(sseEmitter, response);
                break;
            } else {
                if (miaExecutionFinishProducer.isMock()
                        || !miaExecutionFinishProducer.executionFinishEventSend(response)) {
                    log.error("Not possible to sent response due to emitter absent and kafka is turned off"
                            + " (try number {}/3)", tryNumber);
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                        //ignored block
                    }
                } else {
                    break;
                }
            }
        } while (++tryNumber <= 3);
    }

    /**
     * Update response and send into emitter.
     *
     * @param response ExecutionResponse to be update
     * @param sseId    sse ID
     * @param isFinal  is final message into emitter
     * @param order    order message
     */
    public void updateResponseAndSendToEmitter(ExecutionResponse response, UUID sseId, boolean isFinal, int order) {
        if (response != null && sseId != null) {
            response.setSseId(sseId);
            response.setFinalMessage(isFinal);
            response.setOrder(order);
            sendEventWithExecutionResult(response);
        }
    }

    private void complete(UUID sseId) {
        SsePingRunnable ping = ssePings.get(sseId);
        if (ping != null) {
            ping.shutdown();
        }
        sseEmitters.remove(sseId);
        ssePings.remove(sseId);
    }

    /**
     * Prepares and sends notification message about sse emitter is expired.
     *
     * @param token token
     */
    private void prepareAndSendSseEmitterExpiredNotification(String token) {
        Notification notification = new Notification("SSE emitter is expired. Please establish new connection.",
                Notification.Type.INFO, atpUserService.getUserIdFromToken(token));
        notificationService.sendNotification(notification);
    }
}
