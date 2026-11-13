package com.teamsheet.web;

import com.teamsheet.api.Dtos.*;
import com.teamsheet.game.PlayService;
import com.teamsheet.game.PuzzleService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api")
public class GameController {

    private final PuzzleService puzzles;
    private final PlayService plays;

    public GameController(PuzzleService puzzles, PlayService plays) {
        this.puzzles = puzzles;
        this.plays = plays;
    }

    @GetMapping("/puzzle/today")
    public PuzzleView today() {
        return puzzles.todayView();
    }

    @PostMapping("/plays")
    public PlayView createPlay(@Valid @RequestBody CreatePlayRequest req) {
        return plays.create(req.anonId());
    }

    @PostMapping("/plays/{id}/guesses")
    public GuessResult guess(@PathVariable UUID id, @Valid @RequestBody GuessRequest req) {
        return plays.guess(id, req.text());
    }

    @GetMapping("/plays/{id}/reveal")
    public RevealView reveal(@PathVariable UUID id) {
        return plays.reveal(id);
    }
}
