package com.uniquindio.teen_sport.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Porcentajes de los reportes.
 */
class PercentagesTest {

    @Test
    void roundsToOneDecimalHalfUp() {
        assertThat(Percentages.of(2, 3)).isEqualTo(66.7);
        assertThat(Percentages.of(1, 3)).isEqualTo(33.3);
        assertThat(Percentages.of(1, 8)).isEqualTo(12.5);
        assertThat(Percentages.round(0.05)).isEqualTo(0.1);
        assertThat(Percentages.ofOrNull(3, 3)).isEqualTo(100.0);
    }

    @Test
    void withoutBaseTheRequiredValueIsZeroAndTheNullableValueIsNull() {
        assertThat(Percentages.of(0, 0)).isZero();
        assertThat(Percentages.ofOrNull(0, 0)).isNull();
    }
}
