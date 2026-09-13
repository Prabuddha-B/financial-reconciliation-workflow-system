package com.frwss.system.workflow.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Workflow Transition Log Unit Tests")
class WorkflowTransitionLogTest {
    @Test
    @DisplayName("Should store and return transition log fields")
    void workflowTransitionLog_allFields_gettersReturnAssignedValues() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        WorkflowTransitionLog log = new WorkflowTransitionLog(); LocalDateTime time = LocalDateTime.now();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        log.setLogId(1L); log.setWorkflowId(2L); log.setFromState("ENTERED"); log.setToState("REVIEWED");
        log.setPerformedBy("reviewer"); log.setJustification("Reviewed"); log.setTransitionTime(time);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals(1L, log.getLogId()); assertEquals(2L, log.getWorkflowId()); assertEquals("ENTERED", log.getFromState());
        assertEquals("REVIEWED", log.getToState()); assertEquals("reviewer", log.getPerformedBy());
        assertEquals("Reviewed", log.getJustification()); assertEquals(time, log.getTransitionTime());
    }
}
