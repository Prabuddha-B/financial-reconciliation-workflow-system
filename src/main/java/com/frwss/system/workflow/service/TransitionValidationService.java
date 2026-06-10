//--------------------------- Handles Workflow Transition Validation Logic -----------------------------------------

package com.frwss.system.workflow.service;

import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.model.WorkflowTransitionLog;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class TransitionValidationService {

//    Store Allowed workflow State transitions
    public static final Map<WorkflowState, Set<WorkflowState>> ALLOWED_TRANSITIONS =
            Map.of(
                    WorkflowState.ENTERED,
                    Set.of(WorkflowState.REVIEWED),

                    WorkflowState.REVIEWED,
                    Set.of(
                            WorkflowState.VERIFIED,
                            WorkflowState.REJECTED
                    ),

                    WorkflowState.VERIFIED,
                    Set.of(),

                    WorkflowState.REJECTED,
                    Set.of()

            );

//    Validation of State Transitions
    public boolean isValidTransition(WorkflowState currentState, WorkflowState targetState) {
        return ALLOWED_TRANSITIONS.getOrDefault(currentState, Set.of()).contains(targetState);
    }
}
