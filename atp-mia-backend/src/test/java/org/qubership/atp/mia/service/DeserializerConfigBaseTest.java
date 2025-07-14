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

package org.qubership.atp.mia.service;

import static org.qubership.atp.mia.utils.Utils.listToSet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.lang.SerializationUtils;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.impl.executable.Command;
import org.qubership.atp.mia.model.impl.executable.Input;
import org.qubership.atp.mia.model.impl.executable.Prerequisite;
import org.qubership.atp.mia.model.impl.executable.ProcessSettings;
import org.qubership.atp.mia.model.impl.executable.Rest;
import org.qubership.atp.mia.model.impl.executable.Soap;
import org.qubership.atp.mia.model.impl.executable.TableMarker;
import org.qubership.atp.mia.model.impl.executable.TestDataParams;
import org.qubership.atp.mia.model.impl.executable.Validation;
import org.qubership.atp.mia.model.pot.Marker;

import com.google.common.collect.ImmutableList;

public abstract class DeserializerConfigBaseTest extends ProcessServiceBaseTest {

    private static String systemBillingSystem = "Billing System";

    /**
     * Get BG process.
     *
     * @return BG process
     */
    public static ProcessConfiguration getBg() {
            final Input inputBG =
                    new Input("bill_period", "list", listToSet("1", "2")).setLabel("Bill Period").setRequired(true);
        final Prerequisite prerequisitesBG = new Prerequisite("SSH", systemBillingSystem, "echo 1;");
        final List<Prerequisite> prerequisites = new ArrayList<>();
        prerequisites.add(prerequisitesBG);
        final Command commandBG = new Command("BG", "SSH", systemBillingSystem, listToSet("BG -a \"-a :accountNumber\""));
        commandBG.setLogFileNameFormat("Custom_log_name.log");
        TableMarker marker1 = new TableMarker();
        marker1.setTableRowCount(">0");
        final Validation validation1 = new Validation("SQL", systemBillingSystem, "select * from ACCOUNTDETAILS where "
                + "account_num = :accountNumber")
                .setTableName("AccountDetails")
                .setReferToCommandExecution(ImmutableList.of("BG -a \"-a :accountNumber\""))
                .setTableMarker(marker1);
        TableMarker marker2 = new TableMarker();
        HashMap<String, String> expectedResults = new HashMap<>();
        expectedResults.put("account_num", ":accountNumber");
        marker2.setExpectedResultForQuery(expectedResults);
        final Validation validation2 = new Validation("SQL", systemBillingSystem, "select bill_period, payment_method_id "
                + "from ACCOUNTDETAILS where account_num = :accountNumber")
                .setTableName("AccountDetails")
                .setReferToCommandExecution(ImmutableList.of("BG -a \"-a :accountNumber\""))
                .setTableMarker(marker2);
        return ProcessConfiguration.builder()
                .id(UUID.randomUUID())
                .name("SSH_BG")
                .processSettings(new ProcessSettings().toBuilder()
                        .name("SSH_BG")
                        .inputs(ImmutableList.of(inputBG))
                        .prerequisites(prerequisites)
                        .command(commandBG)
                        .validations(ImmutableList.of(validation1, validation2))
                        .build())
                .build();
    }

    public static ProcessConfiguration getBgWithMarker() {
        ProcessConfiguration processBgWithMarker = (ProcessConfiguration) SerializationUtils.clone(getBg());
        processBgWithMarker.setName("SSH_MARKER");
        processBgWithMarker.getProcessSettings().getCommand().setMarker(Marker.builder()
                            .failWhenNoPassedMarkersFound(true)
                            .passedMarkerForLog(Collections.singletonList(":passMarker"))
                            .build());
        return processBgWithMarker;
    }

    /**
     * Get Bgforswitcher process.
     *
     * @return Bgforswitcher process
     */
    public static ProcessConfiguration getBgforswitcher() {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        values.add("BG -a \"-a :accountNumber\"");
        final Command commandBG = new Command("BG", "SSH", systemBillingSystem, values);
        commandBG.setLogFileNameFormat("Custom_log_name.log");
        return ProcessConfiguration.builder()
                .id(UUID.randomUUID())
                .name("SSH_BG")
                .processSettings(new ProcessSettings().toBuilder().name("SSH_BG").command(commandBG).build())
                .build();
    }

