package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamsheet.game.NameNormalizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.*;

/**
 * Generates V2__seed.sql from teamsheet-seed.json. The output is committed; SeedSqlGeneratorTest fails if it drifts.
 *
 * <pre>./mvnw -q test-compile exec:java -Dexec.mainClass=com.teamsheet.tools.SeedSqlGenerator -Dexec.classpathScope=test</pre>
 */
public final class SeedSqlGenerator {

    public static final Path DEFAULT_JSON = Path.of("../teamsheet-seed.json");
    public static final Path DEFAULT_OUT = Path.of("src/main/resources/db/migration/V2__seed.sql");
    public static final LocalDate FIRST_PUZZLE_DATE = LocalDate.of(2026, 11, 11);

    public static void main(String[] args) throws IOException {
        Path json = args.length > 0 ? Path.of(args[0]) : DEFAULT_JSON;
        Path out = args.length > 1 ? Path.of(args[1]) : DEFAULT_OUT;
        Files.writeString(out, generate(json), StandardCharsets.UTF_8);
        System.out.println("Wrote " + out.toAbsolutePath());
    }

    // ---------------------------------------------------------------- hand-curated data

    /**
     * Cold-start proxy: estimated % of players who'd name this player for these lineups (0-100).
     * PLACEHOLDER until real pageview/caps data lands. One value per person, shared across matches.
     */
    static final Map<String, Double> SEED_RARITY = Map.ofEntries(
            // Liverpool 2005
            Map.entry("Jerzy Dudek", 30.0), Map.entry("Steve Finnan", 8.0), Map.entry("Jamie Carragher", 45.0),
            Map.entry("Sami Hyypiä", 25.0), Map.entry("Djimi Traoré", 6.0), Map.entry("Luis García", 25.0),
            Map.entry("Steven Gerrard", 70.0), Map.entry("Xabi Alonso", 45.0), Map.entry("John Arne Riise", 20.0),
            Map.entry("Harry Kewell", 15.0), Map.entry("Milan Baroš", 12.0), Map.entry("Dietmar Hamann", 15.0),
            Map.entry("Vladimír Šmicer", 8.0), Map.entry("Djibril Cissé", 15.0),
            // Chelsea 2012
            Map.entry("Petr Čech", 40.0), Map.entry("José Bosingwa", 5.0), Map.entry("David Luiz", 30.0),
            Map.entry("Gary Cahill", 20.0), Map.entry("Ashley Cole", 35.0), Map.entry("John Obi Mikel", 12.0),
            Map.entry("Frank Lampard", 55.0), Map.entry("Salomon Kalou", 8.0), Map.entry("Juan Mata", 30.0),
            Map.entry("Ryan Bertrand", 3.0), Map.entry("Didier Drogba", 70.0), Map.entry("Florent Malouda", 10.0),
            Map.entry("Fernando Torres", 35.0),
            // Greece 2004
            Map.entry("Antonios Nikopolidis", 10.0), Map.entry("Giourkas Seitaridis", 8.0),
            Map.entry("Michalis Kapsis", 3.0), Map.entry("Traianos Dellas", 15.0), Map.entry("Takis Fyssas", 4.0),
            Map.entry("Kostas Katsouranis", 8.0), Map.entry("Theodoros Zagorakis", 25.0),
            Map.entry("Angelos Basinas", 10.0), Map.entry("Stelios Giannakopoulos", 15.0),
            Map.entry("Zisis Vryzas", 5.0), Map.entry("Angelos Charisteas", 55.0),
            Map.entry("Stylianos Venetidis", 1.5), Map.entry("Dimitris Papadopoulos", 2.0),
            // Spain 2010
            Map.entry("Iker Casillas", 55.0), Map.entry("Sergio Ramos", 50.0), Map.entry("Gerard Piqué", 40.0),
            Map.entry("Carles Puyol", 45.0), Map.entry("Joan Capdevila", 12.0), Map.entry("Sergio Busquets", 35.0),
            Map.entry("Pedro", 15.0), Map.entry("Xavi", 55.0), Map.entry("Andrés Iniesta", 60.0),
            Map.entry("David Villa", 40.0), Map.entry("Jesús Navas", 6.0), Map.entry("Cesc Fàbregas", 25.0),
            // Germany 2014
            Map.entry("Manuel Neuer", 55.0), Map.entry("Philipp Lahm", 40.0), Map.entry("Jérôme Boateng", 25.0),
            Map.entry("Mats Hummels", 30.0), Map.entry("Benedikt Höwedes", 6.0),
            Map.entry("Bastian Schweinsteiger", 45.0), Map.entry("Sami Khedira", 20.0),
            Map.entry("Thomas Müller", 55.0), Map.entry("Toni Kroos", 45.0), Map.entry("Mesut Özil", 35.0),
            Map.entry("Miroslav Klose", 50.0), Map.entry("Per Mertesacker", 8.0), Map.entry("André Schürrle", 15.0),
            Map.entry("Julian Draxler", 4.0),
            // Man Utd 2008
            Map.entry("Edwin van der Sar", 30.0), Map.entry("Wes Brown", 6.0), Map.entry("Rio Ferdinand", 40.0),
            Map.entry("Nemanja Vidić", 35.0), Map.entry("Patrice Evra", 25.0), Map.entry("Owen Hargreaves", 8.0),
            Map.entry("Michael Carrick", 20.0), Map.entry("Paul Scholes", 35.0),
            Map.entry("Cristiano Ronaldo", 75.0), Map.entry("Wayne Rooney", 55.0), Map.entry("Carlos Tevez", 30.0),
            Map.entry("Ryan Giggs", 20.0), Map.entry("Nani", 10.0), Map.entry("Anderson", 3.0),
            // Inter 2010
            Map.entry("Júlio César", 15.0), Map.entry("Maicon", 25.0), Map.entry("Lúcio", 15.0),
            Map.entry("Walter Samuel", 8.0), Map.entry("Cristian Chivu", 6.0), Map.entry("Javier Zanetti", 30.0),
            Map.entry("Esteban Cambiasso", 15.0), Map.entry("Samuel Eto'o", 45.0),
            Map.entry("Wesley Sneijder", 50.0), Map.entry("Goran Pandev", 5.0), Map.entry("Diego Milito", 45.0),
            Map.entry("Dejan Stanković", 4.0), Map.entry("Sulley Muntari", 3.0), Map.entry("Marco Materazzi", 15.0),
            // Portugal 2016
            Map.entry("Rui Patrício", 15.0), Map.entry("Cédric", 3.0), Map.entry("Pepe", 35.0),
            Map.entry("José Fonte", 8.0), Map.entry("Raphaël Guerreiro", 10.0), Map.entry("William Carvalho", 8.0),
            Map.entry("Renato Sanches", 15.0), Map.entry("João Mário", 8.0), Map.entry("Adrien Silva", 3.0),
            Map.entry("Ricardo Quaresma", 15.0), Map.entry("João Moutinho", 15.0), Map.entry("Éder", 30.0),
            // Italy 2006
            Map.entry("Gianluigi Buffon", 55.0), Map.entry("Gianluca Zambrotta", 15.0),
            Map.entry("Fabio Cannavaro", 45.0), Map.entry("Fabio Grosso", 15.0),
            Map.entry("Mauro Camoranesi", 8.0), Map.entry("Gennaro Gattuso", 30.0), Map.entry("Andrea Pirlo", 55.0),
            Map.entry("Simone Perrotta", 4.0), Map.entry("Francesco Totti", 45.0), Map.entry("Luca Toni", 20.0),
            Map.entry("Daniele De Rossi", 15.0), Map.entry("Vincenzo Iaquinta", 3.0),
            Map.entry("Alessandro Del Piero", 30.0),
            // Brazil 2002
            Map.entry("Marcos", 8.0), Map.entry("Edmílson", 5.0), Map.entry("Roque Júnior", 5.0),
            Map.entry("Cafu", 40.0), Map.entry("Gilberto Silva", 12.0), Map.entry("Kléberson", 4.0),
            Map.entry("Roberto Carlos", 55.0), Map.entry("Ronaldinho", 50.0), Map.entry("Rivaldo", 40.0),
            Map.entry("Ronaldo", 70.0), Map.entry("Juninho Paulista", 6.0), Map.entry("Denílson", 5.0)
    );

