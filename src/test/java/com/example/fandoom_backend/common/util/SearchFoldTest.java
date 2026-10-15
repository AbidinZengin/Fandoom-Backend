package com.example.fandoom_backend.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SearchFoldTest {

    @Test
    void dotlessI_becomesI_everythingElseUntouched() {
        assertThat(SearchFold.fold("ışık")).isEqualTo("işik");
        assertThat(SearchFold.fold("IŞIK")).isEqualTo("IŞIK");   // büyük I'yı collation zaten i ile eşler
        assertThat(SearchFold.fold("Interstellar")).isEqualTo("Interstellar");
    }

    @Test
    void sqlSide_wrapsColumnInReplace() {
        assertThat(SearchFold.SQL_OPEN + "m.title" + SearchFold.SQL_CLOSE).isEqualTo("replace(m.title, 'ı', 'i')");
    }
}
