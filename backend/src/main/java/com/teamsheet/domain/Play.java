package com.teamsheet.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Rows are created by {@code PlayRepository.insertIfAbsent} (idempotent upsert), then updated via JPA. */
@Entity
@Table(name = "play")
public class Play {

    @Id
    private UUID id;

    @Column(name = "puzzle_number", nullable = false)
    private Integer puzzleNumber;

    @Column(name = "anon_id", nullable = false)
    private UUID anonId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "guess_opened_at", nullable = false)
    private Instant guessOpenedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_score", nullable = false)
    private Integer totalScore;

    protected Play() {}

    public UUID getId() { return id; }
    public Integer getPuzzleNumber() { return puzzleNumber; }
    public UUID getAnonId() { return anonId; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getGuessOpenedAt() { return guessOpenedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public Integer getTotalScore() { return totalScore; }
    public boolean isFinished() { return finishedAt != null; }

    public void openNextGuess(Instant now) { this.guessOpenedAt = now; }
    public void finish(Instant now) { this.finishedAt = now; }
    public void addPoints(int points) { this.totalScore += points; }
}
