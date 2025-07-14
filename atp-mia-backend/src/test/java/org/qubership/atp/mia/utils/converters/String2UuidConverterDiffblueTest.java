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

package org.qubership.atp.mia.utils.converters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.modelmapper.internal.InheritingConfiguration;
import org.modelmapper.internal.MappingContextImpl;
import org.modelmapper.internal.MappingEngineImpl;
import org.modelmapper.spi.ConditionalConverter;
import org.modelmapper.spi.MappingContext;

import com.fasterxml.classmate.types.TypePlaceHolder;

public class String2UuidConverterDiffblueTest {

    /**
     * Method under test: {@link String2UuidConverter#match(Class, Class)}
     */
    @Test
    public void testMatch() {
        String2UuidConverter string2UuidConverter = new String2UuidConverter();
        Class<Object> sourceType = Object.class;
        Class<Object> destinationType = Object.class;
        assertEquals(ConditionalConverter.MatchResult.NONE, string2UuidConverter.match(sourceType, destinationType));
    }

    /**
     * Method under test: {@link String2UuidConverter#match(Class, Class)}
     */
    @Test
    public void testMatch2() {
        String2UuidConverter string2UuidConverter = new String2UuidConverter();
        Class<String> sourceType = String.class;
        Class<Object> destinationType = Object.class;
        assertEquals(ConditionalConverter.MatchResult.NONE, string2UuidConverter.match(sourceType, destinationType));
    }

    /**
     * Method under test: {@link String2UuidConverter#convert(MappingContext)}
     */
    @Test
    public void testConvert() {
        String2UuidConverter string2UuidConverter = new String2UuidConverter();
        Class<String> sourceType = String.class;
        UUID randomUUIDResult = UUID.randomUUID();
        Class<UUID> destinationType = UUID.class;
        TypePlaceHolder genericDestinationType = new TypePlaceHolder(1);
        assertNull(
                string2UuidConverter.convert(new MappingContextImpl<>(null, sourceType, randomUUIDResult, destinationType,
                        genericDestinationType, "Type Map Name", new MappingEngineImpl(new InheritingConfiguration()))));
    }
}
