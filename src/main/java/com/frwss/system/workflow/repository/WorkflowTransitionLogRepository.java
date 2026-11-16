// --------------------------------------------------------------------------------------------------------------
//                          Provides Database Operations For Workflow Transition Logs
// --------------------------------------------------------------------------------------------------------------
package com.frwss.system.workflow.repository;

import com.frwss.system.workflow.model.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {

    // Retrieve all transition logs for a workflow
    List<WorkflowTransitionLog> findByWorkflowId(Long workflowId);

    // Retrieve the latest transition log
    WorkflowTransitionLog findTopByWorkflowIdOrderByTransitionTimeDesc(Long workflowId);
}
