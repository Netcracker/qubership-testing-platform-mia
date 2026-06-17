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

package org.qubership.atp.mia.utils;

import java.util.UUID;

import javax.net.ssl.SSLContext;

import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;

public class HttpUtils {

    /**
     * Create and configure HttpClientBuilder with configuration:
     *  - Trust all certificates,
     *  - Don't check hostnames,
     *  - PoolingHttpClientConnectionManager with shared = true.
     *  In case errors - return HttpClientBuilder without any config settings.
     *
     * @return HttpClientBuilder object created and configured.
     */
    public static HttpClientBuilder createTrustAllHttpClientBuilder() {
        try {
            // 1. Create SSL context that trusts all certificates
            SSLContextBuilder builder = new SSLContextBuilder();
            builder.loadTrustMaterial(TrustAllStrategy.INSTANCE);
            SSLContext sslContext = builder.build();

            // 2. Create an SSLConnectionSocketFactory using the SSLContext
            SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                    .setSslContext(sslContext)
                    .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .build();

            // 3. Create a ConnectionManager and set the SSL socket factory on it
            PoolingHttpClientConnectionManager connectionManager =
                    PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .build();

            // 4. Configure the HttpClientBuilder and set the custom ConnectionManager
            return HttpClients.custom()
                    .setConnectionManager(connectionManager)
                    .setConnectionManagerShared(true); // Important for resource management
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
        return url.formatted(projectId, entityId.toString());
    }
}
