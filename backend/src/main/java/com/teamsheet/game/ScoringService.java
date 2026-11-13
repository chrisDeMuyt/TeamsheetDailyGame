package com.teamsheet.game;

import com.teamsheet.domain.MatchPlayer;
import com.teamsheet.domain.PickPct;
import com.teamsheet.repo.PickPctRepository;
import com.teamsheet.repo.PlayRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Pick % per player of a puzzle.
 * <ul>
 *   <li><b>Cold start</b> (fewer than N finished plays): the lineup is ranked from best- to least-known and split into
 *       tiers by {@link #SHARES} (≈20/30/25/15/10%), at least one per tier, so every puzzle has easy names and a
 *       Cult Hero. Each player gets his tier's representative pick % ({@link #TIER_PCT}).</li>
 *   <li><b>Live</b>: real pick % = picks / finished plays.</li>
 * </ul>
 */
@Service
public class ScoringService {

    public enum PctSource { seed, live }

    public record Rarity(double pct, PctSource source) {}

    public record Score(Tier tier, int points) {}

    @FunctionalInterface
    public interface RarityLookup {
        Rarity of(MatchPlayer mp);
    }

    /** Target share of each tier in a lineup, in Tier order (Tap-in .. Cult Hero). */
    static final double[] SHARES = {0.20, 0.30, 0.25, 0.15, 0.10};
    /** Pick % given to each tier during cold start (inside the Tier thresholds). */
    static final double[] TIER_PCT = {50.0, 25.0, 10.0, 3.0, 0.5};

    /** Least-known last: the player's own estimate, then Wikipedia fame, starters before subs, then id. */
    static final Comparator<MatchPlayer> BEST_KNOWN_FIRST = Comparator
            .comparingDouble((MatchPlayer mp) -> seed(mp)).reversed()
            .thenComparing(mp -> mp.getPlayer().getFame() == null ? 0 : mp.getPlayer().getFame(), Comparator.reverseOrder())
            .thenComparing(MatchPlayer::isSub)
            .thenComparing(MatchPlayer::getPlayerId);

    private final PlayRepository plays;
    private final PickPctRepository pickPct;
    private final PuzzleService puzzles;
    private final GameProperties props;

    public ScoringService(PlayRepository plays, PickPctRepository pickPct, PuzzleService puzzles, GameProperties props) {
        this.plays = plays;
        this.pickPct = pickPct;
        this.puzzles = puzzles;
        this.props = props;
    }

    public RarityLookup rarityFor(int puzzleNumber) {
        boolean live = plays.countByPuzzleNumberAndFinishedAtIsNotNull(puzzleNumber) >= props.liveRarityMinPlays();
        if (!live) {
            Map<Long, Double> tiered = coldStartPct(puzzles.orderedPool(puzzles.byNumber(puzzleNumber)));
            return mp -> new Rarity(tiered.getOrDefault(mp.getPlayerId(), seed(mp)), PctSource.seed);
        }
        Map<Long, BigDecimal> livePct = pickPct.findByPuzzleNumber(puzzleNumber).stream()
                .collect(Collectors.toMap(PickPct::getPlayerId, PickPct::getPct));
        return mp -> {
            BigDecimal pct = livePct.get(mp.getPlayerId());
            return new Rarity(pct == null ? 0.0 : pct.doubleValue(), PctSource.live);
        };
    }

    public Score score(Rarity rarity, MatchPlayer mp) {
        Tier tier = Tier.forPct(rarity.pct());
        return new Score(tier, tier.points + (mp.isSub() ? props.subBonus() : 0));
    }

    /** playerId -> cold-start pick %, by rank within this lineup. */
    static Map<Long, Double> coldStartPct(List<MatchPlayer> pool) {
        List<MatchPlayer> ranked = pool.stream().sorted(BEST_KNOWN_FIRST).toList();
        int[] counts = tierCounts(ranked.size());
        Map<Long, Double> out = new HashMap<>();
        int i = 0;
        for (int t = 0; t < counts.length; t++)
            for (int k = 0; k < counts[t]; k++) out.put(ranked.get(i++).getPlayerId(), TIER_PCT[t]);
        return out;
    }

    /**
     * How many players per tier for a lineup of n: floor of each share, at least 1 each, then the leftovers to the
     * largest remainders. 13 -> 3/4/3/2/1, 14 -> 3/4/4/2/1, 17 -> 3/5/4/3/2.
     */
    static int[] tierCounts(int n) {
        int tiers = SHARES.length;
        if (n < tiers) throw new IllegalArgumentException("lineup too small for " + tiers + " tiers: " + n);
        int[] c = new int[tiers];
        double[] rem = new double[tiers];
        int used = 0;
        for (int t = 0; t < tiers; t++) {
            double exact = n * SHARES[t];
            c[t] = Math.max(1, (int) Math.floor(exact));
            rem[t] = exact - Math.floor(exact);
            used += c[t];
        }
        Integer[] order = new Integer[tiers];
        for (int t = 0; t < tiers; t++) order[t] = t;
        Arrays.sort(order, (a, b) -> Double.compare(rem[b], rem[a]) != 0 ? Double.compare(rem[b], rem[a]) : a - b);
        for (int k = 0; used < n; k = (k + 1) % tiers) { c[order[k]]++; used++; }
        for (int k = tiers - 1; used > n; k = (k + tiers - 1) % tiers) if (c[k] > 1) { c[k]--; used--; }
        return c;
    }

    private static double seed(MatchPlayer mp) {
        BigDecimal s = mp.getPlayer().getSeedRarity();
        return s == null ? 0.0 : s.doubleValue();
    }
}
