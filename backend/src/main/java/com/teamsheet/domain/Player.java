package com.teamsheet.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "short_name", nullable = false)
    private String shortName;

    @Column(name = "seed_rarity")
    private BigDecimal seedRarity;

    protected Player() {}

    public Long getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getShortName() { return shortName; }
    public BigDecimal getSeedRarity() { return seedRarity; }
}
