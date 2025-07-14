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

package org.qubership.atp.mia.model.rest.methods;

import java.net.URI;

import org.apache.http.client.methods.HttpRequestBase;

public class HttpConnect extends HttpRequestBase {

    public static final String METHOD_NAME = "CONNECT";

    public HttpConnect() {
    }

    public HttpConnect(URI uri) {
        this.setURI(uri);
    }

    public HttpConnect(String uri) {
        this.setURI(URI.create(uri));
    }

    public String getMethod() {
        return METHOD_NAME;
    }
}
