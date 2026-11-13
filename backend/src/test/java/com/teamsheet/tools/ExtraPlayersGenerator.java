package com.teamsheet.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.teamsheet.game.NameNormalizer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Writes the "Did you mean" player universe migration from data/wikidata-footballers.json (Wikidata, CC0).
 * <p>
 * Extra players are never in a puzzle: seed_rarity NULL, plus a {@code fame} (Wikipedia sitelinks) used to rank
 * suggestions. Candidates that ARE existing puzzle players are merged (fame + extra aliases), never duplicated,
 * because a duplicate "Xavi Hernández" row would turn a correct pick into a miss.
 *
 * <pre>./mvnw -q test-compile exec:java -Dexec.mainClass=com.teamsheet.tools.ExtraPlayersGenerator -Dexec.classpathScope=test</pre>
 */
public final class ExtraPlayersGenerator {

    public static final Path DATA = Path.of("../data/wikidata-footballers.json");
    public static final String SUFFIX = "__extra_players.sql";
    private static final int MAX_EXTRA_ALIASES = 5;
    /** "Son Heung-min", "Park Ji-sung": family name first (given name is "Xx-yy" with a lower-case second part). */
    private static final Pattern EAST_ASIAN = Pattern.compile("^\\p{Lu}\\p{Ll}+ \\p{Lu}\\p{Ll}+-\\p{Ll}+$");

    public record Result(String sql, int added, List<String> mergedByName, List<String> mergedByAltName, int duplicatesSkipped) {}

    public static void main(String[] args) throws IOException {
        try (var files = Files.list(SeedSqlGenerator.MIGRATIONS)) {
            if (files.anyMatch(p -> p.getFileName().toString().endsWith(SUFFIX)))
                throw new IllegalStateException("An extra-players migration already exists; write a new one for changes.");
        }
        SeedSqlGenerator.Existing ex = SeedSqlGenerator.scan(SeedSqlGenerator.MIGRATIONS);
        Result r = build(new ObjectMapper().readTree(Files.readString(DATA, StandardCharsets.UTF_8)), ex);
        Path out = SeedSqlGenerator.MIGRATIONS.resolve("V" + (ex.maxVersion() + 1) + SUFFIX);
        Files.writeString(out, r.sql(), StandardCharsets.UTF_8);
        System.out.printf("Wrote %s: %d new players, %d merged by exact name, %d duplicates skipped%n",
                out, r.added(), r.mergedByName().size(), r.duplicatesSkipped());
        System.out.println("Merged by ALTERNATIVE name (review these):");
        r.mergedByAltName().forEach(s -> System.out.println("  " + s));
    }

