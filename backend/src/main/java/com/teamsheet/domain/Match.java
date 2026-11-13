package com.teamsheet.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "match")
public class Match {

    @Id
    private String id;

    @Column(name = "match_date", nullable = false)
    private LocalDate matchDate;

    @Column(nullable = false)
    private String competition;

    @Column(nullable = false)
    private String team;

    @Column(nullable = false)
    private String opponent;

    @Column(nullable = false)
    private String result;

    @Column(nullable = false)
    private String formation;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "xi_status", nullable = false)
    private Verification xiStatus;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "subs_status", nullable = false)
    private Verification subsStatus;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(nullable = false)
    private String[] sources;

    protected Match() {}

    public String getId() { return id; }
    public LocalDate getMatchDate() { return matchDate; }
    public String getCompetition() { return competition; }
    public String getTeam() { return team; }
    public String getOpponent() { return opponent; }
    public String getResult() { return result; }
    public String getFormation() { return formation; }
    public Verification getXiStatus() { return xiStatus; }
    public Verification getSubsStatus() { return subsStatus; }
    public String[] getSources() { return sources; }
}
