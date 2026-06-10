package com.frwss.system.repository;

import com.frwss.system.model.StockPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface StockPurchaseRepository extends JpaRepository<StockPurchase, String> {
    boolean existsByInvoiceNo(String invoiceNo);

    Optional<StockPurchase> findFirstByInvoiceNoAndAmountAndPurchaseDate(
            String invoiceNo,
            BigDecimal amount,
            LocalDate purchaseDate
    );
}
