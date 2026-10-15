package com.example.fandoom_backend.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LikePatternsTest {

    @Test
    void contains_plainText_wrapsWithWildcards() {
        assertThat(LikePatterns.contains("dune")).isEqualTo("%dune%");
    }

    @Test
    void startsWith_plainText_appendsWildcard() {
        assertThat(LikePatterns.startsWith("dune")).isEqualTo("dune%");
    }

    @Test
    void userWildcards_areEscaped_soTheyMatchLiterally() {
        assertThat(LikePatterns.contains("%")).isEqualTo("%!%%");
        assertThat(LikePatterns.contains("50%")).isEqualTo("%50!%%");
        assertThat(LikePatterns.contains("a_b")).isEqualTo("%a!_b%");
    }

    @Test
    void escapeCharItself_isEscaped() {
        assertThat(LikePatterns.contains("wow!")).isEqualTo("%wow!!%");
    }

    @Test
    void caseAndWhitespace_areLeftUntouched() {
        assertThat(LikePatterns.contains(" Dune ")).isEqualTo("% Dune %");
        assertThat(LikePatterns.contains("IŞIK")).isEqualTo("%IŞIK%");
    }

    @Test
    void null_isRejected() {
        assertThatThrownBy(() -> LikePatterns.contains(null)).isInstanceOf(NullPointerException.class);
    }
}
