//--------------------- Provides Database Access For Workflow Transition Logs -----------------------------------

package com.frwss.system.workflow.repository;

import com.frwss.system.workflow.model.WorkflowTransitionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkflowTransitionLogRepository extends JpaRepository<WorkflowTransitionLog, Long> {

}
