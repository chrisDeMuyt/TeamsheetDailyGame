package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.teamsheet.TestcontainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Applies a generated promotion migration to real Postgres (V1-V3 already applied by Flyway) inside a transaction
 * that is always rolled back, so the shared test DB is left untouched.
 */
@SpringBootTest
@Import(TestcontainersConfig.class)
class PromotionIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txm;

    @Test
    void allFiftyDraftsApplyCleanlyAsOneMigration() throws Exception {
        JsonNode seed = SeedSqlGeneratorTest.seedWithDrafts();   // all 50 drafts
        var migration = SeedSqlGenerator.plan(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS), false).orElseThrow();
        assertThat(migration.matchIds()).hasSize(50);

        new TransactionTemplate(txm).executeWithoutResult(status -> {
            status.setRollbackOnly();
            jdbc.execute(migration.sql());

            assertThat(jdbc.queryForObject("SELECT count(*) FROM puzzle", Integer.class)).isEqualTo(60);
            assertThat(jdbc.queryForObject("SELECT max(puzzle_date)::text FROM puzzle", String.class)).isEqualTo("2027-01-09");

            // every match has exactly 11 starters with pitch coords, and every sub replaces someone in the same match
            List<Map<String, Object>> bad = jdbc.queryForList("""
                    SELECT m.id, count(*) FILTER (WHERE mp.role = 'starter' AND mp.slot_x IS NOT NULL) AS starters
                    FROM match m JOIN match_player mp ON mp.match_id = m.id
                    GROUP BY m.id HAVING count(*) FILTER (WHERE mp.role = 'starter' AND mp.slot_x IS NOT NULL) <> 11""");
            assertThat(bad).isEmpty();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM match_player s
                    WHERE s.role = 'sub_used' AND NOT EXISTS (
                      SELECT 1 FROM match_player r WHERE r.match_id = s.match_id AND r.player_id = s.replaced_id)""",
                    Integer.class)).isZero();

            // players shared across eras are one row: Xabi Alonso (V2) is reused, Kahn (new) appears twice
            assertThat(jdbc.queryForObject("SELECT count(*) FROM player WHERE display_name = 'Xabi Alonso'", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("""
                    SELECT count(*) FROM match_player mp JOIN player p ON p.id = mp.player_id
                    WHERE p.display_name = 'Oliver Kahn'""", Integer.class)).isEqualTo(2);

            // the player id sequence was advanced, so the app can still insert players later
            Long maxId = jdbc.queryForObject("SELECT max(id) FROM player", Long.class);
            assertThat(jdbc.queryForObject("SELECT last_value FROM player_id_seq", Long.class)).isEqualTo(maxId);

            // aliases work with the real matcher query shape: accent-free surname finds the new player in his pool
            assertThat(jdbc.queryForList("""
                    SELECT DISTINCT pa.player_id FROM player_alias pa JOIN match_player mp ON mp.player_id = pa.player_id
                    WHERE mp.match_id = '2016-euro-r16-iceland' AND pa.alias_norm = 'gudmundsson'""", Long.class)).hasSize(1);
            assertThat(jdbc.queryForList("""
                    SELECT DISTINCT pa.player_id FROM player_alias pa JOIN match_player mp ON mp.player_id = pa.player_id
                    WHERE mp.match_id = '2002-wc-r16-south-korea' AND pa.alias_norm = 'park'""", Long.class)).hasSize(1);
        });

        assertThat(jdbc.queryForObject("SELECT count(*) FROM puzzle", Integer.class)).as("rolled back").isEqualTo(10);
    }
}
