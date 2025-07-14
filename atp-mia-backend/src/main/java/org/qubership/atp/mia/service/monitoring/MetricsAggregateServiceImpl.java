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

package org.qubership.atp.mia.service.monitoring;


import org.qubership.atp.mia.service.MiaContext;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MetricsAggregateServiceImpl implements MetricsAggregateService {

    private static final String PROCESSES_RUNNING_DIRECT = "atp.mia.processes.count";
    private static final String COMPOUNDS_RUNNING_DIRECT = "atp.mia.compounds.count";
    private static final String TEST_DATA_RUNNING_DIRECT = "atp.mia.test.data.count";
    private static final String EVENT_EXCEL_RUNNING_DIRECT = "atp.mia.event.excel.count";
    private static final String PROJECT = "project";
    private static final String REQUEST_CONTEXT_SIZE = "atp.mia.request.context.size";
    private static final String DOWNLOAD_FILE_SIZE = "atp.mia.download.file.size";
    private static final String SQL_QUERY_RECORDS_SIZE = "atp.mia.sql.query.records.size";
    private static final String REST_RESPONSE_SIZE = "atp.mia.rest.response.size";
    private static final String SOAP_RESPONSE_SIZE = "atp.mia.soap.response.size";

    private final Counter.Builder projectToProcessesRunningCounter = Counter.builder(PROCESSES_RUNNING_DIRECT)
            .description("total number of running processes");
    private final Counter.Builder projectToCompoundsRunningCounter = Counter.builder(COMPOUNDS_RUNNING_DIRECT)
            .description("total number of running compounds");
    private final Counter.Builder projectToTestDataRunningCounter = Counter.builder(TEST_DATA_RUNNING_DIRECT)
            .description("total number of running test data");
    private final Counter.Builder projectToEventExcelCallsRunningCounter = Counter.builder(EVENT_EXCEL_RUNNING_DIRECT)
            .description("total number of running excel calls");
    private final Counter.Builder projectToRequestContentSize = Counter.builder(REQUEST_CONTEXT_SIZE)
            .description("content size per request");
    private final Counter.Builder projectToDownloadFileSize = Counter.builder(DOWNLOAD_FILE_SIZE)
            .description("Downloaded file size from Ssh Server");
    private final Counter.Builder projectToSqlQueryRecordsSize = Counter.builder(SQL_QUERY_RECORDS_SIZE)
            .description("No of SQL Query records");
    private final Counter.Builder projectToRestResponseSize = Counter.builder(REST_RESPONSE_SIZE)
            .description("Rest response size");
    private final Counter.Builder projectToSoapResponseSize = Counter.builder(SOAP_RESPONSE_SIZE)
            .description("Soap response size");

    private final MeterRegistry meterRegistry;
    private final MiaContext miaContext;

    @Override
    public void processExecutionWasStarted() {
        projectToProcessesRunningCounter.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment();
    }

    @Override
    public void compoundExecutionWasStarted() {
        projectToCompoundsRunningCounter.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment();
    }

    @Override
    public void testDataExecutionWasStarted() {
        projectToTestDataRunningCounter.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment();
    }

    @Override
    public void eventFromExcelCallStarted() {
        projectToEventExcelCallsRunningCounter.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment();
    }

    @Override
    public void requestContextSize(int contextSize) {
        projectToRequestContentSize.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment(contextSize);
    }

    @Override
    public void requestSshDownloadFileSize(long fileSize) {
        projectToDownloadFileSize.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment(fileSize);
    }

    @Override
    public void sqlQueryRecordsSize(long numberOfRecords) {
        projectToSqlQueryRecordsSize.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment(numberOfRecords);
    }

    @Override
    public void restResponseSize(long responseSize) {
        projectToRestResponseSize.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment(responseSize);
    }

    @Override
    public void soapResponseSize(int responseSize) {
        projectToSoapResponseSize.tags(PROJECT, miaContext.getProjectId().toString())
                .register(meterRegistry).increment(responseSize);
    }
}
