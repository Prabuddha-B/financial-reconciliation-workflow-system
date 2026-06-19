package com.frwss.system.reconciliation.repository;

import com.frwss.system.reconciliation.model.ReconciliationResult;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
    Optional<ReconciliationResult> findByAccountingRecordId(String accountingRecordId);
    boolean existsByAccountingRecordId(String accountingRecord);
    List<ReconciliationResult> findByStatus(ReconciliationStatus status);
    List<ReconciliationResult> findAllByOrderByReconciledAtDesc();
    List<ReconciliationResult> findByStatusOrderByReconciledAtDesc(ReconciliationStatus status);
    List<ReconciliationResult> findByStatusOrderByReconciledAtDesc(ReconciliationStatus status, Pageable pageable);
}
