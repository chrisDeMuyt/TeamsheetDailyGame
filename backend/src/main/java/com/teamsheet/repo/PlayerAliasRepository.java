package com.teamsheet.repo;

import com.teamsheet.domain.PlayerAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerAliasRepository extends JpaRepository<PlayerAlias, PlayerAlias.Key> {

    /** Exact alias hits, scoped to one match's pool. */
    @Query(value = """
            SELECT DISTINCT pa.player_id
            FROM player_alias pa
            JOIN match_player mp ON mp.player_id = pa.player_id
            WHERE mp.match_id = :matchId AND pa.alias_norm = :q
            """, nativeQuery = true)
    List<Long> exactMatches(String matchId, String q);

    /** Best trigram score per pool player (best alias wins), at or above the threshold, best first. */
    @Query(value = """
            SELECT pa.player_id AS playerId,
                   MAX(GREATEST(similarity(pa.alias_norm, :q), word_similarity(:q, pa.alias_norm)))::float8 AS score
            FROM player_alias pa
            JOIN match_player mp ON mp.player_id = pa.player_id
            WHERE mp.match_id = :matchId
            GROUP BY pa.player_id
            HAVING MAX(GREATEST(similarity(pa.alias_norm, :q), word_similarity(:q, pa.alias_norm))) >= :threshold
            ORDER BY score DESC
            """, nativeQuery = true)
    List<FuzzyCandidate> fuzzyMatches(String matchId, String q, double threshold);

    interface FuzzyCandidate {
        Long getPlayerId();
        Double getScore();
    }
}
