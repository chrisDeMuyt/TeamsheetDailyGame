package com.teamsheet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;

/** Read-only mapping of the {@code pick_pct} view: picks / finished plays per puzzle. */
@Entity
@Immutable
@Table(name = "pick_pct")
@IdClass(PickStat.Key.class)
public class PickPct {

    @Id
    @Column(name = "puzzle_number")
    private Integer puzzleNumber;

    @Id
    @Column(name = "player_id")
    private Long playerId;

    private Integer picks;

    private BigDecimal pct;

    protected PickPct() {}

    public Integer getPuzzleNumber() { return puzzleNumber; }
    public Long getPlayerId() { return playerId; }
    public Integer getPicks() { return picks; }
    public BigDecimal getPct() { return pct; }
}
