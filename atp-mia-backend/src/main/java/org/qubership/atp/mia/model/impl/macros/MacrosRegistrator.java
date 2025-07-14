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

package org.qubership.atp.mia.model.impl.macros;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MacrosRegistrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(MacrosRegistrator.class);

    public MacrosRegistrator() {
    }

    /**
     * register found macroses in parser.
     *
     * @return map of registered macroses (class to concrete registered instance)
     */
    public Map<Class<Macros>, Macros> register() {
        List<? extends Macros> macroses = new Reflections("org.qubership.atp.mia")
                .getSubTypesOf(Macros.class)
                .stream()
                .map(this::instantiate)
                .filter(Optional::isPresent) //ignore not registered macroses
                .map(Optional::get)
                .collect(Collectors.toList());
        macroses.forEach(MacroRegistryImpl::registerMacros);
        return macroses.stream().collect(Collectors.toMap(m -> (Class<Macros>) m.getClass(), m -> m));
    }

    /**
     * Returns macros instance.
     *
     * @param macrosClass to be instantiated
     * @param <T>         macros type
     * @return registered instance if created, or empty optional otherwise
     */
    private <T extends Macros> Optional<T> instantiate(Class<T> macrosClass) {
        try {
            T macros = macrosClass.newInstance();
            return Optional.of(macros);
        } catch (Exception e) {
            LOGGER.error("Failed to create macros with abstract module: "
                    + macrosClass, e);
            return Optional.empty();
        }
    }
}
