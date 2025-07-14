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

package org.qubership.atp.mia.model.impl.executable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpTrace;
import org.qubership.atp.mia.exceptions.rest.RestIncorrectEndpointException;
import org.qubership.atp.mia.exceptions.rest.UnsupportedRestMethodException;
import org.qubership.atp.mia.model.rest.methods.HttpConnect;
import org.qubership.atp.mia.model.rest.methods.HttpMkcol;
import org.qubership.atp.mia.model.rest.methods.HttpPropfind;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Slf4j
public class Rest implements Serializable {

    private static final long serialVersionUID = -3948743951276707091L;
    private String restFile;
    private String endpoint;
    private String method;
    private String headers;
    private String body;
    private String bodyFile;
    private boolean parseResponseAsTable;
    private boolean hasFile;
    private boolean disableRedirect;
    private boolean saveCookie;
    private RestLoopParameters restLoopParameters;
    private boolean checkStatusCodeFlag;
    @Nullable
    private List<String> checkStatusCode;
    private String script;

    /**
     * Cloning.
     *
     * @return clone.
     */
    public Rest clone() {
        RestBuilder builder = this.toBuilder();
        if (checkStatusCode != null) {
            builder.checkStatusCode(new ArrayList<>(checkStatusCode));
        }
        return builder.build();
    }

    public enum RestMethod {
        GET(HttpGet.class),
        POST(HttpPost.class),
        PUT(HttpPut.class),
        DELETE(HttpDelete.class),
        PATCH(HttpPatch.class),
        HEAD(HttpHead.class),
        CONNECT(HttpConnect.class),
        OPTION(HttpOptions.class),
        TRACE(HttpTrace.class),
        PROPFIND(HttpPropfind.class),
        MKCOL(HttpMkcol.class);

        private Class<HttpRequestBase> methodClass;

        RestMethod(Class httpMethodClass) {
            this.methodClass = httpMethodClass;
        }

        /**
         * return rest request by rest method.
         */
        public HttpRequestBase getHttpRequest(String url) {
            try {
                return this.methodClass.getConstructor(String.class).newInstance(url);
            } catch (InvocationTargetException ite) {
                if (ite.getTargetException() != null
                        && ite.getTargetException().getCause() != null
                        && (ite.getTargetException().getCause().toString()
                                .contains("java.net.URISyntaxException: Illegal character in path at index")
                        || ite.getTargetException().getCause().toString()
                                .contains("Illegal character in query at index"))) {
                    throw new RestIncorrectEndpointException(ite.getTargetException().getMessage());
                }
                throw new UnsupportedRestMethodException((ite.getCause() != null) ? ite.getCause() : ite);
            } catch (Exception e) {
                throw new UnsupportedRestMethodException(e);
            }
        }
    }
}
