package com.frwss.system.workflow.dto;

import com.frwss.system.workflow.model.WorkflowState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow Request DTO Unit Tests")
class WorkflowRequestDtoTest {
    @Test
    @DisplayName("Should store and return request fields")
    void workflowRequestDto_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowRequestDto dto = new WorkflowRequestDto();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        dto.setSourceType("RECONCILIATION"); dto.setSourceRecordId("1"); dto.setCreatedBy("admin");
        dto.setComments("Review"); dto.setTargetState(WorkflowState.REVIEWED);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("RECONCILIATION", dto.getSourceType()); assertEquals("1", dto.getSourceRecordId());
        assertEquals("admin", dto.getCreatedBy()); assertEquals("Review", dto.getComments()); assertEquals(WorkflowState.REVIEWED, dto.getTargetState());
    }
}
