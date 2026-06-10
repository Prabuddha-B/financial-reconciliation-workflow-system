package com.frwss.system.repository;

import com.frwss.system.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {
    boolean existsByReferenceNo(String referenceNo);
    Optional<Receipt> findFirstByReferenceNoAndAmountAndReceiptDate(
            String referenceNo,
            BigDecimal amount,
            LocalDate receiptDate

    );
}
