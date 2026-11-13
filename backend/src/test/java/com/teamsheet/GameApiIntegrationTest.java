package com.teamsheet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsheet.TestcontainersConfig.MutableClock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Full API against real Postgres 16 (pg_trgm + unaccent) with V1 + V2 migrations applied. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfig.class)
class GameApiIntegrationTest {

    // Puzzle #1 = Liverpool 2005 (2026-11-11), #4 = Spain 2010 (2026-11-14)
    static final Instant DAY1 = Instant.parse("2026-11-11T12:00:00Z");
    static final Instant SPAIN_DAY = Instant.parse("2026-11-14T12:00:00Z");

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired MutableClock clock;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void resetClock() {
        clock.set(DAY1);
    }

    // ---------------------------------------------------------------- required cases

    @Test
    void hit() throws Exception {
        String play = newPlay();
        JsonNode r = guess(play, "Gerrard");
        assertThat(r.get("result").asText()).isEqualTo("hit");
        assertThat(r.get("name").asText()).isEqualTo("Steven Gerrard");
        assertThat(r.get("guessIndex").asInt()).isEqualTo(1);
        assertThat(r.get("guessesLeft").asInt()).isEqualTo(2);
        assertThat(r.get("tier").asText()).isEqualTo("Tap-in");   // seed 70% -> 10 pts
        assertThat(r.get("points").asInt()).isEqualTo(10);
        assertThat(r.get("slot").asInt()).isBetween(1, 14);
    }

    @Test
    void fuzzyTypoStillHits() throws Exception {
        JsonNode r = guess(newPlay(), "Steven Gerard");
        assertThat(r.get("result").asText()).isEqualTo("hit");
        assertThat(r.get("name").asText()).isEqualTo("Steven Gerrard");
    }

    @Test
    void miss() throws Exception {
        String play = newPlay();
        JsonNode r = guess(play, "Thierry Henry");
        assertThat(r.get("result").asText()).isEqualTo("miss");
        assertThat(r.get("guessIndex").asInt()).isEqualTo(1);
        assertThat(r.get("guessesLeft").asInt()).isEqualTo(2);
        assertThat(r.get("points").asInt()).isZero();
        assertThat(r.has("name")).isFalse();
    }

    @Test
    void scopedToThisMatchOnly() throws Exception {
        // Xavi is in the DB (Spain 2010) but not in Liverpool 2005's pool
        assertThat(guess(newPlay(), "Xavi").get("result").asText()).isEqualTo("miss");
    }

    @Test
    void accentInsensitiveHit() throws Exception {
        String play = newPlay();
        JsonNode r = guess(play, "SMICER");
        assertThat(r.get("result").asText()).isEqualTo("hit");
        assertThat(r.get("name").asText()).isEqualTo("Vladimír Šmicer");
        // seed 8% -> Proper Fan 60, +15 sub bonus
        assertThat(r.get("tier").asText()).isEqualTo("Proper Fan");
        assertThat(r.get("points").asInt()).isEqualTo(75);

        JsonNode r2 = guess(play, "sami hyypia");
        assertThat(r2.get("result").asText()).isEqualTo("hit");
        assertThat(r2.get("name").asText()).isEqualTo("Sami Hyypiä");
    }

    @Test
    void duplicateIsBlockedAndDoesNotBurnGuess() throws Exception {
        String play = newPlay();
        guess(play, "Gerrard");
        JsonNode dupe = guess(play, "Steven Gerrard");
        assertThat(dupe.get("result").asText()).isEqualTo("duplicate");
        assertThat(dupe.has("guessIndex")).isFalse();
        assertThat(dupe.get("guessesLeft").asInt()).isEqualTo(2);
        assertThat(dupe.get("points").asInt()).isZero();
        assertThat(dupe.get("totalScore").asInt()).isEqualTo(10);

        JsonNode next = guess(play, "Carragher");
        assertThat(next.get("guessIndex").asInt()).isEqualTo(2);
    }

