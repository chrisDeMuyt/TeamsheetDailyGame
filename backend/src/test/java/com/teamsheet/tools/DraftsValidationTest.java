package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/** Structural checks on teamsheet-drafts.json (the unverified 50-match expansion). */
class DraftsValidationTest {

    static final Path DRAFTS = Path.of("../teamsheet-drafts.json");

    private static JsonNode read(Path p) throws Exception {
        return new ObjectMapper().readTree(Files.readString(p, StandardCharsets.UTF_8));
    }

    @Test
    void everyDraftIsStructurallyValid() throws Exception {
        List<String> problems = new ArrayList<>();
        for (JsonNode m : read(DRAFTS).get("matches")) problems.addAll(SeedSqlGenerator.validate(m));
        assertThat(problems).as("draft problems").isEmpty();
    }

    @Test
    void validatorCatchesBrokenLineups() throws Exception {
        JsonNode broken = new ObjectMapper().readTree("""
                {"id":"bad","date":"1999-05-26","formation":"4-4-2","squad":[
                  {"name":"A","pos":"GK","role":"starter"},{"name":"B","pos":"RB","role":"starter"},
                  {"name":"C","pos":"CB","role":"starter"},{"name":"D","pos":"CB","role":"starter"},
                  {"name":"E","pos":"LB","role":"starter"},{"name":"F","pos":"RM","role":"starter","off":70},
                  {"name":"G","pos":"CM","role":"starter"},{"name":"H","pos":"CM","role":"starter"},
                  {"name":"I","pos":"LW","role":"starter"},{"name":"J","pos":"ST","role":"starter"},
                  {"name":"K","pos":"ST","role":"starter"},
                  {"name":"S1","pos":"MF","role":"sub_used","on":60,"for":"F"},
                  {"name":"S2","pos":"FW","role":"sub_used","on":80,"for":"Nobody"}]}
                """);
        assertThat(String.join("\n", SeedSqlGenerator.validate(broken)))
                .contains("date before 2000")
                .contains("needs 1 LM")
                .contains("position LW not in 4-4-2")
                .contains("F should have off=60")
                .contains("'Nobody', who isn't on the pitch");
    }

    @Test
    void draftIdsAreUniqueAndDontClashWithTheLiveSeed() throws Exception {
        Set<String> seen = new HashSet<>();
        for (JsonNode m : read(SeedSqlGenerator.DEFAULT_JSON).get("matches")) seen.add(m.get("id").asText());
        for (JsonNode m : read(DRAFTS).get("matches"))
            assertThat(seen.add(m.get("id").asText())).as("duplicate id " + m.get("id")).isTrue();
    }

    @Test
    void draftsStayUnverifiedUntilSourced() throws Exception {
        for (JsonNode m : read(DRAFTS).get("matches")) {
            String id = m.get("id").asText();
            boolean sourced = m.get("sources").size() >= 2;
            String xi = m.get("verification").get("xi").asText();
            // Can't claim 'verified' without two sources (CLAUDE.md data rule)
            if (xi.equals("verified")) assertThat(sourced).as(id + " marked verified with <2 sources").isTrue();
            assertThat(m.get("draft_confidence").asText()).isIn("high", "medium", "low");
        }
    }
}