    /**
     * Get Sqlforswitcher process.
     *
     * @return Sqlforswitcher process
     */
    public static ProcessConfiguration getSqlforswitcher() {
            LinkedHashMap<String, String> atpValues = new LinkedHashMap<>();
            String firstSelect = "select * from gparams where name like '%SYSdate%'";
            String firstUpdate = "update gparams set name = 'SYSdateOverride' where name = '#SYSdateOverride'";
            String firstInsert = "insert into gparams (name, \"TYPE\", start_dtm, string_value, integer_value) values"
                    + " ('SYSdateOverride',  'STRING',  date '1995-01-01',  'ANY',  null)";
            String firstDelete = "delete from gparams where name = 'SYSdateValue'";
            atpValues.put("SELECT", firstSelect);
            atpValues.put("UPDATE", firstUpdate);
            atpValues.put("INSERT", firstInsert);
            atpValues.put("DELETE", firstDelete);
            final Command commandSql = new Command("SQL", "SQL", systemBillingSystem, listToSet(
                    firstSelect,
                    firstUpdate,
                    firstInsert,
                    firstDelete,
                    "update gparams set name = '#SYSdateOverride' where name = 'SYSdateOverride'",
                    "update gparams set name = '#SYSdateValue' where name = 'SYSdateValue'",
                    "update gparams set name = 'SYSdateValue' where name = '#SYSdateValue'",
                    "update gparams set string_value = :SYSdateValue where name = 'SYSdateValue'",
                    "insert into gparams (name, \"TYPE\", start_dtm, string_value, integer_value) values "
                            + "('SYSdateValue', 'STRING', to_date('01-01-1970', 'dd-mm-yyyy'), :SYSdateValue, null)"))
                    .setAtpValues(atpValues);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_BG")
                    .processSettings(ProcessSettings.builder().name("SSH_BG").command(commandSql).build())
                    .build();
    }

