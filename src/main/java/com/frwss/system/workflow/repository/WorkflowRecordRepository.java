// ---------------------- Provides Database Access For Workflow Records -------------------------------------

package com.frwss.system.workflow.repository;

import com.frwss.system.workflow.model.WorkflowRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRecordRepository extends JpaRepository<WorkflowRecord, Long> {

    // Find workflow linked to a source record
    Optional<WorkflowRecord> findBySourceTypeAndSourceRecordId(String sourceType, String sourceRecordId
    );

}