    @Test
    void ambiguousDoesNotBurnGuessOrLeakNames() throws Exception {
        clock.set(SPAIN_DAY);
        String play = newPlay();
        JsonNode r = guess(play, "Sergio");       // Ramos and Busquets
        assertThat(r.get("result").asText()).isEqualTo("ambiguous");
        assertThat(r.get("guessesLeft").asInt()).isEqualTo(3);
        assertThat(r.has("guessIndex")).isFalse();
        assertThat(r.has("name")).isFalse();
        assertThat(r.toString().toLowerCase(Locale.ROOT)).doesNotContain("ramos", "busquets");

        JsonNode next = guess(play, "Sergio Ramos");
        assertThat(next.get("result").asText()).isEqualTo("hit");
        assertThat(next.get("guessIndex").asInt()).isEqualTo(1);
    }

    @Test
    void timeoutBurnsGuessEvenWithCorrectName() throws Exception {
        String play = newPlay();
        clock.advanceSeconds(33);                  // > 30s + 2s grace
        JsonNode r = guess(play, "Gerrard");
        assertThat(r.get("result").asText()).isEqualTo("timeout");
        assertThat(r.get("guessIndex").asInt()).isEqualTo(1);
        assertThat(r.get("points").asInt()).isZero();
        assertThat(r.has("name")).isFalse();

        // timer restarted: an immediate second guess is on time
        JsonNode next = guess(play, "Gerrard");
        assertThat(next.get("result").asText()).isEqualTo("hit");
        assertThat(next.get("guessIndex").asInt()).isEqualTo(2);
    }

    @Test
    void emptyTextIsTheClientsTimeout() throws Exception {
        JsonNode r = guess(newPlay(), "");
        assertThat(r.get("result").asText()).isEqualTo("timeout");
        assertThat(r.get("guessIndex").asInt()).isEqualTo(1);
        assertThat(r.get("guessesLeft").asInt()).isEqualTo(2);
    }

    @Test
    void withinGraceIsNotATimeout() throws Exception {
        String play = newPlay();
        clock.advanceSeconds(31);                  // past 30s but inside the 2s grace
        assertThat(guess(play, "Gerrard").get("result").asText()).isEqualTo("hit");
    }

    @Test
    void revealBlockedBeforeThirdGuess() throws Exception {
        String play = newPlay();
        mvc.perform(get("/api/plays/{id}/reveal", play)).andExpect(status().isConflict());
        guess(play, "Gerrard");
        guess(play, "Henry");
        mvc.perform(get("/api/plays/{id}/reveal", play)).andExpect(status().isConflict());
        guess(play, "Smicer");
        mvc.perform(get("/api/plays/{id}/reveal", play)).andExpect(status().isOk());
    }

    // ---------------------------------------------------------------- extras

    @Test
    void todayNeverLeaksPlayerNames() throws Exception {
        JsonNode today = body(mvc.perform(get("/api/puzzle/today")), status().isOk());
        assertThat(today.get("number").asInt()).isEqualTo(1);
        assertThat(today.get("team").asText()).isEqualTo("Liverpool");
        assertThat(today.get("slots")).hasSize(14);

        String raw = today.toString().toLowerCase(Locale.ROOT);
        List<String> names = jdbc.queryForList("""
                SELECT lower(p.short_name) FROM match_player mp JOIN player p ON p.id = mp.player_id
                WHERE mp.match_id = '2005-ucl-final-liverpool'""", String.class);
        assertThat(names).hasSize(14).allSatisfy(n -> assertThat(raw).doesNotContain(n));
        assertThat(raw).doesNotContain("player");
    }

    @Test
    void createPlayIsIdempotentPerDevicePerDay() throws Exception {
        String anon = UUID.randomUUID().toString();
        String first = createPlay(anon).get("playId").asText();
        clock.advanceSeconds(5);
        JsonNode again = createPlay(anon);
        assertThat(again.get("playId").asText()).isEqualTo(first);
        assertThat(again.get("deadline").asText()).isEqualTo("2026-11-11T12:00:30Z");  // timer not reset
    }

    @Test
    void fullGameRevealHasPoolPctAndTotal() throws Exception {
        String play = newPlay();
        int a = guess(play, "Gerrard").get("points").asInt();
        int b = guess(play, "Djimi Traore").get("points").asInt();
        JsonNode last = guess(play, "Nobody");
        assertThat(last.get("finished").asBoolean()).isTrue();
        assertThat(last.has("deadline")).isFalse();

        mvc.perform(post("/api/plays/{id}/guesses", play).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"Gerrard\"}")).andExpect(status().isConflict());

