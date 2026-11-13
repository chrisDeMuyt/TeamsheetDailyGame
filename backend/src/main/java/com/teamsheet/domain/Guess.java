package com.teamsheet.domain;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "guess")
@IdClass(Guess.Key.class)
public class Guess {

    @Id
    @Column(name = "play_id")
    private UUID playId;

    @Id
    @Column(name = "guess_index")
    private Integer guessIndex;

    @Column(name = "raw_text", nullable = false)
    private String rawText;

    @Column(name = "player_id")
    private Long playerId;          // null = miss / timeout

    @Column(nullable = false)
    private String result;          // hit | miss | timeout (V3)

    private String tier;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Guess() {}

    public Guess(UUID playId, int guessIndex, String rawText, String result, Long playerId, String tier, int points,
                 Instant createdAt) {
        this.playId = playId;
        this.guessIndex = guessIndex;
        this.rawText = rawText;
        this.result = result;
        this.playerId = playerId;
        this.tier = tier;
        this.points = points;
        this.createdAt = createdAt;
    }

    public UUID getPlayId() { return playId; }
    public Integer getGuessIndex() { return guessIndex; }
    public String getRawText() { return rawText; }
    public Long getPlayerId() { return playerId; }
    public String getResult() { return result; }
    public String getTier() { return tier; }
    public Integer getPoints() { return points; }
    public Instant getCreatedAt() { return createdAt; }

    public static class Key implements Serializable {
        private java.util.UUID playId;
        private Integer guessIndex;

        public Key() {}

        public Key(java.util.UUID playId, Integer guessIndex) {
            this.playId = playId;
            this.guessIndex = guessIndex;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key k && java.util.Objects.equals(playId, k.playId) && java.util.Objects.equals(guessIndex, k.guessIndex);
        }

        @Override
        public int hashCode() { return java.util.Objects.hash(playId, guessIndex); }
    }
}
