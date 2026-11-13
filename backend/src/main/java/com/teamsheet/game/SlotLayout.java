package com.teamsheet.game;

import com.teamsheet.domain.MatchPlayer;

import java.util.Comparator;
import java.util.List;

/**
 * Deterministic 1-based slot numbering for a pool: starters by pitch y then x (GK first), then subs by
 * minute on. Before reveal, clients only see slot numbers, never player ids.
 */
public final class SlotLayout {

    private SlotLayout() {}

    private static final Comparator<MatchPlayer> ORDER = Comparator
            .comparing(MatchPlayer::isSub)
            .thenComparing(mp -> mp.getSlotY() == null ? 0 : mp.getSlotY())
            .thenComparing(mp -> mp.getSlotX() == null ? 0 : mp.getSlotX())
            .thenComparing(mp -> mp.getMinuteOn() == null ? 0 : mp.getMinuteOn())
            .thenComparing(MatchPlayer::getPlayerId);

    public static List<MatchPlayer> ordered(List<MatchPlayer> pool) {
        return pool.stream().sorted(ORDER).toList();
    }

    public static int slotOf(List<MatchPlayer> ordered, long playerId) {
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getPlayerId() == playerId) return i + 1;
        }
        throw new IllegalArgumentException("player " + playerId + " not in pool");
    }
}