    /** Pitch labels where "surname" isn't what people say. */
    static final Map<String, String> SHORT_NAME_OVERRIDES = Map.ofEntries(
            Map.entry("Luis García", "Luis García"), Map.entry("David Luiz", "David Luiz"),
            Map.entry("Júlio César", "Júlio César"), Map.entry("João Mário", "João Mário"),
            Map.entry("Rui Patrício", "Rui Patrício"), Map.entry("Adrien Silva", "Adrien"),
            Map.entry("Gilberto Silva", "Gilberto Silva"), Map.entry("Roque Júnior", "Roque Júnior"),
            Map.entry("Roberto Carlos", "Roberto Carlos"), Map.entry("Juninho Paulista", "Juninho"),
            Map.entry("Cristiano Ronaldo", "Ronaldo"), Map.entry("John Obi Mikel", "Mikel")
    );

    /** Nicknames / common forms, beyond full name, surname and short name. */
    static final Map<String, List<String>> EXTRA_ALIASES = Map.ofEntries(
            Map.entry("Xabi Alonso", List.of("xabi")),
            Map.entry("Cristiano Ronaldo", List.of("cr7", "cristiano", "ronaldo")),
            Map.entry("Ronaldo", List.of("r9", "ronaldo nazario")),
            Map.entry("John Obi Mikel", List.of("obi mikel", "obi", "mikel john obi")),
            Map.entry("Júlio César", List.of("julio")),
            Map.entry("Gilberto Silva", List.of("gilberto")),
            Map.entry("Cesc Fàbregas", List.of("cesc")),
            Map.entry("Xavi", List.of("xavi hernandez")),
            Map.entry("Pedro", List.of("pedro rodriguez")),
            Map.entry("Bastian Schweinsteiger", List.of("schweini")),
            Map.entry("Steven Gerrard", List.of("stevie g")),
            Map.entry("Gianluigi Buffon", List.of("gigi buffon")),
            Map.entry("Djimi Traoré", List.of("djimi"))
    );

