package com.restaurant.pos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.pos.auth.JwtService;
import com.restaurant.pos.order.OrderChanged;
import com.restaurant.pos.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Base for *IT classes: full app on a real Postgres + embedded Kafka.
 * One container for the whole run (started once, reaped by Testcontainers) so the
 * cached Spring context never points at a stopped database.
 * <p>
 * Every IT class must share ONE Spring context: each context runs its own outbox relay against the same
 * Postgres, and a relay from another context would publish this context's rows to its own embedded broker.
 * So the spies and test beans live here, not in the subclasses.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka
@Import(IntegrationTest.FailBeforeCommit.class)
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    // Delegates to the real template unless a test stubs it; reset after each test.
    @MockitoSpyBean protected KafkaTemplate<String, String> outboxKafkaTemplate;

    /** Arm to make the next order transaction fail at commit time, after everything it wrote. */
    @TestConfiguration(proxyBeanMethods = false)
    public static class FailBeforeCommit {
        public static volatile boolean armed;

        @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
        public void failIfArmed(OrderChanged changed) {
            if (armed) {
                throw new IllegalStateException("test: fail before commit");
            }
        }
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired @Qualifier("brokerChannel") protected AbstractSubscribableChannel brokerChannel;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

    private final List<Message<?>> sent = new CopyOnWriteArrayList<>();
    private final ChannelInterceptor captureSent = new ChannelInterceptor() {
        @Override
        public Message<?> preSend(Message<?> message, MessageChannel channel) {
            sent.add(message);
            return message;
        }
    };

    @BeforeEach
    void startSentCapture() {
        brokerChannel.addInterceptor(captureSent);
    }

    @AfterEach
    void stopSentCapture() {
        brokerChannel.removeInterceptor(captureSent);
    }

    // Setup goes through SQL on purpose: tests must not depend on the code under test.
    protected UUID insertOrder(String type, String status, String total, UUID tableId) {
        UUID id = UUID.randomUUID();
        BigDecimal t = new BigDecimal(total);
        jdbc.update("""
                INSERT INTO orders (id, order_number, order_type, status, table_id, subtotal, tax_amount, total)
                VALUES (?, ?, ?, ?, ?, ?, 0.00, ?)""",
                id, "IT-" + id.toString().substring(0, 8), type, status, tableId, t, t);
        return id;
    }

    protected UUID table(String status) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO tables (id, table_number, capacity, status) VALUES (?, ?, 4, ?)",
                id, "X" + id.toString().substring(0, 8), status);
        return id;
    }

    /** Outbox rows of one order in creation order. */
    protected List<Map<String, Object>> outboxRows(UUID orderId) {
        return jdbc.queryForList("""
                SELECT aggregate_type, event_type, topic, status, attempts, last_error, processed_at, payload::text AS payload
                FROM outbox_events WHERE aggregate_id = ? ORDER BY created_at, id""", orderId.toString());
    }

    /**
     * Messages for this order on /topic/orders so far. The push comes from the Kafka consumer thread, so
     * "a push happened" assertions must wait for it (await); "no push" assertions need a sentinel or an earlier wait.
     */
    protected List<JsonNode> topicOrders(UUID orderId) {
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

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String username) {
        String token = jwtService.generateToken(userRepository.findByUsername(username).orElseThrow());
        return req.header("Authorization", "Bearer " + token);
    }

    protected String orderStatus(UUID orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }
}
