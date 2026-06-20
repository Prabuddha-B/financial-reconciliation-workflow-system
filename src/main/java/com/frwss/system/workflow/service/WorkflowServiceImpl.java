// ------------------------- Implements Workflow Business Logic ------------------

package com.frwss.system.workflow.service;

import com.frwss.system.workflow.dto.WorkflowRequestDto;
import com.frwss.system.workflow.dto.WorkflowResponseDto;
import com.frwss.system.workflow.dto.WorkflowStateUpdateRequestDto;
import com.frwss.system.workflow.exception.WorkflowNotFoundException;
import com.frwss.system.workflow.validation.WorkflowTransitionValidator;
import com.frwss.system.workflow.model.WorkflowRecord;
import com.frwss.system.workflow.model.WorkflowState;
import com.frwss.system.workflow.model.WorkflowTransitionLog;
import com.frwss.system.workflow.repository.WorkflowRecordRepository;
import com.frwss.system.workflow.repository.WorkflowTransitionLogRepository;
import org.springframework.stereotype.Service;
import com.frwss.system.workflow.dto.WorkflowHistoryResponseDto;
import java.util.*;
import com.frwss.system.workflow.exception.InvalidWorkflowTransitionException;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.audit.model.AuditActionType;


import java.time.LocalDateTime;

@Service
public class WorkflowServiceImpl implements WorkflowService {

    private final WorkflowRecordRepository workflowRecordRepository;
    private final WorkflowTransitionLogRepository workflowTransitionLogRepository;

    private final AuditService auditService;

    public WorkflowServiceImpl(WorkflowRecordRepository workflowRecordRepository, WorkflowTransitionLogRepository workflowTransitionLogRepository, AuditService auditService) {
        this.workflowRecordRepository = workflowRecordRepository;
        this.workflowTransitionLogRepository = workflowTransitionLogRepository;
        this.auditService = auditService;
    }

//    Creates a new workflow record
    @Override
    public WorkflowResponseDto createWorkflow(WorkflowRequestDto request) {

        WorkflowRecord workflowRecord = new WorkflowRecord();

        workflowRecord.setSourceType(request.getSourceType());
        workflowRecord.setSourceRecordId(request.getSourceRecordId());
        workflowRecord.setCreatedBy(request.getCreatedBy());
        workflowRecord.setCurrentState(WorkflowState.ENTERED);
        workflowRecord.setCreatedAt(LocalDateTime.now());
        workflowRecord.setUpdatedAt(LocalDateTime.now());

        // Saving to DB
        WorkflowRecord savedRecord = workflowRecordRepository.save(workflowRecord);

        // Prepare response Object
        WorkflowResponseDto response = new WorkflowResponseDto();

        response.setWorkflowId(savedRecord.getWorkflowId());
        response.setSourceType(savedRecord.getSourceType());
        response.setSourceRecordId(savedRecord.getSourceRecordId());
        response.setCurrentState(savedRecord.getCurrentState());
        response.setMessage("Workflow Created Successfully");

        return response;
    }

//    Updates workflow state
    @Override
    public WorkflowResponseDto updateWorkflowState(Long workflowId, WorkflowStateUpdateRequestDto request) {

        WorkflowRecord workflowRecord = workflowRecordRepository.findById(workflowId).orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        WorkflowState oldState = workflowRecord.getCurrentState();

        if(!WorkflowTransitionValidator.isValidTransition(oldState, request.getNewState())){
            throw new InvalidWorkflowTransitionException("Invalid Workflow Transition");
        }

        if (request.getJustification() == null || request.getJustification().trim().isEmpty()) {
            throw new RuntimeException("Justification is required");
        }

        if(workflowRecord.getCreatedBy().equals(request.getPerformedBy())){
            throw new RuntimeException("Creator cannot review or approve their own workflow");
        }

        workflowRecord.setCurrentState(request.getNewState());
        workflowRecord.setUpdatedAt(LocalDateTime.now());

        WorkflowRecord updatedRecord = workflowRecordRepository.save(workflowRecord);

        WorkflowTransitionLog log = new WorkflowTransitionLog();

        log.setWorkflowId(updatedRecord.getWorkflowId());
        log.setFromState(oldState.name());
        log.setToState(updatedRecord.getCurrentState().name());
        log.setPerformedBy(request.getPerformedBy());
        log.setJustification(request.getJustification());
        log.setTransitionTime(LocalDateTime.now());

        workflowTransitionLogRepository.save(log);

        auditService.log(
                AuditActionType.WORKFLOW_STATE_CHANGED,
                "WorkflowRecord",
                updatedRecord.getWorkflowId().toString(),
                request.getPerformedBy(),
                "Workflow moved from " + oldState + " to " + updatedRecord.getCurrentState()
        );

        WorkflowResponseDto response = new WorkflowResponseDto();

        response.setWorkflowId(updatedRecord.getWorkflowId());
        response.setSourceType(updatedRecord.getSourceType());
        response.setSourceRecordId(updatedRecord.getSourceRecordId());
        response.setCurrentState(updatedRecord.getCurrentState());
        response.setMessage("Workflow State Updated Successfully");

        return response;
    }

//    Retrieves Workflow History
    @Override
    public List<WorkflowHistoryResponseDto> getWorkflowHistory(Long workflowId) {

        List<WorkflowTransitionLog> logs = workflowTransitionLogRepository.findByWorkflowId(workflowId);
        List<WorkflowHistoryResponseDto> history = new java.util.ArrayList<>();

        for(WorkflowTransitionLog log : logs){
            WorkflowHistoryResponseDto dto = new WorkflowHistoryResponseDto();
            dto.setFromState(log.getFromState());
            dto.setToState(log.getToState());
            dto.setPerformedBy(log.getPerformedBy());
            dto.setJustification(log.getJustification());
            dto.setTransitionTime(log.getTransitionTime());

            history.add(dto);
        }

        return history;
    }

//    Retrieves a single workflow
    @Override
    public WorkflowResponseDto getWorkflowById(Long workflowId) {

        WorkflowRecord workflowRecord = workflowRecordRepository.findById(workflowId).orElseThrow(() -> new WorkflowNotFoundException("Workflow not found"));

        WorkflowResponseDto response = new WorkflowResponseDto();

        response.setWorkflowId(workflowRecord.getWorkflowId());
        response.setSourceType(workflowRecord.getSourceType());
        response.setSourceRecordId(workflowRecord.getSourceRecordId());
        response.setCurrentState(workflowRecord.getCurrentState());

        return response;
    }

//    Retrieves all workflows
    @Override
    public List<WorkflowResponseDto> getAllWorkflows() {

        List<WorkflowRecord> workflowRecords = workflowRecordRepository.findAll();

        List<WorkflowResponseDto> workflows = new ArrayList<>();

        for (WorkflowRecord workflowRecord : workflowRecords) {

            WorkflowResponseDto dto = new WorkflowResponseDto();

            dto.setWorkflowId(workflowRecord.getWorkflowId());
            dto.setSourceType(workflowRecord.getSourceType());
            dto.setSourceRecordId(workflowRecord.getSourceRecordId());
            dto.setCurrentState(workflowRecord.getCurrentState());

            workflows.add(dto);
        }

        return workflows;
    }
}
