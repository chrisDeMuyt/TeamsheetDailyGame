package com.teamsheet.web;

import com.teamsheet.api.Dtos.*;
import com.teamsheet.game.PlayService;
import com.teamsheet.game.PlayerSearchService;
import com.teamsheet.game.PuzzleService;
import com.teamsheet.game.TooManyRequestsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GameController {

    private final PuzzleService puzzles;
    private final PlayService plays;
    private final PlayerSearchService search;
    private final RateLimiter limiter;
    private final RateLimitProperties limits;

    public GameController(PuzzleService puzzles, PlayService plays, PlayerSearchService search,
                          RateLimiter limiter, RateLimitProperties limits) {
        this.puzzles = puzzles;
        this.plays = plays;
        this.search = search;
        this.limiter = limiter;
        this.limits = limits;
    }

    @GetMapping("/puzzle/today")
    public PuzzleView today() {
        return puzzles.todayView();
    }

    /** Has this device started today's puzzle? Never creates a play, so the timer can't start before Start. */
    @GetMapping("/plays/today")
    public TodayStatus todayStatus(@RequestParam UUID anonId) {
        return plays.todayStatus(anonId);
    }

    /**
     * Pressing Start: creates (or resumes) the play; the first guess's clock starts now. One play per device per day
     * is enforced by the database; since device ids are made by the browser, new plays are also capped per IP.
     */
    @PostMapping("/plays")
    public PlayView createPlay(@Valid @RequestBody CreatePlayRequest req, HttpServletRequest http) {
        Integer puzzle = limits.enabled() ? plays.unstartedPuzzleToday(req.anonId()) : null;
        if (puzzle != null) {
            long wait = limiter.hit("newplay#" + puzzle, RateLimitFilter.clientIp(http), limits.newPlaysPerDay(),
                    Duration.ofDays(2));   // keyed by puzzle number, so it effectively resets each day
            if (wait > 0) throw new TooManyRequestsException("Too many new games from this network today.", wait);
        }
        return plays.create(req.anonId());
    }

    @PostMapping("/plays/{id}/guesses")
    public GuessResult guess(@PathVariable UUID id, @Valid @RequestBody GuessRequest req) {
        if (req.playerId() != null) return plays.guessPlayer(id, req.playerId());
        return plays.guess(id, req.text() == null ? "" : req.text());
    }

    @GetMapping("/plays/{id}/reveal")
    public RevealView reveal(@PathVariable UUID id) {
        return plays.reveal(id);
    }

    /** One "Did you mean" suggestion (200), or 204 when nothing matches / fewer than 2 characters. */
    @GetMapping("/players/suggest")
    public ResponseEntity<PlayerOption> suggest(@RequestParam(defaultValue = "") String q) {
        return search.suggest(q).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }
}
