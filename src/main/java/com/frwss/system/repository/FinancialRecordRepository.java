package com.frwss.system.repository;

import com.frwss.system.model.FinancialRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {
    // This interface now inherits methods like .save(), .findAll(), and .findById()
    // Helps avoid duplicates
    boolean existsByReferenceIdAndAmountAndDate(String referenceId, Double amount, LocalDate date);
}