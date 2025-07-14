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

package org.qubership.atp.mia.model.sse;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class SsePingRunnable implements Runnable {

    private final SseEmitter emitter;
    private final UUID sseId;
    private final long timeout;
    private volatile boolean shutdown = false;
    private final AtomicLong timeoutSpend = new AtomicLong(0);

    @Override
    public void run() {
        Thread.currentThread().setName("PING_FOR_SSE_" + sseId);
        sleep();
        while (!shutdown) {
            try {
                emitter.send(SseEmitter.event()
                        .name(SseEventType.PING.name())
                        .data("Spend " + timeoutSpend.get() + " seconds", MediaType.APPLICATION_JSON));
                sleep();
            } catch (Exception e) {
                log.error("Exception during send ping into emitter with ID [{}]: {}", sseId, e.getMessage());
                shutdown = true;
            }
        }
        log.debug("Thread '{}' is completed", Thread.currentThread().getName());
    }

    public void shutdown() {
        this.shutdown = true;
    }

    private void sleep() {
        try {
            TimeUnit.MILLISECONDS.sleep(timeout);
            timeoutSpend.getAndAdd(timeout / 1000);
        } catch (InterruptedException ignore) {
            //ignore
        }
    }
}
