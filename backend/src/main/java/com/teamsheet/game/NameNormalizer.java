package com.teamsheet.game;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Single normalization used for both stored aliases (seed generator) and user input.
 * "Vladimír Šmicer" -> "vladimir smicer", "Eto'o" -> "etoo", "Höwedes" -> "howedes".
 */
public final class NameNormalizer {

    private NameNormalizer() {}

    public static String normalize(String raw) {
        if (raw == null) return "";
        String s = raw.toLowerCase(Locale.ROOT);
        // letters NFD does not decompose
        s = s.replace("ø", "o").replace("ß", "ss").replace("ł", "l").replace("đ", "d")
             .replace("æ", "ae").replace("œ", "oe").replace("ı", "i").replace("þ", "th").replace("ð", "d");
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        s = s.replaceAll("['’`´]", "");          // apostrophes vanish: eto'o -> etoo
        s = s.replaceAll("[^a-z0-9]+", " ");      // hyphens, dots, etc. become spaces
        return s.trim().replaceAll(" +", " ");
    }
}
