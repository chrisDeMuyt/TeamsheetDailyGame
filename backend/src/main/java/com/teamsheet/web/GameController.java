package com.teamsheet.web;

import com.teamsheet.api.Dtos.*;
import com.teamsheet.game.PlayService;
import com.teamsheet.game.PlayerSearchService;
import com.teamsheet.game.PuzzleService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GameController {

    private final PuzzleService puzzles;
    private final PlayService plays;
    private final PlayerSearchService search;

    public GameController(PuzzleService puzzles, PlayService plays, PlayerSearchService search) {
        this.puzzles = puzzles;
        this.plays = plays;
        this.search = search;
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

    /** Pressing Start: creates (or resumes) the play; the first guess's 30s starts now. */
    @PostMapping("/plays")
    public PlayView createPlay(@Valid @RequestBody CreatePlayRequest req) {
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
