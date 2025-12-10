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

package org.qubership.atp.mia.repo;

import org.qubership.atp.mia.model.impl.FlowData;
import org.springframework.stereotype.Repository;

import com.google.common.base.Preconditions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Repository
@RequiredArgsConstructor
public class ContextRepository {

    private final ThreadLocal<FlowData> currentContext = new ThreadLocal<>();

    /**
     * Get context.
     *
     * @return {@link FlowData}
     */
    public FlowData getContext() {
        return Preconditions.checkNotNull(currentContext.get(), "No FlowData context");
    }

    /**
     * Set context to ThreadLocal.
     *
     * @param flowData {@link FlowData}
     */
    public void setContext(FlowData flowData) {
        this.currentContext.set(flowData);
    }

    /**
     * Remove context after execution.
     */
    public void removeContext() {
        this.currentContext.remove();
    }
}
