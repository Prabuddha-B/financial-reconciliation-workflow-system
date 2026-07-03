package com.frwss.system.reconciliation.repository;

import com.frwss.system.reconciliation.model.BulkReconciliation;
import com.frwss.system.reconciliation.model.ReconciliationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BulkReconciliationRepository extends JpaRepository<BulkReconciliation, Long> {

    List<BulkReconciliation> findByStatus(ReconciliationStatus status);

    @Query("SELECT b FROM BulkReconciliation b WHERE b.bulkRecordId = :bulkRecordId")
    List<BulkReconciliation> findByByBulkRecordId(@Param("bulkRecordId") String bulkRecordId);

    @Query("SELECT b FROM BulkReconciliation b ORDER BY b.reconciledAt DESC")
    List<BulkReconciliation> findAllOrderByReconciledAtDesc();

    boolean existsByBulkRecordId(String bulkRecordId);
}
