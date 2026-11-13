package com.teamsheet.game;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TierTest {

    @ParameterizedTest
    @CsvSource({
            "100, 10", "40, 10", "39.9, 30", "15, 30", "14.9, 60", "5, 60",
            "4.9, 85", "1, 85", "0.9, 100", "0, 100"
    })
    void pointsByPickPct(double pct, int points) {
        assertThat(Tier.forPct(pct).points).isEqualTo(points);
    }

    @ParameterizedTest
    @CsvSource({"50, Tap-in", "20, Solid Shout", "8, Proper Fan", "2, Hidden Gem", "0.5, Cult Hero"})
    void labelsByPickPct(double pct, String label) {
        assertThat(Tier.forPct(pct).label).isEqualTo(label);
    }
}
