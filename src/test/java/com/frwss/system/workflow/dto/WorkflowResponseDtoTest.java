package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow Response DTO Unit Tests")
class WorkflowResponseDtoTest {
    @Test
    @DisplayName("Should store and return response fields")
    void workflowResponseDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowResponseDto dto = new WorkflowResponseDto();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setWorkflowId(1L); dto.setSourceType("WORKFLOW"); dto.setSourceRecordId("100"); dto.setCurrentState(WorkflowState.VERIFIED);
        dto.setMessage("Done"); dto.setCreatedBy("admin");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(1L, dto.getWorkflowId()); assertEquals("WORKFLOW", dto.getSourceType()); assertEquals("100", dto.getSourceRecordId());
        assertEquals(WorkflowState.VERIFIED, dto.getCurrentState()); assertEquals("Done", dto.getMessage()); assertEquals("admin", dto.getCreatedBy());
    }
}
