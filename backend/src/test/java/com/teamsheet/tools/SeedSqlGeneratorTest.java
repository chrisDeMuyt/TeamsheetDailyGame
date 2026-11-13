package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SeedSqlGeneratorTest {

    static final ObjectMapper JSON = new ObjectMapper();
    static final Path DRAFTS = Path.of("../teamsheet-drafts.json");

    static JsonNode read(Path p) throws Exception {
        return JSON.readTree(Files.readString(p, StandardCharsets.UTF_8));
    }

    /** The live seed plus the given drafts, as if they had been verified and moved into teamsheet-seed.json. */
    static JsonNode seedWithDrafts(String... draftIds) throws Exception {
        ObjectNode seed = (ObjectNode) read(SeedSqlGenerator.DEFAULT_JSON);
        ArrayNode matches = (ArrayNode) seed.get("matches");
        List<String> wanted = List.of(draftIds);
        for (JsonNode d : read(DRAFTS).get("matches"))
            if (wanted.isEmpty() || wanted.contains(d.get("id").asText())) matches.add(d.deepCopy());
        return seed;
    }

    @Test
    void committedMigrationsCoverTheSeedExactly() throws Exception {
        var ex = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS);
        JsonNode seed = read(SeedSqlGenerator.DEFAULT_JSON);
        int matches = seed.get("matches").size();
        java.util.Set<String> players = new java.util.HashSet<>();
        seed.get("matches").forEach(m -> m.get("squad").forEach(p -> players.add(p.get("name").asText())));

        int shelved = read(Path.of("../teamsheet-shelved.json")).get("matches").size();
        assertThat(ex.matchIds()).as("DB matches = scheduled (seed) + shelved").hasSize(matches + shelved);
        assertThat(ex.playerIds().keySet()).containsAll(players);
        assertThat(ex.unrated()).as("every puzzle player has a seed rarity").doesNotContainAnyElementsOf(players);
        assertThat(ex.maxPuzzle()).isEqualTo(matches);
        assertThat(ex.lastPuzzleDate()).isEqualTo(SeedSqlGenerator.FIRST_PUZZLE_DATE.plusDays(matches - 1));
        assertThat(SeedSqlGenerator.drift(seed, ex)).as("seed edited after migrating").isEmpty();
        assertThat(SeedSqlGenerator.plan(seed, ex)).as("seed has unmigrated matches: run the generator").isEmpty();
    }

    @Test
    void liveScheduleIsExactlyTheSeedOrder() throws Exception {
        var ex = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS);
        JsonNode matches = read(SeedSqlGenerator.DEFAULT_JSON).get("matches");
        for (int i = 0; i < matches.size(); i++)
            assertThat(ex.puzzles().get(i + 1)).as("puzzle #" + (i + 1)).isEqualTo(matches.get(i).get("id").asText());
        assertThat(ex.puzzles().get(3)).as("today after the 2026-11-13 reschedule").isEqualTo("2022-wc-final-argentina");
    }

    @Test
    void shelvedMatchesAreInTheDbButNotScheduledAndComeBackWithJustAPuzzleRow() throws Exception {
        var ex = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS);
        JsonNode shelved = read(Path.of("../teamsheet-shelved.json")).get("matches");
        assertThat(shelved).hasSize(7);
        for (JsonNode m : shelved) {
            assertThat(ex.matchIds()).contains(m.get("id").asText());
            assertThat(ex.puzzles().values()).doesNotContain(m.get("id").asText());
        }
        ObjectNode seed = (ObjectNode) read(SeedSqlGenerator.DEFAULT_JSON);
        ((ArrayNode) seed.get("matches")).add(shelved.get(0).deepCopy());          // bring Greece back
        var m = SeedSqlGenerator.plan(seed, ex).orElseThrow();
        assertThat(m.sql()).doesNotContain("INSERT INTO match ", "INSERT INTO match_player", "INSERT INTO player (")
                .contains("(" + (ex.maxPuzzle() + 1) + ", DATE '" + ex.lastPuzzleDate().plusDays(1) + "', '"
                        + shelved.get(0).get("id").asText() + "')");
    }

    @Test
    void editingAShippedMatchIsDetectedAsDrift() throws Exception {
        ObjectNode seed = (ObjectNode) read(SeedSqlGenerator.DEFAULT_JSON);
        ((ObjectNode) seed.get("matches").get(0)).put("result", "3-3 aet, won 3-2 on pens (edited)");
        var drift = SeedSqlGenerator.drift(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS));
        assertThat(drift).singleElement().asString().contains("2005-ucl-final-liverpool");
    }

    @Test
    void promotingDraftsWritesAnAppendOnlyMigration() throws Exception {
        var ex = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS);
        // South Korea 2002 reuses Park Ji-sung; Italy 2021 reuses Jorginho and Donnarumma (all already live).
        JsonNode seed = seedWithDrafts("2002-wc-r16-south-korea", "2021-euro-final-italy");
        var m = SeedSqlGenerator.plan(seed, ex, false).orElseThrow();

        int p1 = ex.maxPuzzle() + 1, p2 = ex.maxPuzzle() + 2;
        assertThat(m.version()).isEqualTo(ex.maxVersion() + 1);
        assertThat(m.fileName()).isEqualTo("V" + m.version() + "__puzzles_" + p1 + "_to_" + p2 + ".sql");
        assertThat(m.matchIds()).containsExactly("2002-wc-r16-south-korea", "2021-euro-final-italy");
        String sql = m.sql();

        assertThat(playerRows(sql)).as("puzzle players and extras are never re-inserted")
                .doesNotContain("Park Ji-sung", "Jorginho", "Gianluigi Donnarumma", "Leonardo Bonucci");
        assertThat(sql).contains("('2021-euro-final-italy', " + ex.playerIds().get("Jorginho") + ", 'starter'");
        // Bonucci already exists as an unrated "Did you mean" extra: same row, now given a seed rarity.
        assertThat(ex.unrated()).contains("Leonardo Bonucci");
        assertThat(sql).containsPattern("UPDATE player SET seed_rarity = [\\d.]+ WHERE id = " + ex.playerIds().get("Leonardo Bonucci") + ";");
        if (!playerRows(sql).isEmpty())
            assertThat(sql).contains("(" + (ex.maxPlayerId() + 1) + ", '");                 // new ids continue
        assertThat(sql).contains(
                "(" + p1 + ", DATE '" + ex.lastPuzzleDate().plusDays(1) + "', '2002-wc-r16-south-korea')",
                "(" + p2 + ", DATE '" + ex.lastPuzzleDate().plusDays(2) + "', '2021-euro-final-italy')");
        assertThat(sql).contains("ON CONFLICT DO NOTHING");
    }

    @Test
    void newPlayersWithoutRarityAreRejected() throws Exception {
        ObjectNode seed = (ObjectNode) seedWithDrafts("2002-wc-r16-south-korea");
        JsonNode added = seed.get("matches").get(seed.get("matches").size() - 1);
        for (JsonNode p : added.get("squad")) ((ObjectNode) p).remove("rarity");
        assertThatThrownBy(() -> SeedSqlGenerator.plan(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS), false))
                .hasMessageContaining("No rarity for")
                .hasMessageContaining("Lee Woon-jae");
    }

    @Test
    void unverifiedOrUnsourcedMatchesCannotShip() throws Exception {
        JsonNode seed = seedWithDrafts("2001-uefa-cup-final-alaves");
        assertThatThrownBy(() -> SeedSqlGenerator.plan(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS)))
                .hasMessageContaining("2001-uefa-cup-final-alaves: needs 2+ independent sources")
                .hasMessageContaining("2001-uefa-cup-final-alaves: verification still 'unverified'");
    }

    @Test
    void aliasesIncludeSurnameAndAccentFreeForms() throws Exception {
        assertThat(SeedSqlGenerator.aliases("Vladimír Šmicer")).contains("vladimir smicer", "smicer");
        assertThat(SeedSqlGenerator.aliases("Edwin van der Sar")).contains("van der sar", "vandersar", "sar");
        assertThat(SeedSqlGenerator.shortName("Daniele De Rossi")).isEqualTo("De Rossi");
        assertThat(SeedSqlGenerator.shortName("Robin Le Normand")).isEqualTo("Le Normand");
        assertThat(SeedSqlGenerator.shortName("Jawad El Yamiq")).isEqualTo("El Yamiq");
        assertThat(SeedSqlGenerator.shortName("Marc-André ter Stegen")).isEqualTo("ter Stegen");

        JsonNode park = JSON.readTree("""
                {"name":"Park Ji-sung","family_first":true,"short":"Park Ji-sung","aliases":["Ji-sung","jisung park"]}""");
        assertThat(SeedSqlGenerator.aliases("Park Ji-sung", park))
                .contains("park ji sung", "park", "ji sung", "jisung park")
                .doesNotContain("sung");
    }

    private static String playerRows(String sql) {
        int a = sql.indexOf("INSERT INTO player (");
        return a < 0 ? "" : sql.substring(a, sql.indexOf(";", a));
    }
}
