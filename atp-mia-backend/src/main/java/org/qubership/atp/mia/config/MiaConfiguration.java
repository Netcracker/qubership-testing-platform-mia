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

package org.qubership.atp.mia.config;

import static org.qubership.atp.mia.model.Constants.REDIRECT_URL;
import static org.qubership.atp.mia.model.Constants.SERVICE_PATH_DEFAULT;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.modelmapper.AbstractConverter;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.qubership.atp.mia.kafka.producers.MiaExecutionFinishProducer;
import org.qubership.atp.mia.model.configuration.CompoundConfiguration;
import org.qubership.atp.mia.model.configuration.ProcessConfiguration;
import org.qubership.atp.mia.model.configuration.SectionConfiguration;
import org.qubership.atp.mia.model.file.ProjectDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class MiaConfiguration {

    @Value("${configPath:prod}")
    private String profile;
    @Value("${atp.service.internal:false}")
    private boolean isInternalGateWayEnabled;
    @Value("${catalogue.url}")
    private String catalogueUrl;
    @Value("${atp.service.path:/api/atp-mia/v1}")
    private String servicePath;

    /**
     * MIA config path for test or production.
     *
     * @return config path
     */
    @Bean("miaConfigPath")
    public Path miaConfigPath() {
        if ("test".equals(profile)) {
            return Paths.get("src", "main", "config");
        }
        return Paths.get("config");
    }

    @ConditionalOnProperty(
            value = "kafka.enable",
            havingValue = "false"
    )
    @Bean
    public MiaExecutionFinishProducer miaExecutionFinishProducer() {
        return new MiaExecutionFinishProducer(null);
    }

    @Bean
    public ExecutorService executorServiceForSql(
            @Value("${mia.sql.threadPool.minThreads:#{50}}") int minThreads,
            @Value("${mia.sql.threadPool.maxThreads:#{100}}") int maxThreads,
            @Value("${mia.sql.threadPool.aliveTimeMs:#{0L}}") long aliveTime) {
        return new ThreadPoolExecutor(minThreads, maxThreads, aliveTime, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>());
    }

    /**
     * FileDownloadPrefix bean.
     */
    @Bean("fileDownloadPrefix")
    public String fileDownloadPrefix() {
        String internalGatewayFileDownloadPathPrefix = "";
        if (isInternalGateWayEnabled) {
            if (catalogueUrl != null && catalogueUrl.length() > 0) {
                if (servicePath != null && servicePath.length() > 0) {
                    if (servicePath.endsWith("/**")) {
                        servicePath = servicePath.substring(0, servicePath.length() - 3);
                    }
                } else {
                    log.warn("Service Path is empty & setting to default value");
                    servicePath = SERVICE_PATH_DEFAULT;
                }
                log.debug("InternalGateway is Enabled & setting the file download URL prefix");
                try {
                    internalGatewayFileDownloadPathPrefix =
                            new URI(catalogueUrl + REDIRECT_URL + servicePath).normalize().toString();
                } catch (URISyntaxException e) {
                    log.error("Problem in download file URL Prefix Formation. {}", e.getMessage());
                }
                log.info("File download URL Prefix set to {}", internalGatewayFileDownloadPathPrefix);
            } else {
                log.error("invalid catalog URL. Please set environment variable FEIGN_ATP_CATALOGUE_URL");
                log.warn("File download URL Prefix set to empty");
            }
        } else {
            log.info("InternalGateway is not enabled. File download URL prefix did not set and not required");
        }
        return internalGatewayFileDownloadPathPrefix;
    }

    /**
     * MiaEntityUrlFormat bean.
     */
    @Bean("miaEntityUrlFormat")
    public String miaEntityUrlFormat() {
        String entityUrlPrefix = Strings.isNullOrEmpty(catalogueUrl) ? "" : catalogueUrl;
        return entityUrlPrefix + "/project/%s/mia/execution?entityId=%s";
    }

    /**
     * MIA POT template for test or production.
     *
     * @return pot template
     */
    @Bean("miaPotTemplate")
    public String miaPotTemplate() {
        if ("test".equals(profile)) {
            return ".." + File.separator + "atp-mia-distribution" + File.separator + "src" + File.separator + "main"
                    + File.separator + "resources" + File.separator + "POT_template.docx";
        }
        return "config" + File.separator + "POT_template.docx";
    }

    /**
     * Model mapper been with Converter Object to UUID.
     *
     * @return {@link ModelMapper}
     */
    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        Converter<SectionConfiguration, UUID> sectionConfigurationUuidConverter =
                new AbstractConverter<SectionConfiguration, UUID>() {
                    protected UUID convert(SectionConfiguration source) {
                        if (source != null) {
                            return source.getId();
                        }
                        return null;
                    }
                };
        Converter<ProcessConfiguration, UUID> processConfigurationUuidConverter =
                new AbstractConverter<ProcessConfiguration, UUID>() {
                    protected UUID convert(ProcessConfiguration source) {
                        if (source != null) {
                            return source.getId();
                        }
                        return null;
                    }
                };
        Converter<CompoundConfiguration, UUID> compoundConfigurationUuidConverter =
                new AbstractConverter<CompoundConfiguration, UUID>() {
                    protected UUID convert(CompoundConfiguration source) {
                        if (source != null) {
                            return source.getId();
                        }
                        return null;
                    }
                };
        Converter<ProjectDirectory, UUID> projectDirectoryUuidConverter =
                new AbstractConverter<ProjectDirectory, UUID>() {
                    protected UUID convert(ProjectDirectory source) {
                        if (source != null) {
                            return source.getId();
                        }
                        return null;
                    }
                };
        modelMapper.addConverter(sectionConfigurationUuidConverter);
        modelMapper.addConverter(processConfigurationUuidConverter);
        modelMapper.addConverter(compoundConfigurationUuidConverter);
        modelMapper.addConverter(projectDirectoryUuidConverter);
        modelMapper.getConfiguration().setSkipNullEnabled(true);
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return modelMapper;
    }
}
