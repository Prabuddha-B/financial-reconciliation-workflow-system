package com.frwss.system.repository;

import com.frwss.system.model.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;;
import java.util.List;

@Repository
public interface AccountingRecordRepository extends JpaRepository<AccountingRecord, String> {
    @Query("""
           SELECT a FROM AccountingRecord a
           WHERE a.recordId NOT IN (
               SELECT r.accountingRecordId FROM ReconciliationResult r
           )
           ORDER BY a.createdAt DESC
           """)

    List<AccountingRecord> findRecentRecordsWithoutReconciliationResult(Pageable pageable);
}
