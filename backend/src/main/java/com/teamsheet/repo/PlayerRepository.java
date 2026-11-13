package com.teamsheet.repo;

import com.teamsheet.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    /**
     * Autocomplete over EVERY player (never scoped to a match, so suggestions give nothing away).
     * q is already normalized (lower-case, accent-free, [a-z0-9 ] only). Rank: exact alias > alias starts with q >
     * a word in an alias starts with q > trigram typo match; ties go to the better-known player (fame = Wikipedia
     * sitelinks, then seed_rarity for puzzle players Wikidata didn't match).
     */
    @Query(value = """
            SELECT p.id AS id, p.display_name AS name
            FROM player p
            JOIN player_alias pa ON pa.player_id = p.id
            WHERE pa.alias_norm LIKE :q || '%'
               OR pa.alias_norm LIKE '% ' || :q || '%'
               OR similarity(pa.alias_norm, :q) > 0.35
            GROUP BY p.id, p.display_name, p.fame, p.seed_rarity
            ORDER BY MAX(CASE WHEN pa.alias_norm = :q THEN 4
                              WHEN pa.alias_norm LIKE :q || '%' THEN 3
                              WHEN pa.alias_norm LIKE '% ' || :q || '%' THEN 2
                              ELSE similarity(pa.alias_norm, :q) END) DESC,
                     COALESCE(p.fame, 0) DESC,
                     COALESCE(p.seed_rarity, 0) DESC,
                     p.display_name
            LIMIT :limit
            """, nativeQuery = true)
    List<OptionRow> search(String q, int limit);

    interface OptionRow {
        Long getId();
        String getName();
    }
}
