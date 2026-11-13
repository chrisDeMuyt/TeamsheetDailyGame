package com.teamsheet.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "puzzle")
public class Puzzle {

    @Id
    private Integer number;

    @Column(name = "puzzle_date", nullable = false, unique = true)
    private LocalDate puzzleDate;

    @Column(name = "match_id", nullable = false)
    private String matchId;

    protected Puzzle() {}

    public Integer getNumber() { return number; }
    public LocalDate getPuzzleDate() { return puzzleDate; }
    public String getMatchId() { return matchId; }
}
