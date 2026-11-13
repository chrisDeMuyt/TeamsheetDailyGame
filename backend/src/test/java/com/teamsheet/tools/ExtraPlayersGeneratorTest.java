package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsheet.game.NameNormalizer;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class ExtraPlayersGeneratorTest {

    static Path committed() throws Exception {
        try (var files = Files.list(SeedSqlGenerator.MIGRATIONS)) {
            return files.filter(p -> p.getFileName().toString().endsWith(ExtraPlayersGenerator.SUFFIX)).findFirst().orElseThrow();
        }
    }

    static int version(Path p) {
        String n = p.getFileName().toString();
        return Integer.parseInt(n.substring(1, n.indexOf("__")));
    }

    static ExtraPlayersGenerator.Result regenerate() throws Exception {
        Path v = committed();
        var before = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS, version(v) - 1);
        JsonNode data = new ObjectMapper().readTree(Files.readString(ExtraPlayersGenerator.DATA, StandardCharsets.UTF_8));
        return ExtraPlayersGenerator.build(data, before);
    }

    @Test
    void committedMigrationMatchesTheDataFile() throws Exception {
        assertThat(Files.readString(committed(), StandardCharsets.UTF_8))
                .as("extra-players migration is stale; data or merge rules changed (write a new migration)")
                .isEqualToNormalizingNewlines(regenerate().sql());
    }

    @Test
    void puzzlePlayersAreMergedNotDuplicated() throws Exception {
        var r = regenerate();
        assertThat(r.mergedByName()).contains("Lionel Messi", "Cristiano Ronaldo", "Ronaldo", "Xavi", "Park Ji-sung");

        // No new row may share a normalized display name with an existing (puzzle) player.
        var before = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS, version(committed()) - 1);
        Set<String> existing = new HashSet<>();
        before.playerIds().keySet().forEach(n -> existing.add(NameNormalizer.normalize(n)));
        Matcher m = Pattern.compile("^  \\(\\d+, '((?:[^']|'')*)', '(?:[^']|'')*', NULL, \\d+\\)[,;]$", Pattern.MULTILINE)
                .matcher(r.sql());
        int rows = 0;
        Set<String> added = new HashSet<>();
        while (m.find()) {
            String n = NameNormalizer.normalize(m.group(1).replace("''", "'"));
            assertThat(existing).as("duplicate of a puzzle player: " + n).doesNotContain(n);
            assertThat(added.add(n)).as("duplicate extra: " + n).isTrue();
            rows++;
        }
        assertThat(rows).isEqualTo(r.added()).isGreaterThan(4000);
        assertThat(r.sql()).doesNotContain("'Xavi Hernández'", "'Viktor Orbán'", "'Jason Statham'");
    }
}
