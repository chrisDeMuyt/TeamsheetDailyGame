package com.teamsheet.game;

import com.teamsheet.api.Dtos.PlayerOption;
import com.teamsheet.repo.PlayerRepository;
import com.teamsheet.repo.PlayerRepository.OptionRow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * "Did you mean": the one best player for what the user has typed, from the whole database (never scoped to
 * today's match, so it can't hint at the answer). If the text is exactly a player's full name, that player wins
 * and is flagged exact.
 */
@Service
public class PlayerSearchService {

    public static final int MIN_CHARS = 2;
    private static final int CANDIDATES = 10;

    private final PlayerRepository players;

    public PlayerSearchService(PlayerRepository players) {
        this.players = players;
    }

    @Transactional(readOnly = true)
    public Optional<PlayerOption> suggest(String raw) {
        String q = NameNormalizer.normalize(raw);
        if (q.length() < MIN_CHARS) return Optional.empty();
        List<OptionRow> ranked = players.search(q, CANDIDATES);
        if (ranked.isEmpty()) return Optional.empty();
        OptionRow best = ranked.stream()
                .filter(r -> NameNormalizer.normalize(r.getName()).equals(q))
                .findFirst()
                .orElse(ranked.get(0));
        boolean exact = NameNormalizer.normalize(best.getName()).equals(q);
        return Optional.of(new PlayerOption(best.getId(), best.getName(), exact));
    }
}
