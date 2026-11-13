package com.teamsheet.game;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TierCountsTest {

    @ParameterizedTest
    @CsvSource({"13, 3 4 3 2 1", "14, 3 4 4 2 1", "15, 3 5 4 2 1", "16, 3 5 4 2 2", "17, 3 5 4 3 2"})
    void splitsLineupsRoughly20_30_25_15_10(int n, String expected) {
        assertThat(ScoringService.tierCounts(n))
                .containsExactly(Arrays.stream(expected.split(" ")).mapToInt(Integer::parseInt).toArray());
    }

    @Test
    void alwaysAtLeastOnePerTierAndSumsToTheLineup() {
        for (int n = 5; n <= 30; n++) {
            int[] c = ScoringService.tierCounts(n);
            assertThat(Arrays.stream(c).sum()).as("n=" + n).isEqualTo(n);
            assertThat(Arrays.stream(c).min().orElseThrow()).as("n=" + n + " min per tier").isGreaterThanOrEqualTo(1);
        }
    }

    @Test
    void tierPctFallsInsideEachTier() {
        Tier[] tiers = Tier.values();
        for (int t = 0; t < tiers.length; t++)
            assertThat(Tier.forPct(ScoringService.TIER_PCT[t])).isEqualTo(tiers[t]);
    }
}
