package com.teamsheet.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Public API shapes. Nothing in PuzzleView or SlotView may carry a player name or id. */
public final class Dtos {

    private Dtos() {}

    public record SlotView(int slot, String pos, String role, Integer x, Integer y) {}

    public record PuzzleView(int number, String team, String competition, LocalDate date,
                             String formation, List<SlotView> slots) {}

    public record CreatePlayRequest(@NotNull UUID anonId) {}

    /** guesses = this play's consumed guesses so a refreshed client can resume (names only for its own hits). */
    public record PlayView(UUID playId, int puzzleNumber, int guessesUsed, int guessesLeft,
                           boolean finished, int totalScore, Instant deadline, List<PickView> guesses) {}

    public record GuessRequest(@NotNull @Size(max = 100) String text) {}

    public enum GuessResultType { hit, miss, duplicate, ambiguous, timeout }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record GuessResult(GuessResultType result,
                              Integer guessIndex,      // index this guess consumed; null if it didn't consume one
                              int guessesLeft,
                              Integer slot,            // hit/duplicate only
                              String name,             // hit/duplicate only
                              String shortName,        // hit/duplicate only (pitch label)
                              Boolean sub,             // hit/duplicate only
                              String tier,             // hit only
                              int points,
                              int totalScore,
                              boolean finished,
                              Instant deadline,        // when the current open guess times out; null if finished
                              String message) {}

    public record PoolEntry(int slot, String name, String shortName, String pos, String role,
                            Integer x, Integer y, Integer minuteOn, Integer minuteOff, Integer replacedSlot,
                            double pickPct, boolean picked) {}

    /** result: hit | miss | timeout. sub = the hit player came off the bench. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PickView(int guessIndex, String text, String result, Integer slot, String name, String shortName,
                           Boolean sub, String tier, int points) {}

    public record RevealView(int puzzleNumber, String team, String opponent, String result, String competition,
                             LocalDate date, String formation, String pctSource,
                             List<PoolEntry> pool, List<PickView> picks, int totalScore) {}
}
