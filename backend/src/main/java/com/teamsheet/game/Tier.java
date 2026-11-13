package com.teamsheet.game;

/** Rarity tiers by pick % (CLAUDE.md), most common first. */
public enum Tier {
    TAP_IN("Tap-in", 40, 10),
    SOLID_SHOUT("Solid Shout", 15, 30),
    PROPER_FAN("Proper Fan", 5, 60),
    HIDDEN_GEM("Hidden Gem", 1, 85),
    CULT_HERO("Cult Hero", 0, 100);

    public final String label;
    public final double minPct;
    public final int points;

    Tier(String label, double minPct, int points) {
        this.label = label;
        this.minPct = minPct;
        this.points = points;
    }

    /** 40+ = 10, 15-40 = 30, 5-15 = 60, 1-5 = 85, under 1 = 100. Lower bounds are inclusive. */
    public static Tier forPct(double pct) {
        for (Tier t : values()) {
            if (pct >= t.minPct) return t;
        }
        return CULT_HERO;
    }
}
