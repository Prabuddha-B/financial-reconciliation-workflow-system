package com.frwss.system.repository;

import com.frwss.system.model.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingRecordRepository extends JpaRepository<AccountingRecord, String> {
}