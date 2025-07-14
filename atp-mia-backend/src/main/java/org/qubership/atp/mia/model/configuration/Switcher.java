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

package org.qubership.atp.mia.model.configuration;

import java.io.Serializable;

import org.javers.core.metamodel.annotation.Value;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Value
public class Switcher implements Serializable, Cloneable {

    private static final long serialVersionUID = -233711472714816457L;
    private boolean value;
    private String actionType;
    private String name;
    private String display;
    private String actionTrue;
    private String actionFalse;

    /**
     * Clone the Switcher. Its Required after DB is introduced. Its purpose is, not to modify the original switcher.
     *
     * @return Switcher a clone copy to modify temporarily based on UI Selection of System switchers
     */
    public Switcher clone() {
        try {
            return (Switcher) super.clone();
        } catch (CloneNotSupportedException e) {
            return this;
        }
    }
}
