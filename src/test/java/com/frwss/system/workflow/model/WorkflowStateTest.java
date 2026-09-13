package com.frwss.system.workflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Workflow State Unit Tests")
class WorkflowStateTest {
    @Test
    @DisplayName("Should expose all workflow states")
    void workflowState_values_returnsAllStates() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        WorkflowState[] states = WorkflowState.values();

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertArrayEquals(new WorkflowState[]{WorkflowState.ENTERED, WorkflowState.REVIEWED, WorkflowState.VERIFIED, WorkflowState.REJECTED}, states);
        assertEquals(WorkflowState.REVIEWED, WorkflowState.valueOf("REVIEWED"));
    }
}
