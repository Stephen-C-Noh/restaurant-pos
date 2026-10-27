package com.restaurant.pos;

import com.restaurant.pos.auth.JwtService;
import com.restaurant.pos.user.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Base for *IT classes: full app on a real Postgres + embedded Kafka.
 * One container for the whole run (started once, reaped by Testcontainers) so the
 * cached Spring context never points at a stopped database.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@AutoConfigureMockMvc
@EmbeddedKafka
public abstract class IntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;

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

    protected MockHttpServletRequestBuilder authed(MockHttpServletRequestBuilder req, String username) {
        String token = jwtService.generateToken(userRepository.findByUsername(username).orElseThrow());
        return req.header("Authorization", "Bearer " + token);
    }

    protected String orderStatus(UUID orderId) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, orderId);
    }
}
