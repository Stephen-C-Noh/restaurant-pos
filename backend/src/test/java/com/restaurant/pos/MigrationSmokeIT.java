package com.restaurant.pos;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Boots the full app against a clean Postgres and proves every Flyway migration applies.
 */
class MigrationSmokeIT extends IntegrationTest {

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
