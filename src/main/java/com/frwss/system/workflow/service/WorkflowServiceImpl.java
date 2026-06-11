// ------------------------- Implements Workflow Business Logic ------------------

package com.frwss.system.workflow.service;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.model.WorkflowRecord;
import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import com.frwss.system.workflow.repository.WorkflowTransitionLogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class WorkflowServiceImpl implements WorkflowService{
    private final WorkflowRecordRepository workflowRecordRepository;
    private final WorkflowTransitionLogRepository workflowTransitionLogRepository;

    public WorkflowServiceImpl(WorkflowRecordRepository workflowRecordRepository, WorkflowTransitionLogRepository workflowTransitionLogRepository) {
        this.workflowRecordRepository = workflowRecordRepository;
        this.workflowTransitionLogRepository = workflowTransitionLogRepository;
    }

// Creates a new workflow record
    @Override
    public WorkflowResponseDto createWorkflow(WorkflowRequestDto request) {

        WorkflowRecord workflowRecord = new WorkflowRecord();

        workflowRecord.setSourceType(request.getSourceType());
        workflowRecord.setSourceRecordId(request.getSourceRecordId());
        workflowRecord.setCreatedBy(request.getCreatedBy());
        workflowRecord.setCurrentState(WorkflowState.ENTERED);
        workflowRecord.setCreatedAt(LocalDateTime.now());
        workflowRecord.setUpdatedAt(LocalDateTime.now());

//        Saving to DB
        WorkflowRecord savedRecord = workflowRecordRepository.save(workflowRecord);

//        Prepare response Object
        WorkflowResponseDto response = new WorkflowResponseDto();

        response.setWorkflowId(savedRecord.getWorkflowId());
        response.setSourceType(savedRecord.getSourceType());
        response.setSourceRecordId(savedRecord.getSourceRecordId());
        response.setCurrentState(savedRecord.getCurrentState());
        response.setMessage("Workflow Created Successfully");

        return response;
    }
}
