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

package org.qubership.atp.mia.kafka.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.qubership.atp.mia.kafka.model.KafkaRequestImport;
import org.qubership.atp.mia.kafka.model.notification.EnvironmentUpdateEvent;
import org.qubership.atp.mia.kafka.model.notification.ProjectEvent;
import org.qubership.atp.mia.kafka.producers.MiaExecutionFinishProducer;
import org.qubership.atp.mia.kafka.service.ItfImportFinishNotificationService;
import org.qubership.atp.mia.model.impl.ExecutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.AlwaysRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ConditionalOnProperty(
        value = "kafka.enable",
        havingValue = "true"
)
@RequiredArgsConstructor
public class KafkaConfiguration {

    public static final String CATALOG_PROJECT_EVENT_CONTAINER_FACTORY = "catalogProjectEventContainerFactory";
    public static final String ITF_IMPORT_CONTAINER_FACTORY = "itfImportContainerFactory";
    public static final String MIA_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME =
            "miaExecutionFinishKafkaContainerFactory";
    public static final String ENVIRONMENT_UPDATE_EVENT_CONTAINER_FACTORY = "environmentUpdateEventContainerFactory";
    @Value("${kafka.itf.import.end.partitions:1}")
    public int kafkaExecutionEndPartitions;
    @Value("${kafka.itf.import.end.replicas:3}")
    public int kafkaExecutionEndReplicas;
    @Value("${kafka.mia.execution.finish.partitions:1}")
    public int kafkaMiaExecutionFinishPartitions;
    @Value("${kafka.mia.execution.finish.replicas:3}")
    public int kafkaMiaExecutionFinishReplicas;
    @Value("${kafka.mia.execution.finish.topic}")
    public String kafkaMiaExecutionFinishTopic;
    @Value("${kafka.retry.backoff.period}")
    private String backOffPeriod;
    @Value("${kafka.itf.import.end.topic}")
    private String kafkaItfImportFinishTopic;
    @Value("${kafka.server}")
    private String kafkaServer;

    /**
     * Kafka consumer configuration.
     */
    @Bean
    public Map<String, Object> consumerConfigJsonValue() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // we are manually committing the message
        return props;
    }

    /**
     * Factory for kafka start execution event topic listener for itfLite.
     */
    @Bean(CATALOG_PROJECT_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<UUID, ProjectEvent> containerFactoryCatalogProjectEvent() {
        ConcurrentKafkaListenerContainerFactory<UUID, ProjectEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(catalogConsumerFactory());
        return containerFactory;
    }

    /**
     * Factory for kafka start execution event topic listener for itfLite.
     */
    @Bean(ITF_IMPORT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<UUID, String> containerFactoryItfImport() {
        ConcurrentKafkaListenerContainerFactory<UUID, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(itfImportConsumerFactory());
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        factory.setRetryTemplate(retryTemplate());
        factory.setStatefulRetry(true);
        return factory;
    }

    /**
     * Container factory for kafka listener.
     *
     * @return concurrent kafka listener container factory
     */
    @Bean(MIA_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME)
    public ConcurrentKafkaListenerContainerFactory<UUID, ExecutionResponse> containerFactoryMiaExecutionFinish() {
        log.debug("Start MIA execution finish kafka container factory: {}",
                MIA_EXECUTION_FINISH_CONTAINER_FACTORY_BEAN_NAME);
        ConcurrentKafkaListenerContainerFactory<UUID, ExecutionResponse> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(miaExecutionFinishConsumerFactory());
        return factory;
    }

    /**
     * Factory for environment update listener from environment service.
     */
    @Bean(ENVIRONMENT_UPDATE_EVENT_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<UUID, EnvironmentUpdateEvent>
    containerFactoryEnvironmentUpdateEvent() {
        ConcurrentKafkaListenerContainerFactory<UUID, EnvironmentUpdateEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(environmentUpdatesConsumerFactory());
        return containerFactory;
    }

    /**
     * Creates ItfImportFinishNotificationService.
     *
     * @return ItfImportFinishNotificationService.
     */
    @Bean
    public ItfImportFinishNotificationService endItfImportNotificationService() {
        return new ItfImportFinishNotificationService(kafkaItfImportFinishTopic, kafkaTemplate());
    }

    /**
     * Custom kafka consumer factory.
     */
    @Bean
    public ConsumerFactory itfImportConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigJsonValue(), new UUIDDeserializer(),
                new JsonDeserializer<>(KafkaRequestImport.class, false));
    }

    /**
     * KafkaTemplate constructor.
     *
     * @return new Kafka template
     */
    @Bean
    public KafkaTemplate<UUID, String> kafkaTemplate() {
        log.info("Create KafkaTemplate bean");
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public MiaExecutionFinishProducer miaExecutionFinishProducer() {
        return new MiaExecutionFinishProducer(producerConfig(JsonSerializer.class));
    }

    /**
     * Create DefaultKafkaProducerFactory.
     *
     * @return ProducerFactory.
     */
    @Bean
    public ProducerFactory<UUID, String> producerFactory() {
        log.info("ProducerFactory");
        return new DefaultKafkaProducerFactory<>(producerConfig(StringSerializer.class));
    }

    /**
     * Create RetryTemplate with AlwaysRetryPolicy.
     *
     * @return RetryTemplate.
     */
    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setRetryPolicy(new AlwaysRetryPolicy());
        FixedBackOffPolicy fixedBackOffPolicy = new FixedBackOffPolicy();
        fixedBackOffPolicy.setBackOffPeriod(Long.parseLong(backOffPeriod));
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy);
        return retryTemplate;
    }

    /**
     * Configure kafka end execution topic.
     *
     * @return new topic.
     */
    @Bean
    public NewTopic topicItfImportFinishEnd() {
        return TopicBuilder.name(kafkaItfImportFinishTopic)
                .partitions(kafkaExecutionEndPartitions)
                .replicas(kafkaExecutionEndReplicas)
                .build();
    }

    /**
     * Configure kafka end mia execution finish topic.
     *
     * @return new topic.
     */
    @Bean
    public NewTopic topicMiaExecutionFinish() {
        return TopicBuilder.name(kafkaMiaExecutionFinishTopic)
                .partitions(kafkaMiaExecutionFinishPartitions)
                .replicas(kafkaMiaExecutionFinishReplicas)
                .build();
    }

    private ConsumerFactory<UUID, ProjectEvent> catalogConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigJsonValue(), new UUIDDeserializer(),
                new JsonDeserializer<>(ProjectEvent.class, false));
    }

    private ConsumerFactory<UUID, ExecutionResponse> miaExecutionFinishConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigJsonValue(), new UUIDDeserializer(),
                new JsonDeserializer<>(ExecutionResponse.class, false));
    }

    private ConsumerFactory<UUID, EnvironmentUpdateEvent> environmentUpdatesConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigJsonValue(), new UUIDDeserializer(),
                new JsonDeserializer<>(EnvironmentUpdateEvent.class, false));
    }

    private Map<String, Object> producerConfig(Class<?> clazz) {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaServer);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, clazz);
        return props;
    }
}
