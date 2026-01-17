package com.minicommerceapi.minicommerce.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class SlugUtilTest {
    @Test
    void slugify_shouldReturnEmptyString_whenInputIsNull() {
        assertThat(SlugUtil.slugify(null)).isEqualTo("");
    }

    @Test
    void slugify_shouldReturnEmptyString_whenInputIsEmpty() {
        assertThat(SlugUtil.slugify("")).isEqualTo("");
    }

    @Test
    void slugify_shouldSlugifySimpleString() {
        assertThat(SlugUtil.slugify("Hello World")).isEqualTo("hello-world");
    }

    @Test
    void slugify_shouldRemoveSpecialCharacters() {
        assertThat(SlugUtil.slugify("Hello, World! @2026")).isEqualTo("hello-world-2026");
    }

    @Test
    void slugify_shouldHandleMultipleSpacesAndDashes() {
        assertThat(SlugUtil.slugify("  Hello   World  ")).isEqualTo("hello-world");
        assertThat(SlugUtil.slugify("Hello---World")).isEqualTo("hello-world");
    }

    @Test
    void slugify_shouldRemoveAccentsAndUnicode() {
        assertThat(SlugUtil.slugify("Café déjà vu")).isEqualTo("cafe-deja-vu");
        assertThat(SlugUtil.slugify("İstanbul"))
                .isEqualTo("istanbul");
    }

    @Test
    void slugify_shouldTrimLeadingAndTrailingDashes() {
        assertThat(SlugUtil.slugify("--Hello--World--")).isEqualTo("hello-world");
    }

    @Test
    void slugify_shouldReturnSingleDashForNonAlphanumeric() {
        assertThat(SlugUtil.slugify("!!!")).isEqualTo("");
    }

    @Test
    void slugify_shouldHandleAlreadySlugged() {
        assertThat(SlugUtil.slugify("already-slugged-string")).isEqualTo("already-slugged-string");
    }
}