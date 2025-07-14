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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Objects;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.repo.impl.RestRepositoryTestConfiguration;
import org.qubership.atp.mia.service.execution.RestClientService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class RestClientExecutorImplTest extends RestRepositoryTestConfiguration {

    protected static final ThreadLocal<RestClientService> restClientService = new ThreadLocal();

    @BeforeEach
    public void beforeRestClientExecutorImplTest() {
        restClientService.set(new RestClientService(miaContext.get()));
    }

    @Test
    public void prepareRestRequest() {
        miaContext.get().getFlowData().addParameter("postMethod", "POST");
        ListAppender<ILoggingEvent> listAppender = new ListAppender<>();
        listAppender.start();
        ((Logger) LoggerFactory.getLogger(RestClientService.class)).addAppender(listAppender);
        HttpRequestBase requestBase = restClientService.get().prepareRestRequest(rest.get(), server.get(), new HashMap<>());
        assertTrue("Appender.list is wrong: " + listAppender.list, listAppender.list.stream().anyMatch(m ->
                m.getFormattedMessage().equals("Charset \"UTF-8\" will use for parsing request body according to header")));
        assertEquals("http://localhost:8080/CUSTOMECA/services/CUSTOMECAAXPaymentsPort",
                requestBase.getURI().toString());
        assert requestBase instanceof HttpEntityEnclosingRequestBase;
        assert Objects.nonNull(((HttpEntityEnclosingRequestBase) requestBase).getEntity());
        assertEquals("", requestBase.getFirstHeader("soapaction").getValue());
        assertEquals("text/xml;charset=UTF-8", requestBase.getFirstHeader("content-type").getValue());
    }

    @Test
    public void whenHttpRestRequest_correctlyResult() {
        Rest restHttp = Rest.builder().build();
        restHttp.setEndpoint("https://atp-public-gateway:8080/");
        restHttp.setMethod("GET");
        restHttp.setParseResponseAsTable(true);
        restHttp.setBody("test");
        HttpRequestBase requestBase = restClientService.get().prepareRestRequest(restHttp, server.get(), new HashMap<>());
        assertEquals(restHttp.getEndpoint(), requestBase.getURI().toString());
    }


    @Test
    public void getPrettyStringFromXml() {
        System.setProperty("javax.xml.transform.TransformerFactory", "com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl");
        String test = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas."
                + "xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www."
                + "w3.org/2001/XMLSchema-instance\"><soapenv:Body><pagoRegistrarRequestOutput xmlns=\"urn:Convergys-"
                + "Interface-CUSTOMECA-AXPayments-WSDL\"><ns1:result "
                + "xmlns:ns1=\"urn:Convergys-Interface-CUSTOMECA-AXPaym"
                + "ents\"><ns1:codigoResultado>0</ns1:codigoResultado></ns1:result></pagoRegistrarRequestOutput"
                + "></soapenv:Body></soapenv:Envelope>";
        String prettyString = Utils.getPrettyStringFromXml(test);
        String expString = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><soapenv:Envelope "
                + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://www.w3"
                + ".org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <soapenv:Body>\r\n"
                + "    <pagoRegistrarRequestOutput xmlns=\"urn:Convergys-Interface-CUSTOMECA-AXPayments-WSDL\">\r\n"
                + "      <ns1:result xmlns:ns1=\"urn:Convergys-Interface-CUSTOMECA-AXPayments\">\r\n"
                + "        <ns1:codigoResultado>0</ns1:codigoResultado>\r\n"
                + "      </ns1:result>\r\n"
                + "    </pagoRegistrarRequestOutput>\r\n"
                + "  </soapenv:Body>\r\n"
                + "</soapenv:Envelope>\r\n";
        assertEquals(expString, prettyString);
    }
}