    public static ProcessConfiguration getGenerationPp() {
            final Marker marker = Marker.builder().build();
            marker.setPassedMarkerForLog(ImmutableList.of("The process aborted with status 0"));
            marker.setFailedMarkersForLog(ImmutableList.of("The process aborted with status"));
            final Input inputPp1 = new Input("payment_amount", "text", "100.00").setLabel("Payment Amount");
            final Input inputPp2 = new Input("payment_transaction_id", "text", nullSet).setLabel("Payment "
                    + "Transaction ID");
            final Input inputPp3 = new Input("actual_payment_date", "text", nullSet).setLabel("Actual Payment Date");
            final Command commandPp = new Command("Postal Payment", "SSH_GenerationFile", systemBillingSystem,
                    listToSet("NCPP -a \"-mode POSTAL\""));
            commandPp.setNamesOfFilesForGeneration(ImmutableList.of("PP_${Random(10000)}"))
                    .setFileExtension(".dat")
                    .setPathForUpload("/u02/nifi/Payment/POSTAL/input/")
                    .setEthalonFilesForGeneration(ImmutableList.of("postal-payment"))
                    .setMarker(marker);
            final Validation validationPp = new Validation("SQL", systemBillingSystem, "select account_num,account_payment_seq,"
                    + "payment_channel_id,payment_confirmation_date,payment_extract_date,payment_file_name from "
                    + "ACCOUNTPAYATTRIBUTES where account_num = :accountNumber").setTableName("ACCOUNTPAYATTRIBUTES");
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_GenerationFile_Postal_Payment")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_GenerationFile_Postal_Payment")
                            .inputs(ImmutableList.of(inputPp1, inputPp2, inputPp3))
                            .command(commandPp)
                            .validations(ImmutableList.of(validationPp))
                            .build())
                    .build();
    }

    public static ProcessConfiguration getCheck() {
            final Input inputPr1 = new Input("extractDat", "text", nullSet).setLabel("Extract Dat");
            final Input inputPr2 = new Input("startDat", "text", nullSet).setLabel("Start Dat");
            final Input inputPr3 = new Input("endDat", "text", nullSet).setLabel("End Dat");
            final Command commandPr = new Command("Check file on server", "SSH_CheckFileOnServer", systemBillingSystem,
                    listToSet("PAYMENTREPORTGEN -a \"-mode DETAIL -extractDat :extractDat\"",
                            "PAYMENTREPORTGEN -a \"-mode DETAIL -startDat :startDat -endDat :endDat\" "))
                    .setRegexpForFileRetrieve("(?<=.csv to ).*(?<=.csv)");
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_CheckFileOnServer_PAYMENTREPORTGEN")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_CheckFileOnServer_PAYMENTREPORTGEN")
                            .inputs(ImmutableList.of(inputPr1, inputPr2, inputPr3))
                            .command(commandPr)
                            .build())
                    .build();
    }

    public static ProcessConfiguration getSql() {
            final Input inputSql = new Input("SYSdateValue", "date", nullSet).setLabel("SYSdateValue YYYYMMDD hhmmss")
                    .setMask("YYYYMMDD").setRequired(true);
            LinkedHashMap<String, String> atpValues = new LinkedHashMap<>();
            String firstSelect = "select * from gparams where name like '%SYSdate%'";
            String firstUpdate = "update gparams set name = 'SYSdateOverride' where name = '#SYSdateOverride'";
            String firstInsert = "insert into gparams (name, \"TYPE\", start_dtm, string_value, integer_value) values"
                    + " ('SYSdateOverride',  'STRING',  date '1995-01-01',  'ANY',  null)";
            String firstDelete = "delete from gparams where name = 'SYSdateValue'";
            atpValues.put("SELECT", firstSelect);
            atpValues.put("UPDATE", firstUpdate);
            atpValues.put("INSERT", firstInsert);
            atpValues.put("DELETE", firstDelete);
            final Command commandSql = new Command("SQL", "SQL", systemBillingSystem, listToSet(
                    firstSelect,
                    firstUpdate,
                    firstInsert,
                    firstDelete,
                    "update gparams set name = '#SYSdateOverride' where name = 'SYSdateOverride'",
                    "update gparams set name = '#SYSdateValue' where name = 'SYSdateValue'",
                    "update gparams set name = 'SYSdateValue' where name = '#SYSdateValue'",
                    "update gparams set string_value = :SYSdateValue where name = 'SYSdateValue'",
                    "insert into gparams (name, \"TYPE\", start_dtm, string_value, integer_value) values "
                            + "('SYSdateValue', 'STRING', to_date('01-01-1970', 'dd-mm-yyyy'), :SYSdateValue, null)"))
                    .setAtpValues(atpValues);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SQL_GPARAMS")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SQL_GPARAMS")
                            .inputs(ImmutableList.of(inputSql))
                            .command(commandSql).build())
                    .build();
    }

    public static ProcessConfiguration getTransfer() {
            final Command commandTransfer = new Command("E1EXP", "SSH_TransferFile", systemBillingSystem, listToSet(
                    "E1EXP"))
                    .setPathForUpload("/u02/qubership/billing_system/dev/infinys_root/PROJECT_ENGAGEONE/input/periodicbill/")
                    .setRegexpForFileRetrieve("(?<=.E1EXP : Output file created with File Name: ').*?(?=')");
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_TransferFile_E1EXP")
                    .processSettings(new ProcessSettings().toBuilder().name("SSH_TransferFile_E1EXP").command(commandTransfer).build())
                    .build();
    }

    public static ProcessConfiguration getFillInput() {
            final Input inputFill = new Input("newAccountNumber", "text", nullSet)
                    .setLabel("New account number (filled by 0 in the start while length < 20)")
                    .setMaxLength(20);
            final Command commandFill = new Command("Fill Input by 0", "SSH", systemBillingSystem, listToSet("BG -a \"-a "
                    + ":accountNumber\""));
        ProcessConfiguration processFillInput = ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("Fill Input by 0")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Fill Input by 0")
                            .inputs(ImmutableList.of(inputFill))
                            .command(commandFill).build())
                    .build();
            List<Validation> statement = new ArrayList<>();
            statement.add(new Validation("SQL", systemBillingSystem, "select * from gparams where name like '%SYSdate%'"));
            processFillInput.getProcessSettings().setCurrentStatement(statement);
        return processFillInput;
    }

    public static ProcessConfiguration getDump() {
            final Command commandDump = new Command("Create Billing System dump", "SSH_UploadFileAndDownloadResult", systemBillingSystem,
                    listToSet("exp $DATABASE parfile="))
                    .setDelayForGeneration("5")
                    .setPathForUpload(":infinys_root/")
                    .setFileExtension(".dmp").setNamesOfFilesForGeneration(ImmutableList.of(":accountNumber_${Random"
                                    + "(10000)}",
                            ":customerRef_${Random(10000)}", "configDump_${Random(10000)}"))
                    .setEthalonFilesForGeneration(ImmutableList.of("parfile_RB80_acc_data.dat", "parfile_RB80_cust_data"
                            + ".dat", "parfile_RB804_exp_config.dat"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_UploadFileAndDownloadResult_DUMP")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_UploadFileAndDownloadResult_DUMP")
                            .command(commandDump).build())
                    .build();
    }

    public static ProcessConfiguration getMultiDownload() {
            final Command commandMD = new Command("Multiple download", "SSH_DownloadFiles", systemBillingSystem,
                    listToSet("ls -la"))
                    .setRegexpForFileRetrieve("-name \"*:accountNumber*.pdf\"")
                    .setPathsForDownload(ImmutableList.of(":infinys_root/PROJECT_ENGAGEONE/output/periodicbill/ebill",
                            ":infinys_root/PROJECT_ENGAGEONE/output/periodicbill/print"))
                    .setDisplayDownloadedFileContent(true);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_DownloadFiles_EngageOne")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_DownloadFiles_EngageOne")
                            .command(commandMD).build())
                    .build();
    }

    public static ProcessConfiguration getDefaultRest() {
            Rest rest = Rest.builder().build();
            rest.setEndpoint("/rest/config?projectId=46578a43-4cfb-46e4-80b8-95ce21151a9f&needReload=false");
            rest.setMethod("GET");
            rest.setHeaders("Content-Type: application/json;charset=UTF-8");
            rest.setBody("not used for GET method");
            final Command commandRest = new Command("DEFAULT_REST", "REST", "Billing System", nullSet);
            commandRest.setRest(rest);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("DEFAULT_REST")
                    .processSettings(new ProcessSettings().toBuilder().name("DEFAULT_REST").command(commandRest).build())
                    .build();
    }

    public static ProcessConfiguration getRestBodyFromFile() {
            final Input inpuBodyFile = new Input("bodyFile", "attachment", nullSet).setLabel("File for rest body");
            Rest rest = Rest.builder().build();
            rest.setEndpoint("/rest/config?projectId=46578a43-4cfb-46e4-80b8-95ce21151a9f&needReload=false");
            rest.setMethod("GET");
            rest.setHeaders("Content-Type: application/json;charset=UTF-8");
            rest.setBodyFile(":bodyFile");
            final Command commandRest = new Command("REST_WITH_BODY_FROM_FILE", "REST", "Billing System", nullSet);
            commandRest.setRest(rest);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("REST_WITH_BODY_FROM_FILE")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("REST_WITH_BODY_FROM_FILE")
                            .inputs(ImmutableList.of(inpuBodyFile))
                            .command(commandRest).build())
                    .build();
    }

    public static ProcessConfiguration getRestFromFile() {
            final Input inputRestFile =
                    new Input("restFile", "attachment", nullSet).setLabel("File for whole rest").setRequired(true);
            Rest rest = Rest.builder().build();
            rest.setRestFile(":restFile");
            final Command commandRest = new Command("REST_FROM_FILE", "REST", "Billing System", nullSet);
            commandRest.setRest(rest);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("REST_FROM_FILE")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("REST_FROM_FILE")
                            .inputs(ImmutableList.of(inputRestFile))
                            .command(commandRest).build())
                    .build();
    }

    public static ProcessConfiguration getSoap() {
            final Input inputRequestFile = new Input("soapRequestFile", "attachment", nullSet)
                    .setLabel("File for request");
            final Input inputEP = new Input("soapEndpoint", "text", nullSet).setLabel("Endpoint");
            final Input inputRequest = new Input("soapRequest", "ace", nullSet).setLabel("Request");
            Soap soap = new Soap();
            soap.setEndpoint(":soapEndpoint");
            soap.setRequest(":soapRequest");
            soap.setRequestFile(":soapRequestFile");
            final Command commandRest = new Command("Soap", "SOAP", "ECA", nullSet);
            commandRest.setSoap(soap);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SOAP")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SOAP")
                            .inputs(ImmutableList.of(inputEP, inputRequest, inputRequestFile))
                            .command(commandRest).build())
                    .build();
    }

    public static ProcessConfiguration getEventS() {
            final Input generateEventInput = new Input("template", "checkBoxGroup", nullSet).setLabel("Event types");
            generateEventInput.setValues(listToSet("SMS: :TemplateEvent1", "CALL: :TemplateEvent2"));
            final Input generateEventInput1 = new Input("Calling", "text", "32553697560").setLabel("Event Source");
            final Input generateEventInput2 = new Input("EVENT_DTM", "text", "2019/07/05-05-35-28.01").setLabel(
                    "Event Date");
            final Input generateEventInput3 = new Input("MO", "text", "MO_1").setLabel("MO");
            final Input generateEventInput4 = new Input("Destination", "text", "32184854784").setLabel("Destination");
            final Command generateEventCommand = new Command("SSH_GenerateEvent", "SSH_GenerateEvent",
                    systemBillingSystem, listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
            generateEventCommand.setNamesOfFilesForGeneration(ImmutableList.of("event_file_sub_${Timestamp"
                                    + "(YYYYMMdd_hhmmss)}",
                            "control_file_${Timestamp(YYYYMMdd_hhmmss)}"))
                    .setSaveGeneratedFilesToParameter("generatedFiles").setFileExtension(".dat")
                    .setPathForUpload(":infinys_root/")
                    .setEthalonFilesForGeneration(ImmutableList.of("eventFromFile_templ_Project_One.dat", "control_templ"
                            + ".dat"))
                    .setRegexpForFileRetrieve("(?<=.to ).*(?<=.dat_ERR_.{12})")
                    .setValues(listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_GenerateEvent")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_GenerateEvent")
                            .inputs(ImmutableList.of(generateEventInput, generateEventInput1, generateEventInput2,
                                    generateEventInput3, generateEventInput4))
                            .command(generateEventCommand).build())
                    .build();
    }

    public static ProcessConfiguration getEventFileProjectThree() {
            final Input generateEventInput1 = new Input("EventGenerationFile", "attachment", nullSet)
                    .setLabel("File for event generation (MANDATORY)");
            final Input generateEventInput2 = new Input("EventGenerationSheet", "text", nullSet)
                    .setLabel("Sheet name from file for event generation (OPTIONAL, by default get first sheet)");
            final Input generateEventInput3 = new Input("EventGenerationTestCase", "text", nullSet)
                    .setLabel("Test case for event generation (OPTIONAL, by default get all TCs from sheet)");
            final Input generateEventInput4 = new Input("EventGenerationScenario", "text", nullSet)
                    .setLabel("Scenario for event generation (OPTIONAL, by default get all scenarios from test case)");
            final Command generateEventCommand = new Command("SSH_GenerateEventFromFile", "SSH_GenerateEventFromFile",
                    systemBillingSystem, listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
            generateEventCommand.setNamesOfFilesForGeneration(ImmutableList.of("event_file_sub_${Timestamp"
                                    + "(YYYYMMdd_hhmmss)}",
                            "control_file_${Timestamp(YYYYMMdd_hhmmss)}"))
                    .setSaveGeneratedFilesToParameter("generatedFiles").setFileExtension(".dat")
                    .setPathForUpload(":infinys_root/")
                    .setEthalonFilesForGeneration(ImmutableList.of("eventFromFile_templ_Project_Three.dat", "control_templ"
                            + ".dat"))
                    .setCharsetForGeneratedFile("ASCII")
                    .setRegexpForFileRetrieve("(?<=.to ).*(?<=.dat_ERR_.{12})")
                    .setValues(listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("EVENTS FROM FILE PROJECT THREE BASED")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("EVENTS FROM FILE PROJECT THREE BASED")
                            .inputs(ImmutableList.of(generateEventInput1, generateEventInput2, generateEventInput3,
                                    generateEventInput4))
                            .command(generateEventCommand).build())
                    .build();
    }

    public static ProcessConfiguration getEventFileProjectOne() {
            final Input generateEventInput1 = new Input("EventGenerationFile", "attachment", nullSet)
                    .setLabel("File for event generation (MANDATORY)");
            final Input generateEventInput2 = new Input("EventGenerationSheet", "text", nullSet)
                    .setLabel("Sheet name from file for event generation (OPTIONAL, by default get first sheet)");
            final Input generateEventInput3 = new Input("EventGenerationScenario", "text", nullSet)
                    .setLabel("Scenario for event generation (OPTIONAL, by default get all scenarios from sheet)");
            final Input generateEventInput4 = new Input("EventGenerationTestCase", "text", nullSet)
                    .setLabel("Description name (Test case) for event generation (OPTIONAL, by default get all from "
                            + "for scenario)");
            final Command generateEventCommand = new Command("SSH_GenerateEventFromFile", "SSH_GenerateEventFromFile",
                    systemBillingSystem, listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
            generateEventCommand.setNamesOfFilesForGeneration(ImmutableList.of("event_file_sub_${Timestamp"
                                    + "(YYYYMMdd_hhmmss)}",
                            "control_file_${Timestamp(YYYYMMdd_hhmmss)}"))
                    .setSaveGeneratedFilesToParameter("generatedFiles").setFileExtension(".dat")
                    .setPathForUpload(":infinys_root/")
                    .setEthalonFilesForGeneration(ImmutableList.of("eventFromFile_templ_Project_One.dat", "control_templ"
                            + ".dat"))
                    .setRegexpForFileRetrieve("(?<=.to ).*(?<=.dat_ERR_.{12})")
                    .setValues(listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("EVENTS FROM FILE PROJECT THREE BASED")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("EVENTS FROM FILE PROJECT ONE")
                            .inputs(ImmutableList.of(generateEventInput1, generateEventInput2, generateEventInput3,
                                    generateEventInput4))
                            .command(generateEventCommand).build())
                    .build();
    }

    public static ProcessConfiguration getEventFileProjectTwo() {
            final Command generateEventCommand = new Command("SSH_GenerateEventFromFile", "SSH_GenerateEventFromFile",
                    systemBillingSystem, listToSet("FID -a '-sourceDir \":infinys_root\"'"));
            generateEventCommand.setNamesOfFilesForGeneration(ImmutableList.of("event_file_sub_${Timestamp"
                            + "(YYYYMMdd_hhmmss)}"))
                    .setSaveGeneratedFilesToParameter("generatedFiles").setFileExtension(".dat")
                    .setCharsetForGeneratedFile("ASCII")
                    .setPathForUpload(":infinys_root/")
                    .setEthalonFilesForGeneration(ImmutableList.of("eventFromFile_templ_Project_Two.dat"))
                    .setRegexpForFileRetrieve("(?<=.to ).*(?<=.dat_ERR_.{12})");
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("EVENTS FROM FILE PROJECT TWO")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("EVENTS FROM FILE PROJECT TWO")
                            .command(generateEventCommand).build())
                    .build();
    }

    public static ProcessConfiguration getEventTestData() {
            final Command eventTestDataCommand = new Command("Validate", "EVENT_TEST_DATA", systemBillingSystem,
                    listToSet("{ FID -a '-sourceDir \":infinys_root\"' ; EFD ; }"));
            eventTestDataCommand.setDisplayDownloadedFileContent(false);
            eventTestDataCommand.setSaveGeneratedFilesToParameter("generatedFiles");
            eventTestDataCommand.setPathForUpload(":infinys_root/");
            eventTestDataCommand.setRegexpForFileRetrieve("(?<=.to ).*(?<=.dat_ERR_.{12})");
            eventTestDataCommand.setFileExtension(".dat");
            eventTestDataCommand.setNamesOfFilesForGeneration(
                    Arrays.asList("event_file_sub_${Timestamp(YYYYMMdd_hhmmss_S)}",
                            "control_file_${Timestamp(YYYYMMdd_hhmmss)}"));
            final TestDataParams testDataParams = new TestDataParams();
            testDataParams.setEventTemplate(":EVENT_TEMPLATE");
            testDataParams.setEventFileTemplate("event_testData.dat");
            testDataParams.setEventParameterInTemplate("EVENT_LINES");
            testDataParams.setControlFileTemplate("control_templ.dat");
            testDataParams.setEventFileForEachDescription(false);
            eventTestDataCommand.setTestDataParams(testDataParams);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("TestData_Event")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("TestData_Event")
                            .command(eventTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getVerifyTestData() {
            final Command verifyTestDataCommand = new Command("Validate", "VALIDATE_TEST_DATA", systemBillingSystem,
                    listToSet("echo 1"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("Validate test data")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Validate test data")
                            .command(verifyTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getSoapTestData() {
            final Command soapTestDataCommand = new Command("SoapTestData", "SOAP_FROM_TEST_DATA", systemBillingSystem,
                    listToSet("soapExecution"));
            soapTestDataCommand.setSoap(new Soap("soapEndpoint"));
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("EVENTS FROM FILE PROJECT THREE BASED")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Soap test data")
                            .command(soapTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getRestTestData() {
            final Command restTestDataCommand = new Command("RestTestData", "REST_FROM_TEST_DATA", systemBillingSystem,
                    listToSet("restExecution"));
            restTestDataCommand.setRest(Rest.builder().build());
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("Rest test data")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Rest test data")
                            .command(restTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getSshTestData() {
            final Command sshTestDataCommand = new Command("SshTestData", "SSH_FROM_TEST_DATA", systemBillingSystem, null);
            LinkedHashMap<String, String> atpValues = new LinkedHashMap<>();
            atpValues.put("column1", "sshExecution");
            sshTestDataCommand.setAtpValues(atpValues);
            LinkedHashMap<String, String> variablesToExtractFromLog = new LinkedHashMap<>();
            variablesToExtractFromLog.put("COLUMN_EXTRACTED_FROM_LOG_1", "text([\\S\\s]*)3");
            variablesToExtractFromLog.put("COLUMN_EXTRACTED_FROM_LOG_2", "([\\S\\s]*2)");
            sshTestDataCommand.setVariablesToExtractFromLog(variablesToExtractFromLog);
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("Ssh test data")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Ssh test data")
                            .command(sshTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getSqlTestData() {
            final Command sqlTestDataCommand = new Command("SqlTestData", "SQL_FROM_TEST_DATA", systemBillingSystem, null);
            LinkedHashMap<String, String> atpValues = new LinkedHashMap<>();
            atpValues.put("column1", "sqlExecution");
            sqlTestDataCommand.setAtpValues(atpValues);
            HashMap<String, String> variablesToExtractFromLog = new HashMap<>();
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("Sql test data")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("Sql test data")
                            .command(sqlTestDataCommand).build())
                    .build();
    }

    public static ProcessConfiguration getUploadFile() {
            final Command uploadFileCommand = new Command("UploadFile", "SSH_UploadFile", systemBillingSystem,
                    listToSet("cd :pathForUpload; ls"))
                    .setFilesForUpload(ImmutableList.of(":input_file"))
                    .setPathForUpload(":pathForUpload");
            final Input uploadFileInput1 = new Input("input_file", "attachment", nullSet)
                    .setLabel("Input File");
            final Input uploadFileInput2 = new Input("pathForUpload", "text", nullSet)
                    .setLabel("Path For Upload");
        return ProcessConfiguration.builder()
                    .id(UUID.randomUUID())
                    .name("SSH_UploadFile")
                    .processSettings(new ProcessSettings().toBuilder()
                            .name("SSH_UploadFile")
                            .inputs(ImmutableList.of(uploadFileInput1, uploadFileInput2))
                            .command(uploadFileCommand).build())
                    .build();
    }

    /**
     * If you changed any input for functions from default folder (with .json),
     * probably you will need to add new inputs here.
     */
    public static ArrayList<Validation> getDefaultCurrentStatement() {
        ArrayList<Validation> validations = new ArrayList<>();
        validations.add(Validation.builder().type("SQL").system("Billing System").values(new LinkedHashSet<String>() {{
            add("select * from gparams where name like '%SYSdate%'");
        }}).saveToWordFile(true).build());
        return validations;
    }

    /**
     * If you changed any input for functions from default folder (with .json),
     * probably you will need to add new inputs here.
     */
    public static ArrayList<Input> getDefaultInputs() {
        ArrayList<Input> inputs = new ArrayList<>();
        inputs.add(Input.builder().label("SYSdateValue YYYYMMDD hhmmss").mask("YYYYMMDD").name("SYSdateValue")
                .type("date").required(true).system("Billing System").build());
        inputs.add(Input.builder().label("Bill Period").name("bill_period").required(true)
                .type("list").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("1");
                    add("2");
                }}).build());
        inputs.add(Input.builder().label("Extract Dat").name("extractDat")
                .type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Start Dat").name("startDat")
                .type("text").system("Billing System").build());
        inputs.add(Input.builder().label("End Dat").name("endDat")
                .type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Payment Amount").name("payment_amount")
                .type("text").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("100.00");
                }}).build());
        inputs.add(Input.builder().label("Payment Transaction ID").name("payment_transaction_id")
                .type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Actual Payment Date").name("actual_payment_date")
                .type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Endpoint").name("soapEndpoint")
                .type("text").system("ECA").build());
        inputs.add(Input.builder().label("Request").name("soapRequest")
                .type("ace").system("ECA").build());
        inputs.add(Input.builder().label("File for request").name("soapRequestFile")
                .type("attachment").system("ECA").build());
        inputs.add(Input.builder().label("Event types").name("template")
                .type("checkBoxGroup").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("1");
                    add("2");
                }}).build());
        inputs.add(Input.builder().label("Event Source").name("Calling")
                .type("text").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("32553697560");
                }}).build());
        inputs.add(Input.builder().label("Event Date").name("EVENT_DTM")
                .type("text").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("2019/07/05-05-35-28.01");
                }}).build());
        inputs.add(Input.builder().label("MO").name("MO")
                .type("text").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("MO_1");
                }}).build());
        inputs.add(Input.builder().label("Destination").name("Destination")
                .type("text").system("Billing System").values(new LinkedHashSet<String>() {{
                    add("32184854784");
                }}).build());
        inputs.add(Input.builder().label("New account number (filled by 0 in the start while length < 20)").
                name("newAccountNumber").type("text").system("Billing System").maxLength(20).build());
        inputs.add(Input.builder().label("File for event generation (MANDATORY)").name("EventGenerationFile")
                .type("attachment").system("Billing System").build());
        inputs.add(Input.builder().label("Sheet name from file for event generation (OPTIONAL, by default get first sheet)")
                .name("EventGenerationSheet").type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Test case for event generation (OPTIONAL, by default get all TCs from sheet)")
                .name("EventGenerationTestCase").type("text").system("Billing System").build());
        inputs.add(Input.builder().label("Scenario for event generation (OPTIONAL, by default get all scenarios from test case)")
                .name("EventGenerationScenario").type("text").system("").build());
        return inputs;
    }
    public static CompoundConfiguration getDefaultCompound() {
        LinkedList<ProcessConfiguration> processes = new LinkedList<>();
        processes.add((ProcessConfiguration) SerializationUtils.clone(getSql()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getBg()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getTransfer()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getMultiDownload()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getCheck()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getGenerationPp()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getUploadFile()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getDefaultRest()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getSoap()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getEventS()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getFillInput()));
        processes.add((ProcessConfiguration) SerializationUtils.clone(getEventFileProjectThree()));
        return CompoundConfiguration.builder()
                .id(UUID.randomUUID())
                .name("Compound Name")
                .processes(processes)
                .build();
    }
}
