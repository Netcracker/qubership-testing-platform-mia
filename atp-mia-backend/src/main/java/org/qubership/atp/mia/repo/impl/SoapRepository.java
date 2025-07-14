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

package org.qubership.atp.mia.repo.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.xml.soap.MessageFactory;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPConnectionFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

import org.qubership.atp.mia.exceptions.soap.SoapCreateConnectionFailException;
import org.qubership.atp.mia.exceptions.soap.SoapCreateRequestFailException;
import org.qubership.atp.mia.exceptions.soap.SoapExecutionFailException;
import org.qubership.atp.mia.exceptions.soap.SoapGetInstanceFailException;
import org.qubership.atp.mia.exceptions.soap.SoapWriteIoException;
import org.qubership.atp.mia.exceptions.soap.SoapWriteOutputFailException;
import org.qubership.atp.mia.model.environment.Connection;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.environment.System;
import org.qubership.atp.mia.model.impl.CommandResponse;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Soap;
import org.qubership.atp.mia.model.impl.output.CommandOutput;
import org.qubership.atp.mia.service.MiaContext;
import org.qubership.atp.mia.service.monitoring.MetricsAggregateService;
import org.qubership.atp.mia.utils.CryptoUtils;
import org.qubership.atp.mia.utils.FileUtils;
import org.qubership.atp.mia.utils.Utils;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@Slf4j
@RequiredArgsConstructor
public class SoapRepository {

    private final MiaContext miaContext;
    private final MetricsAggregateService metricsService;

    private static SOAPMessage createSoapRequest(Soap soap) {
        try {
            InputStream is = new ByteArrayInputStream(soap.getRequest().getBytes());
            SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(null, is);
            log.info("Request SOAP Message was created");
            log.debug(soap.getRequest());
            return soapMessage;
        } catch (SOAPException e) {
            throw new SoapGetInstanceFailException(e);
        } catch (IOException e) {
            throw new SoapCreateRequestFailException(e);
        }
    }

    /**
     * Sends soap request.
     */
    public CommandResponse sendSoapRequest(Command command) {
        log.info("Sending SOAP request");
        try {
            final System system = miaContext.getFlowData().getSystem(command.getSystem());
            Server server;
            try {
                server = system.getServer(Server.ConnectionType.HTTP);
            } catch (IllegalArgumentException e1) {
                try {
                    server = system.getServer(Connection.SourceTemplateId.HTTP);
                } catch (IllegalArgumentException e2) {
                    throw new SoapCreateConnectionFailException(e2);
                }
            }
            Map<String, String> serverParams = server.getConnection().getParameters();
            final HashMap<String, String> connectionInfo = new HashMap<>();
            String url = serverParams.get("url") + miaContext.evaluate(command.getSoap().getEndpoint());
            String login = serverParams.get("login");
            connectionInfo.put("user", login);
            connectionInfo.put("endpoint", url);
            connectionInfo.put("bodyRequest", command.getSoap().getRequest());
            SOAPMessage soapRequest = createSoapRequest(command.getSoap());
            MimeHeaders hd = soapRequest.getMimeHeaders();
            String authorization = Base64.getEncoder()
                    .encodeToString((login + ":" + CryptoUtils.decryptValue(serverParams.get("password"))).getBytes());
            hd.addHeader("Authorization", "Basic " + authorization);
            hd.addHeader("SOAPAction", "");
            connectionInfo.put("timestampRequest", Utils.getTimestamp());
            SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
            SOAPConnection soapConnection = soapConnectionFactory.createConnection();
            SOAPMessage soapResponse = soapConnection.call(soapRequest, url);
            log.info("Response SOAP Message was got");
            soapConnection.close();
            final File logFile = miaContext.getLogPath().resolve(miaContext.createLogFileName(command)).toFile();
            final CommandResponse commandResponse = new CommandResponse(
                    new CommandOutput(createFileWithResponse(soapResponse, logFile), null, true, miaContext));
            commandResponse.setConnectionInfo(connectionInfo);
            connectionInfo.put("timestampResponse", Utils.getTimestamp());
            try {
                connectionInfo.put("bodyResponse", soapResponse.toString());
                File fileFullInfo = miaContext.getLogPath().resolve(
                        miaContext.createLogFileName("SOAP_FULL_INFO", "json")).toFile();
                FileUtils.logIntoFile(Utils.GSON.toJson(connectionInfo), fileFullInfo);
                final CommandOutput commandFullInfo = new CommandOutput(fileFullInfo.getPath(), null, false,
                        miaContext);
                commandResponse.addCommandOutput(commandFullInfo);
            } finally {
                connectionInfo.remove("bodyResponse");
            }
            return commandResponse;
        } catch (SOAPException e) {
            throw new SoapExecutionFailException(e);
        }
    }

    /**
     * Writes soap response to file.
     */
    private String createFileWithResponse(SOAPMessage response, File logFile) {
        log.info("Creating file with response: {}, logfile: {}", response, logFile);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.writeTo(out);
            int responseSize = out.size() / 1024;
            log.info("[SIZE] SOAP Response length: {} kb", responseSize);
            metricsService.soapResponseSize(responseSize);
            String responseString = out.toString();
            responseString = Utils.getPrettyStringFromXml(responseString).replaceFirst(">", ">\n");
            log.debug(responseString);
            Files.write(Paths.get(logFile.getPath()), responseString.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new SoapWriteIoException(e);
        } catch (SOAPException e) {
            throw new SoapWriteOutputFailException(e);
        }
        log.debug("File with response created, path: " + logFile.getPath());
        return logFile.getPath();
    }
}
