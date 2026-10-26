package com.restaurant.pos.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.pos.IntegrationTest;
import com.restaurant.pos.events.DomainEvent;
import com.restaurant.pos.events.ItemStatusChangedEvent;
import com.restaurant.pos.events.OrderFiredEvent;
import com.restaurant.pos.menu.KitchenSection;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 1 of docs/improvement-plan.md — characterization of fire, item status and the push, as the code
 * behaves today. Setup and DB assertions go through SQL on purpose. The WebSocket push is captured in-process
 * with an interceptor on the brokerChannel; Kafka is read from the embedded broker with a fresh consumer group.
 */
class FireOrderIT extends IntegrationTest {

    // V4 seed: GRILL, COLD, FRYER
    private static final String BURGER = "bb4d597e-0524-4ac7-ace6-749d8a36d38f";
    private static final String SALAD = "88a06d9d-0201-4bd7-a42c-09bb3f42128f";
    private static final String FRIES = "040d648d-5120-48a2-94d9-ee6faa4bcf91";

    private static final String ORDER_FIRED_TOPIC = "order-fired-events";
    private static final String ITEM_STATUS_TOPIC = "item-status-events";

    @MockitoSpyBean private KafkaTemplate<String, DomainEvent> kafkaTemplate;
    @Autowired private EmbeddedKafkaBroker kafka;
    @Autowired private ObjectMapper json;
    @Autowired @Qualifier("brokerChannel") private AbstractSubscribableChannel brokerChannel;

