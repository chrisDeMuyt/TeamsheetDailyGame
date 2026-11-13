package com.teamsheet.game;

import com.teamsheet.domain.MatchPlayer;
import com.teamsheet.domain.PickPct;
import com.teamsheet.repo.PickPctRepository;
import com.teamsheet.repo.PlayRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/** Cold start: seed_rarity until N finished plays for the puzzle, then real pick %. */
@Service
public class ScoringService {

    public enum PctSource { seed, live }

    public record Rarity(double pct, PctSource source) {}

    public record Score(Tier tier, int points) {}

    @FunctionalInterface
    public interface RarityLookup {
        Rarity of(MatchPlayer mp);
    }

    private final PlayRepository plays;
    private final PickPctRepository pickPct;
    private final GameProperties props;

    public ScoringService(PlayRepository plays, PickPctRepository pickPct, GameProperties props) {
        this.plays = plays;
        this.pickPct = pickPct;
        this.props = props;
    }

    /** Pick % for each player of a puzzle, from whichever source currently applies. */
    public RarityLookup rarityFor(int puzzleNumber) {
        boolean live = plays.countByPuzzleNumberAndFinishedAtIsNotNull(puzzleNumber) >= props.liveRarityMinPlays();
        if (!live) {
            return mp -> {
                BigDecimal seed = mp.getPlayer().getSeedRarity();
                return new Rarity(seed == null ? 0.0 : seed.doubleValue(), PctSource.seed);
            };
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
}
