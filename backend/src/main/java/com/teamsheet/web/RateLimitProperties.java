package com.teamsheet.web;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Per-IP limits that keep a scripted flood from running up the bill or skewing pick %. */
@ConfigurationProperties("teamsheet.rate-limit")
public record RateLimitProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("300") int apiPerMinute,        // every /api call
        @DefaultValue("30") int guessesPerMinute,     // POST /api/plays/{id}/guesses (a game needs 3, plus duplicates)
        @DefaultValue("60") int suggestPerMinute,     // GET /api/players/suggest (the heaviest query)
        @DefaultValue("20") int newPlaysPerDay        // new plays (devices) per IP per puzzle; resuming doesn't count
) {}
