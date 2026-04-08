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

package org.qubership.atp.mia.service.execution;

import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.CredentialsStore;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.LaxRedirectStrategy;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
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

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class RestClientService {

    @Value("${rest.execution.timeout:5}")
    protected long executionTimeout;

    /*
     * Regexp to capture mime-type as group(1) and charset (if set) as group(2).
     * It contains robust expression which successfully parses such strings, for example:
     *  multipart/form-data; Charset=utf-8
     *  multipart/form-data; Charset="utf-8"
     *  multipart/form-data
     *  application/json
     *  text/html; charset=utf-8
     *  application/xml; charset=ISO-8859-1
     *  '  text/plain  ;  charset  =  utf-8  ' (extra spaces between words).
     */
    private static final Pattern CONTENT_TYPE_CHARSET_PATTERN =
            //Pattern.compile("^([^;]+)(?:;\\s*charset=\"?([^;\"]+)\"?)?", Pattern.CASE_INSENSITIVE);
            Pattern.compile("^\\s*([^;\\s]+)(?:\\s*;\\s*charset\\s*=\\s*\"?([^;\\s\"]+)\"?)?",
                    Pattern.CASE_INSENSITIVE);

    private final MiaContext miaContext;

    /**
     * Set SSL Context.
     */
    public static SSLContext getSslContext() {
        SSLContext sslContext;
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

        // Create an SSLConnectionSocketFactory using the SSLContext
        SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(getSslContext())
                .setHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();

        // Create a ConnectionManager and set the SSL socket factory on it
        PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setSSLSocketFactory(sslSocketFactory)
                        .build();

        HttpClientBuilder httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setConnectionManagerShared(true) // Important for resource management
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setResponseTimeout((int) TimeUnit.MINUTES.toMillis(executionTimeout), TimeUnit.MILLISECONDS)
                        .setConnectTimeout((int) TimeUnit.MINUTES.toMillis(executionTimeout), TimeUnit.MILLISECONDS)
                        .build());
        if (Strings.isNullOrEmpty(login) || Strings.isNullOrEmpty(password)) {
            log.info("These fields from REST connection are empty: login, password.");
        } else {
            CredentialsStore provider = new BasicCredentialsProvider();
            UsernamePasswordCredentials credentials =
                    new UsernamePasswordCredentials(login, CryptoUtils.decryptValue(password).toCharArray());
            provider.setCredentials(new AuthScope(null, -1), credentials);
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
    public HttpUriRequestBase prepareRestRequest(Rest rest, Server server, Map<String, String> connectionInfo) {
        log.info("Preparing REST request");
        Rest.RestMethod method;
        try {
            method = Rest.RestMethod.valueOf(miaContext.evaluate(rest.getMethod()));
        } catch (Exception e) {
            throw new IllegalArgumentException("Rest method should be one of "
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
        HttpUriRequestBase request = method.getHttpRequest(fullUrl);
        if (!Strings.isNullOrEmpty(rest.getHeaders())) {
            String headers = miaContext.evaluate(miaContext.evaluate(rest.getHeaders()));
            setHeaders(request, headers);
            connectionInfo.put("headersRequest", headers);
        }
        if (!Strings.isNullOrEmpty(rest.getBody()) && request instanceof HttpUriRequestBase base) {
            String body = miaContext.evaluate(miaContext.evaluate(rest.getBody()));
            connectionInfo.put("bodyRequest", body);

            /*
                After migration to httpclient5, there is no such constructor:
                    new ByteArrayEntity(byte[] buf)
                The closest variant is:
                    new ByteArrayEntity(byte[] buf, ContentType contentType)
                and, we already get 'Content-Type' header inside getCharsetFromRequest() method.
                So, the decision is:
                    1. Replace the method with a new one, returning 'Content-Type' (it includes Charset as well),
                    2. Use header value as the 2nd parameter in new ByteArrayEntity(byte[] buf, ContentType contentType)
             */
            ContentType contentType = getContentTypeFromRequest(request);
            HttpEntity bodyEntity = new ByteArrayEntity(body.getBytes(contentType.getCharset()), contentType);
            base.setEntity(bodyEntity);
            log.debug("REST body: {}", bodyEntity);
        }
        log.debug("REST request: {}", request);
        return request;
    }

    /**
     * Executes rest request.
     */
    public ClassicHttpResponse executeRestRequest(HttpClient httpClient, HttpUriRequestBase request) {
        ClassicHttpResponse httpResponse;
        try {
            log.info("Executing REST request: {}", request);
            httpResponse = (ClassicHttpResponse) httpClient.execute(request);
            log.debug("REST executed with response: {}", httpResponse);
        } catch (SocketTimeoutException ste) {
            try {
                throw new RestExecutionTimeOutException(executionTimeout, "minute(s)", request.getUri().toString());
            } catch (URISyntaxException e) {
                throw new RestIncorrectUrlException(); // It seems extra constructor(s) should be added.
            }
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
            Files.writeString(Path.of(logFile.getPath()), responseBody);
        } catch (IOException e) {
            throw new RestResultWriteToFileException(e);
        }
        return logFile.getPath();
    }

    /**
     * Sets http headers on rest request.
     */
    private void setHeaders(HttpUriRequestBase request, String headers) {
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

    private ContentType getContentTypeFromRequest(HttpUriRequestBase request) {
        ContentType contentType = ContentType.create("text/html", StandardCharsets.ISO_8859_1);

        Header[] headers = request.getHeaders("Content-Type");
        if (headers == null || headers.length == 0) {
            return contentType;
        } else {
            String lastMimeTypeString = null;
            for (Header h : headers) {
                Matcher matcher = CONTENT_TYPE_CHARSET_PATTERN.matcher(h.getValue());
                if (matcher.find()) {
                    String mimeTypeString = matcher.group(1).trim();
                    String charsetString = matcher.group(2);
                    if (StringUtils.isEmpty(charsetString)) {
                        // We need charset (if set).
                        // So, we check all content-type headers till it's set AND VALID!
                        // But what if there is no header with valid charset?
                        // In such case, we use the last mimeType with DEFAULT charset - StandardCharsets.ISO_8859_1
                        lastMimeTypeString = mimeTypeString;
                    } else {
                        try {
                            Charset charset = Charset.forName(charsetString);
                            contentType = ContentType.create(mimeTypeString, charset);
                            log.info("Charset \"{}\" will be used to parse request body according to header {}",
                                    charset, h.getValue());
                            return contentType;
                        } catch (Exception e) {
                            log.error("Incorrect charset \"{}\". Check other Content-Type headers (if any). "
                                    + "Finally, use default \"{}\"", charsetString, contentType.getCharset());
                        }
                    }
                }
            }

            return StringUtils.isEmpty(lastMimeTypeString)
                    ? contentType
                    : ContentType.create(lastMimeTypeString, StandardCharsets.ISO_8859_1);
        }
    }
}