    private final List<Message<?>> sent = new CopyOnWriteArrayList<>();
    private final ChannelInterceptor capture = new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            sent.add(message);
            return message;
        }
    };

    @BeforeEach
    void startCapture() {
        brokerChannel.addInterceptor(capture);
    }

    @AfterEach
    void stopCapture() {
        brokerChannel.removeInterceptor(capture);
    }

    // --- fire: happy path ---------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "SUBMITTED"})
    void fire_marksOrderAndAllItemsFired(String from) throws Exception {
        UUID orderId = orderWithItems(from, BURGER, SALAD, FRIES);

        fire(orderId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("FIRED"))
                .andExpect(jsonPath("$.firedAt").isNotEmpty());

        assertThat(orderStatus(orderId)).isEqualTo("FIRED");
        assertThat(firedAt(orderId)).isNotNull();
        assertThat(itemStatuses(orderId)).containsExactly("FIRED", "FIRED", "FIRED");
    }

    @Test
    void fire_publishesOneOrderFiredEvent_keyedByOrderId_groupedBySection() throws Exception {
        UUID orderId = orderWithItems("DRAFT", BURGER, SALAD, FRIES);

        fire(orderId).andExpect(status().isOk());

        List<ConsumerRecord<String, String>> records = records(ORDER_FIRED_TOPIC, orderId, Duration.ofSeconds(10));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).key()).isEqualTo(orderId.toString());
        OrderFiredEvent event = json.readValue(records.get(0).value(), OrderFiredEvent.class);
        assertThat(event.getEventType()).isEqualTo("OrderFired");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getOrderNumber()).isEqualTo(orderNumber(orderId));
        assertThat(event.getFiredAt()).isNotNull();
        assertThat(event.getItemsBySection()).containsOnlyKeys(
                KitchenSection.GRILL, KitchenSection.COLD, KitchenSection.FRYER);
        assertThat(event.getItemsBySection().get(KitchenSection.GRILL))
                .singleElement().satisfies(i -> assertThat(i.getMenuItemName()).isEqualTo("Classic Burger"));
        assertThat(event.getItemsBySection().get(KitchenSection.COLD))
                .singleElement().satisfies(i -> assertThat(i.getMenuItemName()).isEqualTo("Caesar Salad"));
        assertThat(event.getItemsBySection().get(KitchenSection.FRYER))
                .singleElement().satisfies(i -> assertThat(i.getMenuItemName()).isEqualTo("French Fries"));
    }

    @Test
    void fire_sendsOneOrderResponseOnTopicOrders() throws Exception {
        UUID orderId = orderWithItems("DRAFT", BURGER, SALAD);

        fire(orderId).andExpect(status().isOk());

        List<JsonNode> messages = topicOrders(orderId);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).get("status").asText()).isEqualTo("FIRED");
        assertThat(messages.get(0).get("firedAt").isNull()).isFalse();
        assertThat(messages.get(0).get("items")).hasSize(2);
    }

    // --- fire: rejected -----------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"FIRED", "COMPLETED", "CANCELLED"})
    void fire_onNonFireableOrder_is409_andChangesNothing(String from) throws Exception {
        UUID orderId = orderWithItems(from, BURGER);

        fire(orderId).andExpect(status().isConflict());

        assertNothingHappened(orderId, from, "PENDING");
    }

    @Test
    void fire_onEmptyOrder_is409_andChangesNothing() throws Exception {
        UUID orderId = insertOrder("TAKEOUT", "DRAFT", "0.00", null);

        fire(orderId).andExpect(status().isConflict());

        assertNothingHappened(orderId, "DRAFT");
    }

    // --- item status --------------------------------------------------------

    @Test
    void itemFiredToPreparing_is200_publishesEvent_andPushesOrder() throws Exception {
        UUID orderId = orderWithItems("FIRED", BURGER);
        UUID itemId = itemIds(orderId).get(0);
        setItemStatus(itemId, "FIRED");

        // permitAll in SecurityConfig: the KDS calls this without a token
        itemStatus(orderId, itemId, "PREPARING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.status").value("PREPARING"));

        assertThat(itemStatuses(orderId)).containsExactly("PREPARING");
        List<ConsumerRecord<String, String>> records = records(ITEM_STATUS_TOPIC, orderId, Duration.ofSeconds(10));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).key()).isEqualTo(orderId.toString());
        ItemStatusChangedEvent event = json.readValue(records.get(0).value(), ItemStatusChangedEvent.class);
        assertThat(event.getEventType()).isEqualTo("ItemStatusChanged");
        assertThat(event.getOrderId()).isEqualTo(orderId);
        assertThat(event.getItemId()).isEqualTo(itemId);
        assertThat(event.getOldStatus()).isEqualTo(OrderItemStatus.FIRED);
        assertThat(event.getNewStatus()).isEqualTo(OrderItemStatus.PREPARING);
        // The push is the whole order (not the item) on the same /topic/orders destination.
        List<JsonNode> messages = topicOrders(orderId);
        assertThat(messages).hasSize(1);
        assertThat(messages.get(0).get("status").asText()).isEqualTo("FIRED");
        assertThat(messages.get(0).get("items")).singleElement()
                .satisfies(i -> assertThat(i.get("status").asText()).isEqualTo("PREPARING"));
    }

    @Test
    void itemFiredToServed_is409_andNothingIsPublishedOrSent() throws Exception {
        UUID orderId = orderWithItems("FIRED", BURGER);
        UUID itemId = itemIds(orderId).get(0);
        setItemStatus(itemId, "FIRED");

        itemStatus(orderId, itemId, "SERVED").andExpect(status().isConflict());

        assertThat(itemStatuses(orderId)).containsExactly("FIRED");
        assertThat(records(ITEM_STATUS_TOPIC, orderId, Duration.ofSeconds(2))).isEmpty();
        assertThat(topicOrders(orderId)).isEmpty();
    }

    // --- KDS initial load ---------------------------------------------------

    @Test
    void activeOrders_withoutToken_returnsOnlyFiredOrders() throws Exception {
        List<String> notFired = List.of("DRAFT", "SUBMITTED", "PREPARING", "READY", "COMPLETED", "CANCELLED");
        UUID fired = insertOrder("TAKEOUT", "FIRED", "1.00", null);
        List<UUID> others = notFired.stream().map(s -> insertOrder("TAKEOUT", s, "1.00", null)).toList();

        String body = mvc.perform(get("/api/orders/active"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> ids = new ArrayList<>();
        List<String> statuses = new ArrayList<>();
        json.readTree(body).forEach(o -> {
            ids.add(o.get("id").asText());
            statuses.add(o.get("status").asText());
        });
        assertThat(ids).contains(fired.toString());
        assertThat(ids).doesNotContainAnyElementsOf(others.stream().map(UUID::toString).toList());
        // other IT classes leave orders behind, so assert on every returned row rather than on the list size
        assertThat(statuses).containsOnly("FIRED");
    }

    // --- Phase 2 flips this -------------------------------------------------

    /**
     * Finding 2: a Kafka failure rolls the fire back today (EventPublisher rethrows, the transaction rolls back,
     * GlobalExceptionHandler answers 500). The spy delegates to the real template in every other test and is
     * reset after each test.
     */
    // flips in Phase 2: fire must return 200, order FIRED, WS message sent
    @Test
    void fire_whenKafkaSendThrows_is500_andOrderStaysDraft() throws Exception {
        doThrow(new IllegalStateException("kafka down"))
                .when(kafkaTemplate).send(anyString(), anyString(), any(DomainEvent.class));
        UUID orderId = orderWithItems("DRAFT", BURGER);

        fire(orderId).andExpect(status().isInternalServerError());

        assertNothingHappened(orderId, "DRAFT", "PENDING");
    }

    // --- helpers ------------------------------------------------------------

    /** An order with one PENDING item per menu item id, inserted via SQL. */
    private UUID orderWithItems(String status, String... menuItemIds) {
        UUID orderId = insertOrder("TAKEOUT", status, "0.00", null);
        for (String menuItemId : menuItemIds) {
            jdbc.update("""
                    INSERT INTO order_items (order_id, menu_item_id, quantity, unit_price, subtotal)
                    SELECT ?, id, 1, base_price, base_price FROM menu_items WHERE id = ?""",
                    orderId, UUID.fromString(menuItemId));
        }
        return orderId;
    }

    private ResultActions fire(UUID orderId) throws Exception {
        return mvc.perform(authed(post("/api/orders/" + orderId + "/fire"), "server1"));
    }

    private ResultActions itemStatus(UUID orderId, UUID itemId, String newStatus) throws Exception {
        return mvc.perform(patch("/api/orders/" + orderId + "/items/" + itemId + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"" + newStatus + "\"}"));
    }

    private void setItemStatus(UUID itemId, String status) {
        jdbc.update("UPDATE order_items SET status = ? WHERE id = ?", status, itemId);
    }

    /** Rejected or rolled-back fire: DB as before, no event, no WS message. */
    private void assertNothingHappened(UUID orderId, String status, String... itemStatuses) {
        assertThat(orderStatus(orderId)).isEqualTo(status);
        assertThat(firedAt(orderId)).isNull();
        assertThat(itemStatuses(orderId)).containsExactly(itemStatuses);
        assertThat(records(ORDER_FIRED_TOPIC, orderId, Duration.ofSeconds(2))).isEmpty();
        assertThat(topicOrders(orderId)).isEmpty();
    }

    private Object firedAt(UUID orderId) {
        return jdbc.queryForObject("SELECT fired_at FROM orders WHERE id = ?", Object.class, orderId);
    }

    private String orderNumber(UUID orderId) {
        return jdbc.queryForObject("SELECT order_number FROM orders WHERE id = ?", String.class, orderId);
    }

    private List<String> itemStatuses(UUID orderId) {
        return jdbc.queryForList("SELECT status FROM order_items WHERE order_id = ? ORDER BY created_at, id",
                String.class, orderId);
    }

    private List<UUID> itemIds(UUID orderId) {
        return jdbc.queryForList("SELECT id FROM order_items WHERE order_id = ? ORDER BY created_at, id",
                UUID.class, orderId);
    }

    /** Messages for this order on /topic/orders. The template sends synchronously, so no waiting is needed. */
    private List<JsonNode> topicOrders(UUID orderId) {
        List<JsonNode> out = new ArrayList<>();
        for (Message<?> m : sent) {
            if (!"/topic/orders".equals(SimpMessageHeaderAccessor.getDestination(m.getHeaders()))) {
                continue;
            }
            try {
                JsonNode order = json.readTree(new String((byte[]) m.getPayload(), StandardCharsets.UTF_8));
                if (orderId.toString().equals(order.path("id").asText())) {
                    out.add(order);
                }
            } catch (Exception e) {
                throw new AssertionError("Unreadable /topic/orders payload", e);
            }
        }
        return out;
    }

    /**
     * Records on {@code topic} keyed by {@code orderId}, read from the beginning with a fresh consumer group
     * (the embedded broker is shared with other IT classes). Returns early once a record arrived and a poll
     * came back empty; an expected-empty read waits the full {@code wait}.
     */
    private List<ConsumerRecord<String, String>> records(String topic, UUID orderId, Duration wait) {
        Map<String, Object> props = KafkaTestUtils.consumerProps(UUID.randomUUID().toString(), "false", kafka);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        List<ConsumerRecord<String, String>> out = new ArrayList<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer()).createConsumer()) {
            List<PartitionInfo> info = consumer.partitionsFor(topic);
            if (info == null || info.isEmpty()) {
                return out; // topic never created: nothing was ever published
            }
            List<TopicPartition> partitions = info.stream()
                    .map(p -> new TopicPartition(topic, p.partition())).toList();
            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);
            long end = System.nanoTime() + wait.toNanos();
            while (System.nanoTime() < end) {
                var batch = consumer.poll(Duration.ofMillis(250));
                batch.forEach(r -> {
                    if (orderId.toString().equals(r.key())) {
                        out.add(r);
                    }
                });
                if (!out.isEmpty() && batch.isEmpty()) {
                    break;
                }
            }
        }
        return out;
    }
}
