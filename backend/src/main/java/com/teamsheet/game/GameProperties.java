package com.teamsheet.game;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.ZoneId;

@ConfigurationProperties("teamsheet.game")
public record GameProperties(
        @DefaultValue("America/Chicago") ZoneId puzzleZone,   // daily reset at local midnight (DST-aware)
        @DefaultValue("30") int guessSeconds,
        @DefaultValue("2") int graceSeconds,
        @DefaultValue("0.45") double fuzzyThreshold,
        @DefaultValue("0.10") double ambiguityMargin,
        @DefaultValue("3") int minFuzzyLength,
        @DefaultValue("200") int liveRarityMinPlays,
        @DefaultValue("15") int subBonus
) {}
