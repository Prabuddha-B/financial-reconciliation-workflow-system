package com.frwss.system.workflow.repository;

import com.frwss.system.workflow.model.WorkflowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRecordRepository extends JpaRepository<WorkflowRecord, Long> {

}
