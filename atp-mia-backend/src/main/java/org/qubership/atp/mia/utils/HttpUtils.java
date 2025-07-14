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

package org.qubership.atp.mia.utils;

import java.util.UUID;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;

public class HttpUtils {

    /**
     * TODO Make javadoc documentation for this method.
     */
    public static HttpClientBuilder createTrustAllHttpClientBuilder() {
        try {
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(null, (chain, authType) -> true);
            SSLConnectionSocketFactory sslsf = new
                    SSLConnectionSocketFactory(builder.build(), NoopHostnameVerifier.INSTANCE);
            return HttpClients.custom().setSSLSocketFactory(sslsf);
        } catch (Exception e) {
            return HttpClientBuilder.create();
        }
    }

    /**
     * GetMiaEntityUrl with projectId and EntityId.
     *
     * @param url       mia url
     * @param projectId project ID
     * @param entityId  entity ID
     * @return MiaEntityUrl.
     */
    public static String getMiaEntityUrl(String url, UUID projectId, UUID entityId) {
        return String.format(url, projectId, entityId.toString());
    }
}
