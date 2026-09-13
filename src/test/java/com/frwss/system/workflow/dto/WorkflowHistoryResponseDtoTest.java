package com.frwss.system.workflow.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow History Response DTO Unit Tests")
class WorkflowHistoryResponseDtoTest {
    @Test
    @DisplayName("Should store and return history fields")
    void workflowHistoryResponseDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowHistoryResponseDto dto = new WorkflowHistoryResponseDto(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setFromState("ENTERED"); dto.setToState("REVIEWED"); dto.setPerformedBy("reviewer"); dto.setJustification("Checked"); dto.setTransitionTime(time);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("ENTERED", dto.getFromState()); assertEquals("REVIEWED", dto.getToState()); assertEquals("reviewer", dto.getPerformedBy());
        assertEquals("Checked", dto.getJustification()); assertEquals(time, dto.getTransitionTime());
    }
}
