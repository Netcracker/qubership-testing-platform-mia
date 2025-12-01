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

import org.qubership.atp.mia.model.configuration.ProjectConfiguration;
import org.qubership.atp.mia.service.configuration.LazyConfigurationLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Post-processor для внедрения LazyConfigurationLoader в ProjectConfiguration 
 * после создания или десериализации из кэша.
 * 
 * Этот компонент автоматически инжектирует LazyConfigurationLoader во все экземпляры
 * ProjectConfiguration, чтобы они могли выполнять ленивую загрузку процессов и компаундов.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectConfigurationBeanPostProcessor implements BeanPostProcessor {
    
    private final LazyConfigurationLoader lazyConfigurationLoader;
    
    /**
     * Инжектирует LazyConfigurationLoader в ProjectConfiguration после его создания.
     * 
     * @param bean создаваемый bean
     * @param beanName имя bean
     * @return bean с внедренным LazyConfigurationLoader (если это ProjectConfiguration)
     * @throws BeansException в случае ошибки
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof ProjectConfiguration) {
            ProjectConfiguration config = (ProjectConfiguration) bean;
            config.setLazyLoader(lazyConfigurationLoader);
            log.trace("LazyConfigurationLoader injected into ProjectConfiguration for project: {}", 
                     config.getProjectId());
        }
        return bean;
    }
}


