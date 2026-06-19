// --------------------------- Defines Workflow Business Operations ---------------------------------------

package com.frwss.system.workflow.service;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import java.util.*;

public interface WorkflowService {

//    Creates a ne wworkflow Record
    WorkflowResponseDto createWorkflow(WorkflowRequestDto workflowRequestDto);

//    Updates workflow State
    WorkflowResponseDto updateWorkflowState(Long workflowId, WorkflowStateUpdateRequestDto request);

//    Retrieves  workflow Transition History
    List<WorkflowHistoryResponseDto> getWorkflowHistory(Long workflowId);

}
