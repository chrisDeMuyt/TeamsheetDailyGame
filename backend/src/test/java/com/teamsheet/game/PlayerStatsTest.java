package com.teamsheet.game;

import com.teamsheet.domain.Play;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlayerStatsTest {

    /** {number, score} pairs, oldest first. */
    private static List<Play> plays(int[]... rows) {
        return Arrays.stream(rows).map(r -> {
            Play p = BeanUtils.instantiateClass(Play.class);
            ReflectionTestUtils.setField(p, "puzzleNumber", r[0]);
            ReflectionTestUtils.setField(p, "totalScore", r[1]);
            return p;
        }).toList();
    }

    @Test
    void streaksCountConsecutivePuzzlesAndTheCurrentOneEndsAtTheLatest() {
        var s = PlayService.stats(plays(new int[]{1, 40}, new int[]{2, 205}, new int[]{3, 10}, new int[]{5, 60}, new int[]{6, 0}));
        assertThat(s.played()).isEqualTo(5);
        assertThat(s.maxStreak()).isEqualTo(3);
        assertThat(s.currentStreak()).isEqualTo(2);
        assertThat(s.bestScore()).isEqualTo(205);
    }

    @Test
    void noGamesIsAllZero() {
        var s = PlayService.stats(List.of());
        assertThat(List.of(s.played(), s.currentStreak(), s.maxStreak(), s.bestScore())).containsOnly(0);
    }
}
