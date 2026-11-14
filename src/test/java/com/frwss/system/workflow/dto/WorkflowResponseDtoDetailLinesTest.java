package com.frwss.system.workflow.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Workflow response details lines")
class WorkflowResponseDtoDetailLinesTest {

    @Test
    @DisplayName("Should split details written one fact per line")
    void detailLines_newFormat_splitsOnLineBreaks() {
        WorkflowResponseDto dto = new WorkflowResponseDto();
        dto.setDetails("Accounting record: ACC-1 \u00b7 REF \u00b7 10.00\r\nMatched RECEIPT: RCT-1\nConfidence: 90.0%");

        assertEquals(
                List.of("Accounting record: ACC-1 \u00b7 REF \u00b7 10.00", "Matched RECEIPT: RCT-1", "Confidence: 90.0%"),
                dto.detailLines());
        assertEquals("Accounting record: ACC-1 \u00b7 REF \u00b7 10.00", dto.detailsSummary());
    }

    @Test
    @DisplayName("Should split older single-line details on ' | '")
    void detailLines_oldFormat_splitsOnPipes() {
        WorkflowResponseDto dto = new WorkflowResponseDto();
        dto.setDetails("Accounting record: ACC-0049 | Matched RECEIPT record: RCT-0018 | Confidence: 85.0% | Resolved by: entry");

        assertEquals(4, dto.detailLines().size());
        assertEquals("Accounting record: ACC-0049", dto.detailsSummary());
        assertEquals("Resolved by: entry", dto.detailLines().get(3));
    }

    @Test
    @DisplayName("Should return no lines and an empty summary when there are no details")
    void detailLines_nullOrBlank_isEmpty() {
        WorkflowResponseDto dto = new WorkflowResponseDto();

        assertTrue(dto.detailLines().isEmpty());
        assertEquals("", dto.detailsSummary());

        dto.setDetails("   ");
        assertTrue(dto.detailLines().isEmpty());
    }
}