    public static Result build(JsonNode data, SeedSqlGenerator.Existing ex) {
        Map<String, String> existingByNorm = new HashMap<>();   // normalized display -> display
        for (String name : ex.playerIds().keySet()) existingByNorm.put(NameNormalizer.normalize(name), name);
        // Multi-word aliases owned by exactly one existing player: "xavi hernandez" -> Xavi, "ronaldo nazario" -> Ronaldo.
        Map<Long, String> displayById = new HashMap<>();
        ex.playerIds().forEach((name, id) -> displayById.put(id, name));
        Map<String, Set<Long>> owners = new HashMap<>();
        ex.aliases().forEach((id, as) -> as.stream().filter(a -> a.contains(" "))
                .forEach(a -> owners.computeIfAbsent(a, k -> new HashSet<>()).add(id)));
        Map<String, String> existingByFullAlias = new HashMap<>();
        owners.forEach((a, ids) -> { if (ids.size() == 1) existingByFullAlias.put(a, displayById.get(ids.iterator().next())); });

        List<JsonNode> candidates = new ArrayList<>();
        data.get("players").forEach(candidates::add);
        Set<String> labelClaims = new HashSet<>();
        for (JsonNode c : candidates) labelClaims.add(NameNormalizer.normalize(c.get("label").asText()));

        Map<String, Integer> fameOfExisting = new LinkedHashMap<>();       // existing display -> fame
        Map<String, Set<String>> extraAliasesOfExisting = new LinkedHashMap<>();
        List<String> mergedByName = new ArrayList<>(), mergedByAlt = new ArrayList<>();
        Set<String> seenLabels = new HashSet<>();
        List<JsonNode> added = new ArrayList<>();
        int dups = 0;

        for (JsonNode c : candidates) {
            String label = c.get("label").asText().trim();
            String ln = NameNormalizer.normalize(label);
            int fame = c.get("fame").asInt();
            if (ln.length() < 2) continue;

            String exact = existingByNorm.get(ln);
            if (exact == null) {
                String byAlias = existingByFullAlias.get(ln);   // "Xavi Hernández" is the existing "Xavi"
                if (byAlias != null && !labelClaims.contains(NameNormalizer.normalize(byAlias))) exact = byAlias;
            }
            if (exact != null) {
                if (fameOfExisting.putIfAbsent(exact, fame) == null) mergedByName.add(exact);
                else dups++;
                seenLabels.add(ln);
                continue;
            }
            // Alt-name merge only when no candidate's main label claims that existing name (Cristiano's alt
            // "Ronaldo" must not swallow the Brazilian Ronaldo).
            List<String> viaAlt = new ArrayList<>();
            for (JsonNode a : c.path("alts")) {
                String an = NameNormalizer.normalize(a.asText());
                String hit = existingByNorm.get(an);
                if (hit != null && !labelClaims.contains(an) && !viaAlt.contains(hit)) viaAlt.add(hit);
            }
            if (viaAlt.size() == 1 && !fameOfExisting.containsKey(viaAlt.get(0))) {
                String hit = viaAlt.get(0);
                fameOfExisting.put(hit, fame);
                extraAliasesOfExisting.computeIfAbsent(hit, k -> new TreeSet<>()).add(ln);
                mergedByAlt.add("\"" + label + "\" (" + c.get("qid").asText() + ") -> existing \"" + hit + "\"");
                seenLabels.add(ln);
                continue;
            }
            if (!seenLabels.add(ln)) { dups++; continue; }
            added.add(c);
        }

        long next = ex.maxPlayerId();
        StringBuilder sql = new StringBuilder();
        sql.append("-- GENERATED by com.teamsheet.tools.ExtraPlayersGenerator from data/wikidata-footballers.json\n");
        sql.append("-- (Wikidata, CC0). Do not edit by hand. Extra players are never in a puzzle (seed_rarity NULL);\n");
        sql.append("-- fame = Wikipedia sitelinks, used only to rank \"Did you mean\" suggestions.\n\n");
        sql.append("ALTER TABLE player ADD COLUMN fame INT;\n\n");

        if (!fameOfExisting.isEmpty()) {
            sql.append("-- ---------- fame for existing puzzle players ----------\n");
            for (var e : fameOfExisting.entrySet())
                sql.append("UPDATE player SET fame = %d WHERE id = %d;\n".formatted(e.getValue(), ex.playerIds().get(e.getKey())));
            sql.append('\n');
        }

        List<String> aliasRows = new ArrayList<>();
        if (!added.isEmpty()) {
            sql.append("-- ---------- extra players ----------\n");
            sql.append("INSERT INTO player (id, display_name, short_name, seed_rarity, fame) VALUES\n");
            List<String> rows = new ArrayList<>();
            for (JsonNode c : added) {
                String label = c.get("label").asText().trim();
                ObjectNode entry = extrasEntry(label, c, existingByNorm.keySet());
                long id = ++next;
                rows.add("  (%d, %s, %s, NULL, %d)".formatted(id, q(label), q(SeedSqlGenerator.shortName(label, entry)), c.get("fame").asInt()));
                for (String a : SeedSqlGenerator.aliases(label, entry)) aliasRows.add("  (%d, %s)".formatted(id, q(a)));
            }
            sql.append(String.join(",\n", rows)).append(";\n");
            sql.append("SELECT setval('player_id_seq', ").append(next).append(");\n\n");
        }
        for (var e : extraAliasesOfExisting.entrySet()) {
            long id = ex.playerIds().get(e.getKey());
            Set<String> have = ex.aliases().getOrDefault(id, Set.of());
            for (String a : e.getValue()) if (!have.contains(a)) aliasRows.add("  (%d, %s)".formatted(id, q(a)));
        }
        if (!aliasRows.isEmpty()) {
            sql.append("-- ---------- aliases ----------\n");
            sql.append("INSERT INTO player_alias (player_id, alias_norm) VALUES\n")
                    .append(String.join(",\n", aliasRows)).append("\nON CONFLICT DO NOTHING;\n");
        }
        return new Result(sql.toString(), added.size(), mergedByName, mergedByAlt, dups);
    }

    /** Short name / aliases for an extra player: a few clean Wikidata alt names, East Asian family-first names. */
    private static ObjectNode extrasEntry(String label, JsonNode c, Set<String> existingNames) {
        ObjectNode e = new ObjectMapper().createObjectNode();
        if (EAST_ASIAN.matcher(label).matches()) {
            e.put("family_first", true);
            e.put("short", label);
        }
        var aliases = e.putArray("aliases");
        int n = 0;
        for (JsonNode a : c.path("alts")) {
            String an = NameNormalizer.normalize(a.asText());
            if (an.length() < 3 || an.length() > 30 || !an.matches(".*[a-z].*")) continue;
            if (existingNames.contains(an)) continue;     // never let an extra claim a puzzle player's full name
            aliases.add(an);
            if (++n == MAX_EXTRA_ALIASES) break;
        }
        return e;
    }

    private static String q(String s) {
        return "'" + s.replace("'", "''") + "'";
    }
}
