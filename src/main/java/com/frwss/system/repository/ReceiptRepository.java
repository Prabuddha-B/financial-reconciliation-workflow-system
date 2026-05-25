package com.frwss.system.repository;

import com.frwss.system.model.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, String> {
    boolean existsByReferenceNo(String referenceNo);
    Optional<Receipt> findByReferenceNo(String referenceNo);
}
