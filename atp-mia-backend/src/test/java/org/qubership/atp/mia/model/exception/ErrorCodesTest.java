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

package org.qubership.atp.mia.model.exception;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class ErrorCodesTest {

    @Test
    public void smoke_test() {
        String expected = "some random text";
        Exception e = new Exception(expected);
        Assert.assertTrue("Doesn't contain expected text",
                ErrorCodes.MIA_8000_UNEXPECTED_ERROR.getMessage(e).contains(expected));
    }
}
