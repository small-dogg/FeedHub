package world.jerry.feedhub.collector.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import world.jerry.feedhub.collector.message.CrawlRequestMessage;
import world.jerry.feedhub.common.command.SyncAllFeedsCommand;
import world.jerry.feedhub.common.command.SyncRssFeedCommand;
import world.jerry.feedhub.common.event.Event;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Consumer/Producer 설정
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

    // ========== CrawlRequestMessage Consumer ==========

    @Bean
    public ConsumerFactory<String, CrawlRequestMessage> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<CrawlRequestMessage> deserializer = new JsonDeserializer<>(CrawlRequestMessage.class,
                objectMapper());
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("world.jerry.feedhub.*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CrawlRequestMessage> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, CrawlRequestMessage> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ========== SyncRssFeedCommand Consumer ==========

    @Bean
    public ConsumerFactory<String, SyncRssFeedCommand> syncCommandConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<SyncRssFeedCommand> deserializer = new JsonDeserializer<>(SyncRssFeedCommand.class,
                objectMapper());
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("world.jerry.feedhub.*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SyncRssFeedCommand> syncCommandListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SyncRssFeedCommand> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(syncCommandConsumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }

    // ========== SyncAllFeedsCommand Consumer ==========

    @Bean
    public ConsumerFactory<String, SyncAllFeedsCommand> syncAllCommandConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        JsonDeserializer<SyncAllFeedsCommand> deserializer = new JsonDeserializer<>(SyncAllFeedsCommand.class,
                objectMapper());
        deserializer.setRemoveTypeHeaders(false);
        deserializer.addTrustedPackages("world.jerry.feedhub.*");
        deserializer.setUseTypeMapperForKey(true);

        return new DefaultKafkaConsumerFactory<>(
                props,
                new StringDeserializer(),
                deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, SyncAllFeedsCommand> syncAllCommandListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, SyncAllFeedsCommand> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(syncAllCommandConsumerFactory());
        factory.setConcurrency(1); // Single consumer for sync-all to avoid duplicate processing
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
