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

package org.qubership.atp.mia.service.history.impl;

import javax.annotation.PostConstruct;

import org.modelmapper.ModelMapper;

abstract class AbstractRestoreMapper<S> extends AbstractMapper<S, S> {

    AbstractRestoreMapper(Class<S> sourceClass, Class<S> destinationClass, ModelMapper mapper) {
        super(sourceClass, destinationClass, mapper);
    }

    @PostConstruct
    public void setupMapper() {
        mapper.createTypeMap(sourceClass, destinationClass).setConverter(mapConverter());
    }
}
