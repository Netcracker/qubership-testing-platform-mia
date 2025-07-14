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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.http.HttpResponse;
import org.qubership.atp.mia.exceptions.fileservice.ReadFailIoExceptionDuringOperation;
import org.qubership.atp.mia.exceptions.itflite.IncorrectProcessNameException;
import org.qubership.atp.mia.model.Constants;
import org.qubership.atp.mia.model.configuration.Switcher;
import org.qubership.atp.mia.model.environment.Server;
import org.qubership.atp.mia.model.exception.ErrorCodes;
import org.qubership.atp.mia.model.exception.MiaException;
import org.qubership.atp.mia.model.pot.db.DbType;
import org.slf4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Utils {

    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter
            .ofPattern("'_'MMddHHmmssSSS", Locale.US);
    private static final String REQUEST_TOO_BIG_ERROR = "Connection terminated as request was larger than";

    /**
     * Check process name for valid value.
     *
     * @param name name of Process
     */
    public static void nameProcessValidator(String name) {
        if (!name.matches("^[\\w ]+$")) {
            throw new IncorrectProcessNameException(name);
        }
    }

    /**
     * Bounds check place for List.
     *
     * @param list the list
     * @param place desired position
     * @return bounded position
     */
    public static int correctPlaceInList(List<?> list, int place) {
        return Math.max(0, Math.min(place, list.size()));
    }

    /**
     * This method takes a type of database from environment and returns it as an enum.
     *
     * @param server - class which allows us to manipulate with environment.
     * @return database type, defined in enum in SqlExecutor.class.
     */
    public static DbType getDbType(Server server) {
        String dbTypeEnvironmentProperty = "db_type";
        String propertyFromServer = server.getProperty(dbTypeEnvironmentProperty);
        DbType dbType;
        if (propertyFromServer == null || propertyFromServer.isEmpty()) {
            dbType = DbType.ORACLE;
        } else {
            try {
                dbType = DbType.valueOf(propertyFromServer.toUpperCase());
            } catch (Exception e) {
                dbType = DbType.UNDEFINED;
            }
        }
        return dbType;
    }

    /**
     * Behaves like a apache IOUtils.
     */
    public static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log.warn("Can not close resource: " + closeable, e);
        }
    }

    @Nonnull
    public static <T> Stream<T> streamOf(@Nonnull Iterator<T> iterator) {
        Spliterator<T> split = Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED);
        return StreamSupport.stream(split, false);
    }

    /**
     * Get paths to files from log.
     */
    public static ArrayList<String> getPathToFileOutOfLog(String outputFilePath, String regex) {
        ArrayList<String> paths = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex);
        try {
            String result = new String(Files.readAllBytes(Paths.get(outputFilePath)), StandardCharsets.UTF_8);
            Matcher matcher = pattern.matcher(result);
            while (matcher.find()) {
                final String pathToFileFound = matcher.group();
                log.info("Found path to file in log: {}", pathToFileFound);
                paths.add(pathToFileFound);
            }
        } catch (IOException e) {
            throw new ReadFailIoExceptionDuringOperation(outputFilePath, e);
        }
        return paths;
    }

    public static String getFileNameFromPath(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    public static String getTimestampFile() {
        return DATE_FORMATTER.format(LocalDateTime.now());
    }

    public static String getTimestamp() {
        return String.valueOf(System.currentTimeMillis());
    }

    /**
     * Gets file name without extension.
     *
     * @param fileName fileName
     * @return file name without extension
     */
    public static String getFileNameWithoutExtension(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    /**
     * Add timestamp to file name.
     */
    public static String getFileNameWithTimestamp(String fileName) {
        String newFileName = getFileNameWithoutExtension(fileName);
        String ext = fileName.substring(fileName.lastIndexOf("."));
        return newFileName + getTimestampFile() + ext;
    }

    /**
     * check content-disposition is present in http response header.
     */
    public static boolean isHeaderNamePresent(HttpResponse httpResponse, String headerName) {
        return Arrays.stream(httpResponse.getAllHeaders())
                .anyMatch(h -> h.getName().equalsIgnoreCase(headerName));
    }

    /**
     * Get header value from the response header by passing header name as input.
     */
    public static String getHeaderValue(HttpResponse httpResponse, String headerName) {
        return Arrays.stream(httpResponse.getAllHeaders())
                .filter(h -> h.getName().equalsIgnoreCase(headerName))
                .map(v -> v.getValue()).reduce("", String::concat);
    }

    /**
     * evaluate url using query params.
     */
    public static String urlEvaluator(String url, Map<String, String> parameters) {
        if (parameters != null) {
            url = url + "?" + parameters.keySet().stream().map(key -> key + "=" + parameters.get(key))
                    .collect(Collectors.joining("&"));
        }
        return url;
    }

    /**
     * Get header in form of supported by MIA .
     */
    public static String prepareRestHeaders(Map<String, String> headers) {
        if (headers != null) {
            return headers.keySet().stream()
                    .map(key -> key + ":" + headers.get(key))
                    .collect(Collectors.joining("\n"));
        }
        return "";
    }

    /**
     * Get variables from log.
     */
    public static String getFirstGroupFromStringByRegexp(String string, String regex) {
        String value = null;
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(string);
        while (matcher.find()) {
            value = matcher.group(1);
        }
        return value;
    }

    /**
     * Check Condition expected result and actual result.
     */
    public static boolean checkCondition(String erValue, String arValue) {
        boolean equality = true;
        char operator = erValue.length() > 0 ? erValue.charAt(0) : '='; // '=' can be replaced with any char unless
        // the below code is changed. It's to avoid throwing error if 'erValue' is empty.
        int intValue;
        try {
            switch (operator) {
                case '>':
                    intValue = Integer.parseInt(erValue.substring(1).trim());
                    if (Integer.parseInt(arValue) <= intValue) {
                        equality = false;
                    }
                    break;
                case '<':
                    intValue = Integer.parseInt(erValue.substring(1).trim());
                    if (Integer.parseInt(arValue) >= intValue) {
                        equality = false;
                    }
                    break;
                default:
                    equality = arValue.matches(erValue);
                    break;
            }
        } catch (NumberFormatException e) {
            equality = false;
        }
        return equality;
    }

    /**
     * Throws an exception and adds an error to the log.
     */
    public static MiaException error(Logger log, ErrorCodes errorCode, Object... params) {
        if (errorCode == null) {
            log.error("Unknown error code! Please fix this error.");
            errorCode = ErrorCodes.MIA_8000_UNEXPECTED_ERROR;
        }
        if (!Objects.isNull(params) && params.length > 1) {
            try {
                Exception e = (Exception) params[0];
                log.error(errorCode.getMessage(), e);
            } catch (ClassCastException exception) {
                // ignore
            }
        }
        return new MiaException(errorCode, params);
    }

    public static LinkedHashSet<String> listToSet(List<String> list) {
        return new LinkedHashSet<>(list);
    }

    public static LinkedHashSet<String> listToSet(String... list) {
        return listToSet(Arrays.asList(list));
    }

    public static Switcher getSystemSwitcherByName(Constants.SystemSwitcherNames name,
                                                   List<Switcher> systemSwitchers) {
        return systemSwitchers.stream().filter(switcher -> switcher.getName().equals(name.toString()))
                .findFirst().get();
    }

    /**
     * Transforms xml response to pretty string.
     */
    public static String getPrettyStringFromXml(String xmlData) {
        log.info("Get pretty string from Xml");
        log.debug("Xml to transform: " + xmlData);
        StreamResult xmlOutput;
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("indent", "yes");
            xmlOutput = new StreamResult(new StringWriter());
            Source xmlInput = new StreamSource(new StringReader(xmlData));
            transformer.transform(xmlInput, xmlOutput);
            xmlData = xmlOutput.getWriter().toString();
            log.trace("Pretty string: \n{}", xmlData);
        } catch (Exception e) {
            log.error("Errors occurred while transforming XML response to pretty string.");
        }
        return xmlData;
    }

    /**
     * Excel cell have a limit for length of cell.
     *
     * @param stringForCell string for cell
     * @return string
     */
    public static String maxExcelString(String stringForCell) {
        return stringForCell.length() > 32670
                ? stringForCell.substring(0, 32670)
                : stringForCell;
    }

    /**
     * Parses long value from string.
     * If {@link NumberFormatException} occur, then default value is used.
     * Default use while parsing {@code @Value} annotation.
     *
     * @param valueToParse string value to parse.
     * @param defaultValue if {@code valueToParse} incorrect use this one.
     * @param valueName    field name to show in logs.
     * @return parsed value.
     */
    public static long parseLongValueOrDefault(String valueToParse, long defaultValue, String valueName) {
        long tempValue;
        try {
            tempValue = Long.parseLong(valueToParse);
        } catch (NumberFormatException e) {
            tempValue = defaultValue;
            log.debug("Error can't parse {} value [{}], use the standard [{}]",
                    valueName, valueToParse, defaultValue);
        }
        return tempValue;
    }

    /**
     * Need to fill empty exception before return to client side.
     *
     * @param e source exception
     * @return the source exception or filled with message in case if the source one was empty.
     */
    public static Exception handleException(Exception e) {
        String msg = e.getMessage() == null
                ? ErrorCodes.MIA_8000_UNEXPECTED_ERROR.getMessage(e)
                : e.getMessage();
        return msg.contains(REQUEST_TOO_BIG_ERROR)
                ? Utils.error(log, ErrorCodes.MIA_8002_REQUEST_TOO_BIG)
                : new Exception(msg, e);
    }

    /**
     * Wait for provided number of milliSecondsTime.
     */
    public static void sleepForTimeInMillSeconds(long millSeconds) {
        if (millSeconds > 0) {
            try {
                log.debug("Waiting for {} milli seconds", millSeconds);
                Thread.sleep(millSeconds);
            } catch (InterruptedException e) {
                log.debug("InterruptedException while waiting for above timeout", e);
            }
        }
    }

    /**
     * Calculates difference between startDateTimestamp and current timestamp.
     *
     * @param startDateTimestamp input parameter, timestamp in ms
     * @return difference between startDateTimestamp and current timestamp.
     */
    public static long calculateDuration(long startDateTimestamp) {
        return System.currentTimeMillis() - startDateTimestamp;
    }
}
