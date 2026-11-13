package com.teamsheet.tools;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

class SeedSqlGeneratorTest {

    @Test
    void committedSeedMatchesJson() throws Exception {
        String expected = SeedSqlGenerator.generate(SeedSqlGenerator.DEFAULT_JSON);
        String committed = Files.readString(SeedSqlGenerator.DEFAULT_OUT, StandardCharsets.UTF_8);
        assertThat(committed)
                .as("V2__seed.sql is stale; rerun SeedSqlGenerator")
                .isEqualToNormalizingNewlines(expected);
    }

    @Test
    void aliasesIncludeSurnameAndAccentFreeForms() {
        assertThat(SeedSqlGenerator.aliases("Vladimír Šmicer")).contains("vladimir smicer", "smicer");
        assertThat(SeedSqlGenerator.aliases("Edwin van der Sar")).contains("van der sar", "vandersar", "sar");
        assertThat(SeedSqlGenerator.shortName("Daniele De Rossi")).isEqualTo("De Rossi");
    }
}
