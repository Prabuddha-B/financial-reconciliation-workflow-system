package com.frwss.system.repository;

import com.frwss.system.model.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;;
import java.util.List;
import java.util.Set;

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

    // Find bulk transactions
    @Query("SELECT a FROM AccountingRecord a WHERE a.isBulkTransaction = true AND a.recordId NOT IN (SELECT b.bulkRecordId FROM BulkReconciliation b)")
    List<AccountingRecord> findUnreconciledBulkTransactions();

    // Find children for a bulk transaction
    @Query("SELECT a FROM AccountingRecord a WHERE a.parentRecordId = :parentId")
    List<AccountingRecord> findChildrenByParentId(@Param("parentId") String parentId);

    // Find potential bulk transactions by reference pattern
    @Query("SELECT a FROM AccountingRecord a WHERE a.referenceNo LIKE %:pattern% AND a.isBulkTransaction = false")
    List<AccountingRecord> findPotentialBulkTransactionsByPattern(@Param("pattern") String pattern);

    // Find children by reference prefix
    @Query("SELECT a FROM AccountingRecord a WHERE a.referenceNo LIKE :prefix% AND a.recordId != :bulkId AND (a.parentRecordId IS NULL OR a.parentRecordId = '')")
    List<AccountingRecord> findChildrenByReferencePrefix(@Param("prefix") String prefix, @Param("bulkId") String bulkId);

    // Count records by parent
    @Query("SELECT COUNT(a) FROM AccountingRecord a WHERE a.parentRecordId = :parentId")
    long countChildrenByParentId(@Param("parentId") String parentId);

    @Query("SELECT a.referenceNo FROM AccountingRecord a WHERE a.referenceNo IN :referenceNos")
    Set<String> findExistingReferenceNos(@Param("referenceNos") List<String> referenceNos);
}
