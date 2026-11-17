package com.restaurant.pos.config;

import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderFiredEvent;
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
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // Producer Configuration. The outbox relay sends the stored JSON string as is.
    // max.block.ms: the relay thread waits at most 1 s for metadata when the broker is down.
    @Bean
    public ProducerFactory<String, String> outboxProducerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 1000);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, String> outboxKafkaTemplate() {
        return new KafkaTemplate<>(outboxProducerFactory());
    }

    // Consumer Configuration - Simple!
    // Consumer Factory for OrderFiredEvent
    @Bean
    public ConsumerFactory<String, OrderFiredEvent> orderFiredConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kds-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.restaurant.pos.events.OrderFiredEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // Consumer Factory for ItemStatusChangedEvent
    @Bean
    public ConsumerFactory<String, ItemStatusChangedEvent> itemStatusConsumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "kds-consumer-group");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.restaurant.pos.events.ItemStatusChangedEvent");
        return new DefaultKafkaConsumerFactory<>(config);
    }

    // Listener factory for OrderFiredEvent
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderFiredEvent> orderFiredKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderFiredEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(orderFiredConsumerFactory());
        return factory;
    }

    // Listener factory for ItemStatusChangedEvent
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ItemStatusChangedEvent> itemStatusKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, ItemStatusChangedEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(itemStatusConsumerFactory());
        return factory;
    }
}