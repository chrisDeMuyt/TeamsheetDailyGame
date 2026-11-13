package com.teamsheet.game;

import java.util.List;

public sealed interface MatchOutcome {
    record Hit(long playerId) implements MatchOutcome {}
    record Ambiguous(List<Long> candidates) implements MatchOutcome {}
    record Miss() implements MatchOutcome {}
}
