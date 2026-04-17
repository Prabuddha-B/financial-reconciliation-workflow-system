package com.peradeniya.frwss.repository;

import com.peradeniya.frwss.model.AccountingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountingRecordRepository extends JpaRepository<AccountingRecord, String> {
}