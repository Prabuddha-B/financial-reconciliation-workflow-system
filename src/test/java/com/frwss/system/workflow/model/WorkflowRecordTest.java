package com.frwss.system.workflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow Record Unit Tests")
class WorkflowRecordTest {
    @Test
    @DisplayName("Should store and return workflow record fields")
    void workflowRecord_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowRecord record = new WorkflowRecord(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        record.setWorkflowId(1L); record.setSourceType("RECONCILIATION"); record.setSourceRecordId("1001");
        record.setCurrentState(WorkflowState.REVIEWED); record.setCreatedBy("admin"); record.setCreatedAt(time); record.setUpdatedAt(time);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(1L, record.getWorkflowId()); assertEquals("RECONCILIATION", record.getSourceType());
        assertEquals("1001", record.getSourceRecordId()); assertEquals(WorkflowState.REVIEWED, record.getCurrentState());
        assertEquals("admin", record.getCreatedBy()); assertEquals(time, record.getCreatedAt()); assertEquals(time, record.getUpdatedAt());
    }
}
