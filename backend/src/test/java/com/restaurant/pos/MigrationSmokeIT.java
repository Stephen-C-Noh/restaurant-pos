package com.restaurant.pos;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full app against a clean Postgres and proves every Flyway migration applies.
 */
@SpringBootTest(properties = "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}")
@EmbeddedKafka
@Testcontainers
class MigrationSmokeIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private Flyway flyway;

    @Test
    void allMigrationsApplyOnCleanDatabase() {
        MigrationInfo[] applied = flyway.info().applied();

        assertThat(flyway.info().pending()).isEmpty();
        assertThat(applied).isNotEmpty();
        assertThat(applied).allSatisfy(m -> assertThat(m.getState().isFailed()).isFalse());
    }
}
