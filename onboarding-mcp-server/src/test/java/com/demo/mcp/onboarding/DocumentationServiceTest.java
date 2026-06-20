package com.demo.mcp.onboarding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentationServiceTest {

    @Test
    void slugFromFilename_stripsNumericPrefixAndMdExtension() {
        assertThat(DocumentationService.slugFromFilename("05-high-level-architecture.md"))
                .isEqualTo("high-level-architecture");
        assertThat(DocumentationService.slugFromFilename("01-overview.md")).isEqualTo("overview");
    }

    @Test
    void splitParagraphs_splitsOnBlankLines() {
        String input = "First paragraph.\n\nSecond paragraph.\n\n\nThird.";
        assertThat(DocumentationService.splitParagraphs(input)).containsExactly("First paragraph.", "Second paragraph.", "Third.");
    }
}
