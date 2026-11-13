package com.teamsheet.game;

import com.teamsheet.repo.PlayerAliasRepository;
import com.teamsheet.repo.PlayerAliasRepository.FuzzyCandidate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resolves free text to a player in ONE match's pool.
 * 1) exact normalized alias; 2) trigram similarity. Two or more plausible players = ambiguous.
 */
@Component
public class GuessMatcher {

    private final PlayerAliasRepository aliases;
    private final GameProperties props;

    public GuessMatcher(PlayerAliasRepository aliases, GameProperties props) {
        this.aliases = aliases;
        this.props = props;
    }

    /** The single pool player whose alias is exactly this name, if exactly one. */
    public java.util.Optional<Long> exactPoolPlayer(String matchId, String name) {
        List<Long> exact = aliases.exactMatches(matchId, NameNormalizer.normalize(name));
        return exact.size() == 1 ? java.util.Optional.of(exact.get(0)) : java.util.Optional.empty();
    }

    public MatchOutcome match(String matchId, String rawText) {
        String q = NameNormalizer.normalize(rawText);
        if (q.isEmpty()) return new MatchOutcome.Miss();

        List<Long> exact = aliases.exactMatches(matchId, q);
        if (exact.size() == 1) return new MatchOutcome.Hit(exact.get(0));
        if (exact.size() > 1) return new MatchOutcome.Ambiguous(exact);

        if (q.length() < props.minFuzzyLength()) return new MatchOutcome.Miss();

        List<FuzzyCandidate> fuzzy = aliases.fuzzyMatches(matchId, q, props.fuzzyThreshold());
        if (fuzzy.isEmpty()) return new MatchOutcome.Miss();

        double top = fuzzy.get(0).getScore();
        List<Long> close = fuzzy.stream()
                .filter(c -> top - c.getScore() < props.ambiguityMargin())
                .map(FuzzyCandidate::getPlayerId)
                .toList();
        return close.size() == 1 ? new MatchOutcome.Hit(close.get(0)) : new MatchOutcome.Ambiguous(close);
    }
}
