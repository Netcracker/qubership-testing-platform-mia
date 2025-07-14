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

package org.qubership.atp.mia.service.execution;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.ws.Holder;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.qubership.atp.mia.exceptions.internal.ssl.SslAlgorithmNotPresentException;
import org.qubership.atp.mia.exceptions.internal.ssl.SslInitException;
import org.qubership.atp.mia.exceptions.rest.RestExceptionDuringExecution;
import org.qubership.atp.mia.exceptions.rest.RestExecutionTimeOutException;
import org.qubership.atp.mia.exceptions.rest.RestHeadersIncorrectFormatException;
import org.qubership.atp.mia.exceptions.rest.RestIncorrectEndpointException;
import org.qubership.atp.mia.exceptions.rest.RestIncorrectUrlException;
import org.qubership.atp.mia.exceptions.rest.RestResultWriteToFileException;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import clover.com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestClientService {

    @Value("${rest.execution.timeout:5}")
    protected long executionTimeout;

    private final MiaContext miaContext;

    /**
     * Set SSL Context.
     */
    public static SSLContext getSslContext() {
        SSLContext sslContext = null;
        TrustManager[] trustAllCerts = new X509TrustManager[]{new X509TrustManager() {
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            @Override
            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
        };
        try {
            sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, null);
        } catch (NoSuchAlgorithmException e) {
            throw new SslAlgorithmNotPresentException(e);
        } catch (KeyManagementException e) {
            throw new SslInitException(e);
        }
        return sslContext;
    }

    /**
     * Prepares rest client.
     */
    public HttpClient prepareRestClient(Server server, boolean disableRedirect,
                                        Map<String, String> connectionInfo) {
        final String login = server.getProperty("login");
        final String password = server.getProperty("password");
        connectionInfo.put("user", login);
        HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(org.apache.http.client.config.RequestConfig.custom()
                        .setSocketTimeout((int) TimeUnit.MINUTES.toMillis(executionTimeout))
                        .setConnectTimeout((int) TimeUnit.MINUTES.toMillis(executionTimeout))
                        .build())
                .setSSLContext(getSslContext())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
        if (Strings.isNullOrEmpty(login) || Strings.isNullOrEmpty(password)) {
            log.info("These fields from REST connection are empty: login, password.");
        } else {
            CredentialsProvider provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials =
                    new UsernamePasswordCredentials(login, CryptoUtils.decryptValue(password));
            provider.setCredentials(AuthScope.ANY, credentials);
            httpClient.setDefaultCredentialsProvider(provider);
        }
        if (disableRedirect) {
            httpClient.disableRedirectHandling();
        } else {
            httpClient.setRedirectStrategy(new LaxRedirectStrategy());
        }
        return httpClient.build();
    }

    /**
     * Prepares rest request by rest method (get, post, put, delete).
     */
    public HttpRequestBase prepareRestRequest(Rest rest, Server server, Map<String, String> connectionInfo) {
        log.info("Preparing REST request");
        Rest.RestMethod method;
        try {
            method = Rest.RestMethod.valueOf(miaContext.evaluate(rest.getMethod()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Rest method should one of "
                    + Arrays.toString(Rest.RestMethod.values()));
        }
        String endPoint = miaContext.evaluate(rest.getEndpoint());
        if (Strings.isNullOrEmpty(endPoint)) {
            throw new RestIncorrectEndpointException("Null or Empty End Point");
        }
        if (Strings.isNullOrEmpty(server.getProperty("url"))) {
            throw new RestIncorrectUrlException();
        }
        String fullUrl = miaContext.evaluate(endPoint);
        if (!fullUrl.startsWith("http")) {
            fullUrl = server.getProperty("url") + fullUrl;
        }
        connectionInfo.put("endpoint", fullUrl);
        connectionInfo.put("method", method.name());
        HttpRequestBase request = method.getHttpRequest(fullUrl);
        if (!Strings.isNullOrEmpty(rest.getHeaders())) {
            String headers = miaContext.evaluate(miaContext.evaluate(rest.getHeaders()));
            setHeaders(request, headers);
            connectionInfo.put("headersRequest", headers);
        }
        if (!Strings.isNullOrEmpty(rest.getBody()) && request instanceof HttpEntityEnclosingRequestBase) {
            String body = miaContext.evaluate(miaContext.evaluate(rest.getBody()));
            connectionInfo.put("bodyRequest", body);
            HttpEntity bodyEntity = new ByteArrayEntity(body.getBytes(getCharsetFromRequest(request)));
            ((HttpEntityEnclosingRequestBase) request).setEntity(bodyEntity);
            log.debug("REST body: {}", bodyEntity);
        }
        log.debug("REST request: {}", request);
        return request;
    }

    /**
     * Executes rest request.
     */
    public HttpResponse executeRestRequest(HttpClient httpClient, HttpRequestBase request) {
        HttpResponse httpResponse;
        try {
            log.info("Executing REST request: {}", request);
            httpResponse = httpClient.execute(request);
            log.debug("REST executed with response: " + httpResponse);
        } catch (SocketTimeoutException ste) {
            throw new RestExecutionTimeOutException(executionTimeout, "minute(s)", request.getURI().toString());
        } catch (IOException e) {
            throw new RestExceptionDuringExecution(e);
        }
        return httpResponse;
    }

    /**
     * Writes rest response to file.
     */
    public String createFileWithResponse(String responseBody, File logFile) {
        if (responseBody.startsWith("<")) {
            responseBody = Utils.getPrettyStringFromXml(responseBody).replaceFirst(">", ">\n");
        }
        try {
            Files.write(Paths.get(logFile.getPath()), responseBody.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RestResultWriteToFileException(e);
        }
        return logFile.getPath();
    }

    /**
     * Sets http headers on rest request.
     */
    private void setHeaders(HttpRequestBase request, String headers) {
        log.info("Setting headers to request: {}, headers: {}", request, headers);
        for (String header : headers.split("\n")) {
            if (header.contains(":")) {
                String headerName = header.substring(0, header.indexOf(":")).trim();
                String headerValue = header.substring(header.indexOf(":") + 1).trim();
                if ("null".equalsIgnoreCase(headerValue)) {
                    headerValue = "";
                }
                log.debug("Header name: {}, Header value: {}", headerName, headerValue);
                request.setHeader(headerName, headerValue);
            } else {
                throw new RestHeadersIncorrectFormatException(header);
            }
        }
    }

    private Charset getCharsetFromRequest(HttpRequestBase request) {
        Holder<Charset> charset = new Holder<>(StandardCharsets.ISO_8859_1);
        Arrays.stream(request.getHeaders("Content-Type")).anyMatch(h -> {
            Matcher matcher = Pattern.compile(".*charset=([\\w\\-]+).*").matcher(h.getValue());
            if (matcher.matches()) {
                String charsetValue = matcher.group(1);
                try {
                    charset.value = Charset.forName(matcher.group(1));
                    log.info("Charset \"{}\" will use for parsing request body according to header", charset.value);
                    return true;
                } catch (Exception e) {
                    log.error("Incorrect charset \"{}\". Use default \"{}\"", charsetValue, charset.value);
                }
            }
            return false;
        });
        return charset.value;
    }
}
