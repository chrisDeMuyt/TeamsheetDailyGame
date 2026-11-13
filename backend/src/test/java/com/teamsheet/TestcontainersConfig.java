package com.teamsheet;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgres() {
        return new PostgreSQLContainer<>("postgres:16");
    }

    @Bean
    @Primary
    MutableClock mutableClock() {
        return new MutableClock(Instant.parse("2026-11-11T12:00:00Z"));
    }

    /** Test clock: set the date of the puzzle you want, advance it to trigger the 30s timeout. */
    public static class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        public MutableClock(Instant start) { this.now = new AtomicReference<>(start); }

        public void set(Instant instant) { now.set(instant); }
        public void advanceSeconds(long s) { now.updateAndGet(i -> i.plusSeconds(s)); }

        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    }
}
