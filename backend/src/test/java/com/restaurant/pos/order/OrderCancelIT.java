package com.restaurant.pos.order;

import com.restaurant.pos.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Phase 5b: POST /api/orders/{id}/cancel. Setup and DB assertions go through SQL on purpose. */
class OrderCancelIT extends IntegrationTest {

    private static final String SERVER_ID = "a0000001-0000-0000-0000-000000000003"; // server1, V5 seed

    @Test
    void serverCancelsADraftOrder_withAReason_andItIsStored() throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "DRAFT", "10.00", null);

        cancel(orderId, "server1", "{\"reason\":\"Guest left\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM orders WHERE id = ?", String.class, orderId))
                .isEqualTo("Guest left");
        assertThat(jdbc.queryForObject("SELECT cancelled_by FROM orders WHERE id = ?", UUID.class, orderId))
                .hasToString(SERVER_ID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"reason\":\"\"}", "{\"reason\":\"   \"}"})
    void aMissingOrBlankReason_is400_andChangesNothing(String body) throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "DRAFT", "10.00", null);

        cancel(orderId, "server1", body).andExpect(status().isBadRequest());

        assertNotCancelled(orderId, "DRAFT");
    }

    @ParameterizedTest
    @ValueSource(strings = {"COMPLETED", "CANCELLED"})
    void aTerminalOrder_is409_andChangesNothing(String from) throws Exception {
        UUID orderId = insertOrder("TAKEOUT", from, "10.00", null);

        cancel(orderId, "manager1", "{\"reason\":\"Too late\"}").andExpect(status().isConflict());

        assertNotCancelled(orderId, from);
    }

    /** Before the kitchen has it anyone but the kitchen may cancel; after the fire only a manager or admin. */
    @ParameterizedTest
    @CsvSource({
            "server1, DRAFT, 200",
            "server1, SUBMITTED, 200",
            "server1, FIRED, 403",
            "server1, PREPARING, 403",
            "server1, READY, 403",
            "manager1, DRAFT, 200",
            "manager1, SUBMITTED, 200",
            "manager1, FIRED, 200",
            "manager1, PREPARING, 200",
            "manager1, READY, 200",
            "admin, DRAFT, 200",
            "admin, SUBMITTED, 200",
            "admin, FIRED, 200",
            "admin, PREPARING, 200",
            "admin, READY, 200"})
    void whoMayCancelWhichState(String username, String from, int expected) throws Exception {
        UUID orderId = insertOrder("TAKEOUT", from, "10.00", null);

        cancel(orderId, username, "{\"reason\":\"Changed mind\"}").andExpect(status().is(expected));

        if (expected == 200) {
            assertThat(orderStatus(orderId)).isEqualTo("CANCELLED");
        } else {
            assertNotCancelled(orderId, from);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "SUBMITTED", "FIRED", "PREPARING", "READY"})
    void kitchen_is403_inEveryState_andChangesNothing(String from) throws Exception {
        UUID orderId = insertOrder("TAKEOUT", from, "10.00", null);

        cancel(orderId, "kitchen1", "{\"reason\":\"Nope\"}").andExpect(status().isForbidden());

        assertNotCancelled(orderId, from);
    }

    @Test
    void withoutToken_is401_andChangesNothing() throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "DRAFT", "10.00", null);

        mvc.perform(post("/api/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Anonymous\"}"))
                .andExpect(status().isUnauthorized());

        assertNotCancelled(orderId, "DRAFT");
    }

    @Test
    void anOrderWithACapturedPayment_is409_andChangesNothing() throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "FIRED", "21.00", null);
        jdbc.update("""
                INSERT INTO payments (order_id, payment_method, amount, status, captured_at)
                VALUES (?, 'CASH', 5.00, 'CAPTURED', now())""", orderId);

        String body = cancel(orderId, "manager1", "{\"reason\":\"Walkout\"}").andExpect(status().isConflict())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("refund or void the payment first");
        assertNotCancelled(orderId, "FIRED");
    }

    @Test
    void aMissingOrder_is404() throws Exception {
        cancel(UUID.randomUUID(), "manager1", "{\"reason\":\"Oops\"}").andExpect(status().isNotFound());
    }

    private void assertNotCancelled(UUID orderId, String status) {
        assertThat(orderStatus(orderId)).isEqualTo(status);
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM orders WHERE id = ?", String.class, orderId)).isNull();
        assertThat(jdbc.queryForObject("SELECT cancelled_by FROM orders WHERE id = ?", UUID.class, orderId)).isNull();
    }

    private ResultActions cancel(UUID orderId, String username, String body) throws Exception {
        return mvc.perform(authed(post("/api/orders/" + orderId + "/cancel"), username)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
