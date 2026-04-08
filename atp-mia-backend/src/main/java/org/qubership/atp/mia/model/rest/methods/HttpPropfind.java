/*
 *  Copyright 2024-2026 NetCracker Technology Corporation
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

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class HttpPropfind extends HttpUriRequestBase {

    public static final String METHOD_NAME = "PROPFIND";

    // No no-arg constructor - it cannot exist in HttpClient 5

    public HttpPropfind(final URI uri) {
        super(METHOD_NAME, uri);
    }

    public HttpPropfind(final String uri) {
        super(METHOD_NAME, URI.create(uri));
    }

    // getMethod() method is no longer needed - the parent class handles it
}
