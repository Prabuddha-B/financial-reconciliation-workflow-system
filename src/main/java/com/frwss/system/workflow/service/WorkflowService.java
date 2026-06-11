// --------------------------- Defines Workflow Business Operations ---------------------------------------

package com.frwss.system.workflow.service;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;

public interface WorkflowService {

//    Creates a ne wworkflow Record
    WorkflowResponseDto createWorkflow(WorkflowRequestDto workflowRequestDto);
}
