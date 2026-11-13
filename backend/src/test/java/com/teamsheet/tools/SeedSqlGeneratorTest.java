package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
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
        assertThat(ex.matchIds()).hasSize(10);
        assertThat(ex.playerIds()).hasSize(131);
        assertThat(ex.maxPuzzle()).isEqualTo(10);
        assertThat(ex.lastPuzzleDate()).isEqualTo(LocalDate.of(2026, 11, 20));

        JsonNode seed = read(SeedSqlGenerator.DEFAULT_JSON);
        assertThat(SeedSqlGenerator.drift(seed, ex)).as("seed edited after migrating").isEmpty();
        assertThat(SeedSqlGenerator.plan(seed, ex)).as("seed has unmigrated matches: run the generator").isEmpty();
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
        // Chelsea 2008 reuses V2 players (Čech, Lampard, Drogba...); Germany 2002 reuses Klose and Hamann.
        JsonNode seed = seedWithDrafts("2008-ucl-final-chelsea", "2002-wc-final-germany");
        var m = SeedSqlGenerator.plan(seed, ex, false).orElseThrow();

        assertThat(m.version()).isEqualTo(ex.maxVersion() + 1);
        assertThat(m.fileName()).isEqualTo("V" + m.version() + "__puzzles_11_to_12.sql");
        assertThat(m.matchIds()).containsExactly("2002-wc-final-germany", "2008-ucl-final-chelsea");
        String sql = m.sql();

        long lampard = ex.playerIds().get("Frank Lampard");
        assertThat(playerRows(sql)).as("reused players are not re-inserted")
                .doesNotContain("Frank Lampard", "Miroslav Klose", "Dietmar Hamann", "Petr Čech")
                .contains("John Terry", "Oliver Kahn");
        assertThat(sql).contains("('2008-ucl-final-chelsea', " + lampard + ", 'starter'");
        assertThat(sql).contains("(132, 'Oliver Kahn', 'Kahn', 60.00)");     // new ids continue after V2's 131
        assertThat(sql).contains("(11, DATE '2026-11-21', '2002-wc-final-germany')",
                                 "(12, DATE '2026-11-22', '2008-ucl-final-chelsea')");
        assertThat(sql).contains("ON CONFLICT DO NOTHING");
    }

    @Test
    void newPlayersWithoutRarityAreRejected() throws Exception {
        ObjectNode seed = (ObjectNode) seedWithDrafts("2002-wc-final-germany");
        JsonNode added = seed.get("matches").get(10);
        for (JsonNode p : added.get("squad")) ((ObjectNode) p).remove("rarity");
        assertThatThrownBy(() -> SeedSqlGenerator.plan(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS), false))
                .hasMessageContaining("No rarity for new player Oliver Kahn");
    }

    @Test
    void unverifiedOrUnsourcedMatchesCannotShip() throws Exception {
        JsonNode seed = seedWithDrafts("2002-wc-final-germany");
        assertThatThrownBy(() -> SeedSqlGenerator.plan(seed, SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS)))
                .hasMessageContaining("2002-wc-final-germany: needs 2+ independent sources")
                .hasMessageContaining("2002-wc-final-germany: verification still 'unverified'");
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
