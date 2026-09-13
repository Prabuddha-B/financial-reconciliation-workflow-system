package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow State Update Request DTO Unit Tests")
class WorkflowStateUpdateRequestDtoTest {
    @Test
    @DisplayName("Should store and return state update fields")
    void workflowStateUpdateRequestDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowStateUpdateRequestDto dto = new WorkflowStateUpdateRequestDto();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setNewState(WorkflowState.REJECTED); dto.setPerformedBy("auditor"); dto.setJustification("Rejected");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(WorkflowState.REJECTED, dto.getNewState()); assertEquals("auditor", dto.getPerformedBy()); assertEquals("Rejected", dto.getJustification());
    }
}