    private static final Set<String> PARTICLES = Set.of("van", "von", "der", "den", "de", "del", "della", "da", "di", "dos", "do");

    /** Pitch coords per formation, x 0 = left touchline, y 0 = own goal line. Repeated positions run right to left. */
    static final Map<String, Map<String, int[][]>> FORMATIONS = Map.ofEntries(
            Map.entry("4-4-1-1", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "RM", xy(85, 50), "CM", xy(60, 46, 40, 46), "LM", xy(15, 50), "CAM", xy(50, 67), "ST", xy(50, 85))),
            Map.entry("4-2-3-1", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "DM", xy(62, 42, 38, 42), "RW", xy(82, 66), "CAM", xy(50, 64), "LW", xy(18, 66), "ST", xy(50, 85))),
            Map.entry("4-3-1-2", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "DM", xy(50, 40), "CM", xy(72, 50, 28, 50), "CAM", xy(50, 65), "ST", xy(62, 84, 38, 84))),
            Map.entry("4-4-2", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "RM", xy(85, 52), "CM", xy(60, 48, 40, 48), "LM", xy(15, 52), "ST", xy(62, 82, 38, 82))),
            Map.entry("4-4-2 (diamond)", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "DM", xy(50, 40), "RM", xy(75, 52), "LM", xy(25, 52), "CAM", xy(50, 65), "ST", xy(62, 84, 38, 84))),
            Map.entry("3-4-2-1", Map.of(
                    "GK", xy(50, 5), "CB", xy(75, 22, 50, 20, 25, 22), "RWB", xy(88, 48), "CM", xy(60, 45, 40, 45),
                    "LWB", xy(12, 48), "AM", xy(65, 66, 35, 66), "ST", xy(50, 85))),
            // Added for the 50-match expansion
            Map.entry("4-3-3", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "DM", xy(50, 40), "CM", xy(68, 52, 32, 52), "RW", xy(82, 72), "ST", xy(50, 85), "LW", xy(18, 72))),
            Map.entry("4-1-4-1", Map.of(
                    "GK", xy(50, 5), "RB", xy(85, 25), "CB", xy(62, 20, 38, 20), "LB", xy(15, 25),
                    "DM", xy(50, 38), "RM", xy(85, 56), "CM", xy(62, 55, 38, 55), "LM", xy(15, 56), "ST", xy(50, 85))),
            Map.entry("3-5-2", Map.of(
                    "GK", xy(50, 5), "CB", xy(75, 22, 50, 20, 25, 22), "RWB", xy(88, 48),
                    "CM", xy(68, 52, 50, 44, 32, 52), "LWB", xy(12, 48), "ST", xy(62, 82, 38, 82))),
            Map.entry("3-4-3", Map.of(
                    "GK", xy(50, 5), "CB", xy(75, 22, 50, 20, 25, 22), "RWB", xy(88, 48), "CM", xy(60, 45, 40, 45),
                    "LWB", xy(12, 48), "RW", xy(80, 74), "ST", xy(50, 85), "LW", xy(20, 74))),
            Map.entry("3-2-4-1", Map.of(
                    "GK", xy(50, 5), "CB", xy(75, 22, 50, 20, 25, 22), "DM", xy(62, 40, 38, 40),
                    "RW", xy(85, 66), "AM", xy(62, 64, 38, 64), "LW", xy(15, 66), "ST", xy(50, 85)))
    );

    static final Set<String> SUB_POSITIONS = Set.of("GK", "DF", "MF", "FW");

    /**
     * Structural checks shared by the live seed and the drafts file. Returns problems; empty = valid.
     * 11 starters whose positions fill the formation template exactly; every sub comes on for someone on the
     * pitch, at the minute that player went off.
     */
    public static List<String> validate(JsonNode m) {
        List<String> problems = new ArrayList<>();
        String id = m.path("id").asText("<no id>");
        String formation = m.path("formation").asText();
        Map<String, int[][]> template = FORMATIONS.get(formation);
        if (template == null) problems.add(id + ": no template for formation '" + formation + "'");
        if (m.path("date").asText("").compareTo("2000-01-01") < 0) problems.add(id + ": date before 2000 or missing");

        Map<String, JsonNode> byName = new LinkedHashMap<>();
        Map<String, Integer> posCount = new HashMap<>();
        int starters = 0;
        for (JsonNode p : m.path("squad")) {
            String name = p.path("name").asText();
            if (byName.put(name, p) != null) problems.add(id + ": duplicate player " + name);
            if (p.path("role").asText().equals("starter")) {
                starters++;
                posCount.merge(p.path("pos").asText(), 1, Integer::sum);
            }
        }
        if (starters != 11) problems.add(id + ": " + starters + " starters, expected 11");
        if (template != null) {
            for (var t : template.entrySet())
                if (posCount.getOrDefault(t.getKey(), 0) != t.getValue().length)
                    problems.add(id + ": formation " + formation + " needs " + t.getValue().length + " " + t.getKey()
                            + ", squad has " + posCount.getOrDefault(t.getKey(), 0));
            for (String pos : posCount.keySet())
                if (!template.containsKey(pos)) problems.add(id + ": position " + pos + " not in " + formation);
        }

        Set<String> onPitch = new HashSet<>();
        for (JsonNode p : m.path("squad")) if (p.path("role").asText().equals("starter")) onPitch.add(p.path("name").asText());
        for (JsonNode p : m.path("squad")) {
            if (!p.path("role").asText().equals("sub_used")) continue;
            String name = p.path("name").asText();
            if (!SUB_POSITIONS.contains(p.path("pos").asText())) problems.add(id + ": sub " + name + " pos must be GK/DF/MF/FW");
            if (!p.path("on").isInt()) { problems.add(id + ": sub " + name + " has no 'on' minute"); continue; }
            String replaced = p.path("for").asText();
            JsonNode off = byName.get(replaced);
            if (off == null || !onPitch.contains(replaced)) {
                problems.add(id + ": sub " + name + " comes on for '" + replaced + "', who isn't on the pitch");
            } else if (off.path("off").asInt(-1) != p.path("on").asInt()) {
                problems.add(id + ": " + replaced + " should have off=" + p.path("on").asInt() + " (replaced by " + name + ")");
            }
            onPitch.add(name);
        }
        return problems;
    }

    private static int[][] xy(int... v) {
        int[][] out = new int[v.length / 2][];
        for (int i = 0; i < out.length; i++) out[i] = new int[]{v[2 * i], v[2 * i + 1]};
        return out;
    }

    // ---------------------------------------------------------------- generation

    public static String generate(Path jsonPath) throws IOException {
        JsonNode root = new ObjectMapper().readTree(Files.readString(jsonPath, StandardCharsets.UTF_8));
        JsonNode matches = root.get("matches");
        List<String> problems = new ArrayList<>();
        for (JsonNode m : matches) problems.addAll(validate(m));
        if (!problems.isEmpty()) throw new IllegalStateException("Invalid seed:\n  " + String.join("\n  ", problems));

        Map<String, Long> playerIds = new LinkedHashMap<>();
        for (JsonNode m : matches)
            for (JsonNode p : m.get("squad"))
                playerIds.putIfAbsent(p.get("name").asText(), (long) playerIds.size() + 1);

        StringBuilder sql = new StringBuilder();
        sql.append("-- GENERATED by com.teamsheet.tools.SeedSqlGenerator from teamsheet-seed.json. Do not edit by hand.\n");
        sql.append("-- seed_rarity values are hand-estimated placeholders (expected pick %) until real proxy data exists.\n\n");

        sql.append("-- ---------- players ----------\n");
        sql.append("INSERT INTO player (id, display_name, short_name, seed_rarity) VALUES\n");
        List<String> rows = new ArrayList<>();
        for (var e : playerIds.entrySet()) {
            Double seed = SEED_RARITY.get(e.getKey());
            if (seed == null) throw new IllegalStateException("No seed rarity for " + e.getKey());
            rows.add(String.format(Locale.ROOT, "  (%d, %s, %s, %.2f)",
                    e.getValue(), q(e.getKey()), q(shortName(e.getKey())), seed));
        }
        sql.append(String.join(",\n", rows)).append(";\n");
        sql.append("SELECT setval('player_id_seq', ").append(playerIds.size()).append(");\n\n");

        sql.append("-- ---------- aliases (normalized: lower, accents stripped, punctuation removed) ----------\n");
        sql.append("INSERT INTO player_alias (player_id, alias_norm) VALUES\n");
        rows = new ArrayList<>();
        for (var e : playerIds.entrySet())
            for (String a : aliases(e.getKey()))
                rows.add("  (%d, %s)".formatted(e.getValue(), q(a)));
        sql.append(String.join(",\n", rows)).append(";\n\n");

        sql.append("-- ---------- matches ----------\n");
        for (JsonNode m : matches) {
            List<String> sources = new ArrayList<>();
            m.get("sources").forEach(s -> sources.add(q(s.asText())));
            sql.append("INSERT INTO match (id, match_date, competition, team, opponent, result, formation, xi_status, subs_status, sources) VALUES (")
                    .append(String.join(", ", q(m.get("id").asText()), q(m.get("date").asText()) + "::date",
                            q(m.get("competition").asText()), q(m.get("team").asText()), q(m.get("opponent").asText()),
                            q(m.get("result").asText()), q(m.get("formation").asText()),
                            q(m.get("verification").get("xi").asText()), q(m.get("verification").get("subs_used").asText()),
                            "ARRAY[" + String.join(", ", sources) + "]"))
                    .append(");\n");
        }
        sql.append('\n');

        sql.append("-- ---------- match_player (pitch x/y from formation templates; subs have NULL coords) ----------\n");
        for (JsonNode m : matches) {
            String formation = m.get("formation").asText();
            Map<String, int[][]> template = FORMATIONS.get(formation);
            if (template == null) throw new IllegalStateException("No template for formation " + formation);
            Map<String, Integer> used = new HashMap<>();
            sql.append("INSERT INTO match_player (match_id, player_id, role, slot_pos, slot_x, slot_y, minute_on, minute_off, replaced_id) VALUES\n");
            rows = new ArrayList<>();
            for (JsonNode p : m.get("squad")) {
                String name = p.get("name").asText();
                String pos = p.get("pos").asText();
                String role = p.get("role").asText();
                String x = "NULL", y = "NULL", on = "NULL", replaced = "NULL";
                String off = p.has("off") ? p.get("off").asText() : "NULL";
                if (role.equals("starter")) {
                    int[][] coords = template.get(pos);
                    int n = used.merge(pos, 1, Integer::sum) - 1;
                    if (coords == null || n >= coords.length)
                        throw new IllegalStateException(m.get("id").asText() + ": no slot for " + pos + " #" + (n + 1));
                    x = String.valueOf(coords[n][0]);
                    y = String.valueOf(coords[n][1]);
                } else {
                    on = p.get("on").asText();
                    replaced = String.valueOf(Objects.requireNonNull(playerIds.get(p.get("for").asText()),
                            "unknown replaced player " + p.get("for")));
                }
                rows.add("  (%s, %d, '%s', %s, %s, %s, %s, %s, %s)".formatted(
                        q(m.get("id").asText()), playerIds.get(name), role, q(pos), x, y, on, off, replaced));
            }
            for (var t : template.entrySet())
                if (used.getOrDefault(t.getKey(), 0) != t.getValue().length)
                    throw new IllegalStateException(m.get("id").asText() + ": " + t.getKey() + " count mismatch");
            sql.append(String.join(",\n", rows)).append(";\n");
        }
        sql.append('\n');

        sql.append("-- ---------- puzzles #1-").append(matches.size()).append(" ----------\n");
        sql.append("INSERT INTO puzzle (number, puzzle_date, match_id) VALUES\n");
        rows = new ArrayList<>();
        for (int i = 0; i < matches.size(); i++)
            rows.add("  (%d, DATE '%s', %s)".formatted(i + 1, FIRST_PUZZLE_DATE.plusDays(i), q(matches.get(i).get("id").asText())));
        sql.append(String.join(",\n", rows)).append(";\n");
        return sql.toString();
    }

    static String shortName(String display) {
        String o = SHORT_NAME_OVERRIDES.get(display);
        if (o != null) return o;
        String[] parts = display.split(" ");
        if (parts.length == 1) return display;
        for (int i = 1; i < parts.length - 1; i++)
            if (PARTICLES.contains(parts[i].toLowerCase(Locale.ROOT)))
                return String.join(" ", Arrays.copyOfRange(parts, i, parts.length));
        return parts[parts.length - 1];
    }

    static Set<String> aliases(String display) {
        Set<String> out = new TreeSet<>();
        String full = NameNormalizer.normalize(display);
        String shortN = NameNormalizer.normalize(shortName(display));
        out.add(full);
        out.add(shortN);
        out.add(full.substring(full.lastIndexOf(' ') + 1));          // surname
        if (shortN.contains(" ")) out.add(shortN.replace(" ", ""));  // "vandersar", "delpiero"
        for (String extra : EXTRA_ALIASES.getOrDefault(display, List.of())) out.add(NameNormalizer.normalize(extra));
        return out;
    }

    private static String q(String s) {
        return "'" + s.replace("'", "''") + "'";
    }
}
