package com.teamsheet.domain;

import jakarta.persistence.*;

import java.io.Serializable;

@Entity
@Table(name = "pick_stat")
@IdClass(PickStat.Key.class)
public class PickStat {

    @Id
    @Column(name = "puzzle_number")
    private Integer puzzleNumber;

    @Id
    @Column(name = "player_id")
    private Long playerId;

    @Column(nullable = false)
    private Integer picks;

    protected PickStat() {}

    public Integer getPuzzleNumber() { return puzzleNumber; }
    public Long getPlayerId() { return playerId; }
    public Integer getPicks() { return picks; }

    public static class Key implements Serializable {
        private Integer puzzleNumber;
        private Long playerId;

        public Key() {}

        public Key(Integer puzzleNumber, Long playerId) {
            this.puzzleNumber = puzzleNumber;
            this.playerId = playerId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && java.util.Objects.equals(puzzleNumber, k.puzzleNumber) && java.util.Objects.equals(playerId, k.playerId);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(puzzleNumber, playerId); }
    }
}
