package com.frwss.system.repository;

import com.frwss.system.model.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingRecordRepository
        extends JpaRepository<AccountingRecord,String>{
}