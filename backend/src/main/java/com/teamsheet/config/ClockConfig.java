package com.teamsheet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Source of "now" for the guess timer and the daily puzzle (tests replace it). The day boundary zone is teamsheet.game.puzzle-zone. */
@Configuration
public class ClockConfig {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
