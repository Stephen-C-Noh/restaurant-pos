package com.restaurant.pos.payment;

import com.restaurant.pos.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Issue #8 — pay an order in full with cash. Cases come from
 * docs/payment-implementation-plan.md (§5, §7, §8.2, P1 catalog #1, #4-#7, #9, #10, #14),
 * not from the implementation. Setup and assertions go through SQL on purpose.
 */
class CashPaymentIT extends IntegrationTest {

    // --- happy path ---------------------------------------------------------

    @Test
    void fullCashPayment_isCaptured_withChange_andOrderCompletes() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "0.00", "25.00"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.orderId").value(orderId.toString()))
                .andExpect(jsonPath("$.method").value("CASH"))
                .andExpect(jsonPath("$.status").value("CAPTURED"));

        Map<String, Object> p = onlyPayment(orderId);
        assertThat(p.get("status")).isEqualTo("CAPTURED");
        assertThat(p.get("payment_method")).isEqualTo("CASH");
        assertMoney(p.get("amount"), "21.00");
        assertMoney(p.get("tendered_amount"), "25.00");
        assertMoney(p.get("change_given"), "4.00");
        assertThat(p.get("captured_at")).isNotNull();
        assertThat(p.get("created_by")).hasToString(SERVER_ID);

        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT completed_at FROM orders WHERE id = ?", Object.class, orderId))
                .isNotNull();
    }

    @Test
    void cashTip_isStoredOnPayment_andChangeIsTenderedMinusAmountMinusTip() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "3.00", "30.00")).andExpect(status().isCreated());

        Map<String, Object> p = onlyPayment(orderId);
        assertMoney(p.get("amount"), "21.00");
        assertMoney(p.get("tip_amount"), "3.00");
        assertMoney(p.get("change_given"), "6.00");
        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
    }

    @Test
    void exactTender_givesZeroChange() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "2.00", "23.00")).andExpect(status().isCreated());

        assertMoney(onlyPayment(orderId).get("change_given"), "0.00");
    }

    @ParameterizedTest
    @ValueSource(strings = {"FIRED", "PREPARING", "READY"})
    void payableStatuses_acceptPayment(String orderStatus) throws Exception {
        UUID orderId = order(orderStatus, "10.50");

        pay(orderId, key(), cash("10.50", "0.00", "20.00")).andExpect(status().isCreated());

        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
    }

    @Test
    void dineInTable_goesOccupiedToCleaning_whenOrderCompletes() throws Exception {
        UUID tableId = table("OCCUPIED");
        UUID orderId = dineInOrder("FIRED", "30.00", tableId);

        pay(orderId, key(), cash("30.00", "0.00", "30.00")).andExpect(status().isCreated());

        assertThat(orderStatus(orderId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM tables WHERE id = ?", String.class, tableId))
                .isEqualTo("CLEANING");
    }

    // --- validation (400) ---------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-5.00"})
    void nonPositiveAmount_is400(String amount) throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash(amount, "0.00", "25.00")).andExpect(status().isBadRequest());

        assertNoPayments(orderId);
    }

    @Test
    void missingAmount_is400() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), """
                {"method":"CASH","tip":0.00,"tendered":25.00}""").andExpect(status().isBadRequest());

        assertNoPayments(orderId);
    }

    @Test
    void cashWithoutTendered_is400() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), """
                {"method":"CASH","amount":21.00,"tip":0.00}""").andExpect(status().isBadRequest());

        assertNoPayments(orderId);
    }

    @Test
    void missingIdempotencyKey_is400() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        mvc.perform(authed(post(url(orderId)), "server1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cash("21.00", "0.00", "25.00")))
                .andExpect(status().isBadRequest());

        assertNoPayments(orderId);
    }

    @Test
    void nonUuidIdempotencyKey_is400() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, "not-a-uuid", cash("21.00", "0.00", "25.00")).andExpect(status().isBadRequest());

        assertNoPayments(orderId);
    }

    // --- business rules (409) -----------------------------------------------

    @Test
    void tenderedLessThanAmountPlusTip_is409() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "4.00", "24.99")).andExpect(status().isConflict());

        assertNoPayments(orderId);
        assertThat(orderStatus(orderId)).isEqualTo("FIRED");
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "SUBMITTED", "COMPLETED", "CANCELLED"})
    void nonPayableOrder_is409(String orderStatus) throws Exception {
        UUID orderId = order(orderStatus, "21.00");

        pay(orderId, key(), cash("21.00", "0.00", "25.00")).andExpect(status().isConflict());

        assertNoPayments(orderId);
        assertThat(orderStatus(orderId)).isEqualTo(orderStatus);
    }

    @Test
    void unknownOrder_is404() throws Exception {
        pay(UUID.randomUUID(), key(), cash("21.00", "0.00", "25.00")).andExpect(status().isNotFound());
    }

    // --- idempotency --------------------------------------------------------

    @Test
    void sameKeySameBody_returnsSamePayment_andStoresOneRow() throws Exception {
        UUID orderId = order("FIRED", "21.00");
        String key = key();
        String body = cash("21.00", "0.00", "25.00");

        String firstId = idOf(pay(orderId, key, body).andExpect(status().isCreated()));
        // The order is COMPLETED now: a replay must still return the stored result, not 409.
        String replayId = idOf(pay(orderId, key, body).andExpect(status().isCreated()));

        assertThat(replayId).isEqualTo(firstId);
        assertThat(paymentCount(orderId)).isEqualTo(1);
    }

    @Test
    void sameKeyDifferentBody_is409_andStoresOneRow() throws Exception {
        UUID orderId = order("FIRED", "21.00");
        String key = key();

        pay(orderId, key, cash("21.00", "0.00", "25.00")).andExpect(status().isCreated());
        pay(orderId, key, cash("21.00", "0.00", "30.00")).andExpect(status().isConflict());

        assertThat(paymentCount(orderId)).isEqualTo(1);
        assertMoney(onlyPayment(orderId).get("tendered_amount"), "25.00");
    }

    // --- security -----------------------------------------------------------

    @Test
    void unauthenticated_is401() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        mvc.perform(post(url(orderId))
                        .header("Idempotency-Key", key())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cash("21.00", "0.00", "25.00")))
                .andExpect(status().isUnauthorized());

        assertNoPayments(orderId);
    }

    @Test
    void kitchenRole_is403() throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "0.00", "25.00"), "kitchen1").andExpect(status().isForbidden());

        assertNoPayments(orderId);
    }

    @ParameterizedTest
    @ValueSource(strings = {"server1", "manager1", "admin"})
    void serverManagerAdmin_mayTakePayment(String username) throws Exception {
        UUID orderId = order("FIRED", "21.00");

        pay(orderId, key(), cash("21.00", "0.00", "25.00"), username).andExpect(status().isCreated());
    }

    // --- helpers ------------------------------------------------------------

    private static final String SERVER_ID = "a0000001-0000-0000-0000-000000000003"; // server1, V5 seed

    private UUID order(String status, String total) {
        return insertOrder("TAKEOUT", status, total, null);
    }

    private UUID dineInOrder(String status, String total, UUID tableId) {
        return insertOrder("DINE_IN", status, total, tableId);
    }

    private UUID table(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tables (id, table_number, capacity, status) VALUES (?, ?, 4, ?)",
                id, "X" + id.toString().substring(0, 8), status);
        return id;
    }

    private static String cash(String amount, String tip, String tendered) {
        return """
                {"method":"CASH","amount":%s,"tip":%s,"tendered":%s}""".formatted(amount, tip, tendered);
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private static String url(UUID orderId) {
        return "/api/orders/" + orderId + "/payments";
    }

    private ResultActions pay(UUID orderId, String key, String body) throws Exception {
        return pay(orderId, key, body, "server1");
    }

    private ResultActions pay(UUID orderId, String key, String body, String username) throws Exception {
        return mvc.perform(authed(post(url(orderId)), username)
                .header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String idOf(ResultActions result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.id");
    }

    private int paymentCount(UUID orderId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM payments WHERE order_id = ?", Integer.class, orderId);
    }

    private Map<String, Object> onlyPayment(UUID orderId) {
        assertThat(paymentCount(orderId)).isEqualTo(1);
        return jdbc.queryForMap("SELECT * FROM payments WHERE order_id = ?", orderId);
    }

    private void assertNoPayments(UUID orderId) {
        assertThat(paymentCount(orderId)).isZero();
    }

    private static void assertMoney(Object actual, String expected) {
        assertThat((BigDecimal) actual).isEqualByComparingTo(expected);
    }
}
