package com.restaurant.pos;

import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

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
}
