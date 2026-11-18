package com.restaurant.pos;

import com.restaurant.pos.kds.KdsConsumerStarter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The POS must start and take fires while the Kafka host does not resolve (the outbox holds the events).
 * <p>
 * The broker override makes this a second Spring context next to the shared one, which {@link IntegrationTest}
 * warns against: two outbox relays on one Postgres. So the context is closed when this class ends, and nothing
 * here asserts on outbox row status or attempts, because the shared context's relay (if cached) may publish
 * this class's rows.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StartWithoutKafkaIT extends IntegrationTest {

    @Autowired private KafkaListenerEndpointRegistry registry;
    @Autowired private KdsConsumerStarter starter;

    private static final String BURGER = "bb4d597e-0524-4ac7-ace6-749d8a36d38f";

    @DynamicPropertySource
    static void unresolvableKafka(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", () -> "kafka-does-not-resolve.invalid:9092");
    }

    @Test
    void theBackendStartsAndTakesAFire_whenTheKafkaHostDoesNotResolve() throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "DRAFT", "0.00", null);
        jdbc.update("""
                INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price, subtotal)
                SELECT ?, id, 1, base_price, base_price FROM menu_items WHERE id = ?""",
                orderId, UUID.fromString(BURGER));

        mvc.perform(authed(post("/api/orders/" + orderId + "/fire"), "server1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FIRED"));
    }

    @Test
    void aFailedConsumerStart_leavesTheContainersStopped_soTheNextTickRetries() {
        assertThat(registry.getListenerContainers()).isNotEmpty();

        starter.startConsumers(); // fails on the unresolvable host

        // Awaited because the scheduled tick may be mid-attempt; a container left running would never clear.
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertThat(registry.getListenerContainers()).noneMatch(MessageListenerContainer::isRunning));
    }
}
