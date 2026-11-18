package com.restaurant.pos.kds;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Starts the KDS consumers and retries until they are up. Building a Kafka consumer throws when the bootstrap
 * host does not resolve, which would stop the whole app at startup; the POS must run without Kafka (the outbox
 * holds the events), so the listener containers do not auto-start. Spring marks the parent container as running
 * before that failure, so a later start() would be a no-op; stop() resets it.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KdsConsumerStarter {

    private final KafkaListenerEndpointRegistry registry;

    @Scheduled(fixedDelay = 5000)
    public void startConsumers() {
        for (MessageListenerContainer container : registry.getListenerContainers()) {
            if (container.isRunning()) {
                continue;
            }
            try {
                container.start();
            } catch (Exception e) {
                log.warn("KDS consumer {} not started, retrying: {}", container.getListenerId(), e.getMessage());
                container.stop();
            }
        }
    }
}