        JsonNode reveal = body(mvc.perform(get("/api/plays/{id}/reveal", play)), status().isOk());
        assertThat(reveal.get("totalScore").asInt()).isEqualTo(a + b).isEqualTo(10 + 60);
        assertThat(reveal.get("pool")).hasSize(14);
        assertThat(reveal.get("pctSource").asText()).isEqualTo("seed");
        assertThat(reveal.get("picks")).hasSize(3);
        assertThat(reveal.toString()).contains("Vladimír Šmicer");
        long pickedCount = 0;
        for (JsonNode p : reveal.get("pool")) if (p.get("picked").asBoolean()) pickedCount++;
        assertThat(pickedCount).isEqualTo(2);
    }

    @Test
    void resumeReturnsHistoryAndKeepsTimeoutDistinctFromMiss() throws Exception {
        String anon = UUID.randomUUID().toString();
        String play = createPlay(anon).get("playId").asText();
        guess(play, "Smicer");                     // hit (sub)
        guess(play, "Thierry Henry");              // miss

        JsonNode resumed = createPlay(anon);       // simulates a page refresh
        assertThat(resumed.get("playId").asText()).isEqualTo(play);
        assertThat(resumed.get("guessesUsed").asInt()).isEqualTo(2);
        JsonNode h = resumed.get("guesses");
        assertThat(h).hasSize(2);
        assertThat(h.get(0).get("result").asText()).isEqualTo("hit");
        assertThat(h.get(0).get("shortName").asText()).isEqualTo("Šmicer");
        assertThat(h.get(0).get("sub").asBoolean()).isTrue();
        assertThat(h.get(0).get("slot").asInt()).isPositive();
        assertThat(h.get(0).get("points").asInt()).isEqualTo(75);
        assertThat(h.get(1).get("result").asText()).isEqualTo("miss");
        assertThat(h.get(1).has("name")).isFalse();
        // only the player's own hit is named; nobody else from the pool leaks
        assertThat(resumed.toString()).doesNotContain("Gerrard", "Carragher");

        clock.advanceSeconds(40);
        assertThat(guess(play, "Gerrard").get("result").asText()).isEqualTo("timeout");
        JsonNode reveal = body(mvc.perform(get("/api/plays/{id}/reveal", play)), status().isOk());
        assertThat(reveal.get("picks").get(2).get("result").asText()).isEqualTo("timeout");
        assertThat(createPlay(anon).get("guesses").get(2).get("result").asText()).isEqualTo("timeout");
    }

    @Test
    void hitCarriesShortNameAndSubFlag() throws Exception {
        JsonNode r = guess(newPlay(), "Cisse");
        assertThat(r.get("shortName").asText()).isEqualTo("Cissé");
        assertThat(r.get("sub").asBoolean()).isTrue();
    }

    @Test
    void dailyResetIsMidnightUsCentral() throws Exception {
        // 2026-11-11 23:59:59 CST (UTC-6) is still puzzle #1 even though UTC is already Nov 12
        clock.set(Instant.parse("2026-11-12T05:59:59Z"));
        assertThat(body(mvc.perform(get("/api/puzzle/today")), status().isOk()).get("number").asInt()).isEqualTo(1);
        // midnight CST flips to #2
        clock.set(Instant.parse("2026-11-12T06:00:00Z"));
        assertThat(body(mvc.perform(get("/api/puzzle/today")), status().isOk()).get("number").asInt()).isEqualTo(2);
    }

    @Test
    void unknownPlayIs404() throws Exception {
        mvc.perform(post("/api/plays/{id}/guesses", UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"x\"}")).andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- helpers

    private String newPlay() throws Exception {
        return createPlay(UUID.randomUUID().toString()).get("playId").asText();
    }

    private JsonNode createPlay(String anonId) throws Exception {
        return body(mvc.perform(post("/api/plays").contentType(MediaType.APPLICATION_JSON)
                .content("{\"anonId\":\"" + anonId + "\"}")), status().isOk());
    }

    private JsonNode guess(String playId, String text) throws Exception {
        return body(mvc.perform(post("/api/plays/{id}/guesses", playId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(java.util.Map.of("text", text)))), status().isOk());
    }

    private JsonNode body(org.springframework.test.web.servlet.ResultActions actions, ResultMatcher expected) throws Exception {
        String s = actions.andExpect(expected).andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return json.readTree(s);
    }
}
