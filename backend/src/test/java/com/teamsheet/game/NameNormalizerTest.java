package com.teamsheet.game;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NameNormalizerTest {

    @Test
    void stripsAccentsCaseAndPunctuation() {
        assertThat(NameNormalizer.normalize("Vladimír Šmicer")).isEqualTo("vladimir smicer");
        assertThat(NameNormalizer.normalize("  HÖWEDES ")).isEqualTo("howedes");
        assertThat(NameNormalizer.normalize("Samuel Eto'o")).isEqualTo("samuel etoo");
        assertThat(NameNormalizer.normalize("Fàbregas")).isEqualTo("fabregas");
        assertThat(NameNormalizer.normalize("Jean-Pierre   Papin")).isEqualTo("jean pierre papin");
        assertThat(NameNormalizer.normalize("Ødegaard")).isEqualTo("odegaard");
        assertThat(NameNormalizer.normalize("Jóhann Berg Guðmundsson")).isEqualTo("johann berg gudmundsson");
        assertThat(NameNormalizer.normalize("Kolbeinn Sigþórsson")).isEqualTo("kolbeinn sigthorsson");
        assertThat(NameNormalizer.normalize("İlkay Gündoğan")).isEqualTo("ilkay gundogan");
        assertThat(NameNormalizer.normalize("Łukasz Piszczek")).isEqualTo("lukasz piszczek");
        assertThat(NameNormalizer.normalize(null)).isEmpty();
    }
}
