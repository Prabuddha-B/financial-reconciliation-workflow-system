// -------------------------------------------------------------------------------------------------------
//                      Defines the available business operations supported by the workflow subsystem
// -------------------------------------------------------------------------------------------------------
package com.frwss.system.workflow.service;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import java.util.*;

public interface WorkflowService {

    /** Creates a new workflow record. */
    WorkflowResponseDto createWorkflow(WorkflowRequestDto workflowRequestDto);

    /** Updates the state of an existing workflow. */
    WorkflowResponseDto updateWorkflowState(Long workflowId, WorkflowStateUpdateRequestDto request);

    /** Retrieves the transition history of a workflow. */
    List<WorkflowHistoryResponseDto> getWorkflowHistory(Long workflowId);

    /** Retrieves a workflow using its ID. */
    WorkflowResponseDto getWorkflowById(Long workflowId);

    /** Retrieves all workflow records. */
    List<WorkflowResponseDto> getAllWorkflows();

    /** Retrieves the latest activity of a workflow. */
    WorkflowHistoryResponseDto getLatestWorkflowActivity(Long workflowId);
}
