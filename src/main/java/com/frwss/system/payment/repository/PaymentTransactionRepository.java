package com.frwss.system.payment.repository;

import com.frwss.system.payment.model.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>{
    /**
     * Find a transaction by its unique transaction ID
     */
    Optional<PaymentTransaction> findByTransactionId(String transactionId);

    /**
     * Find all transactions for a specific accounting record
     */
    List<PaymentTransaction> findByAccountingRecordId(String accountingRecordId);

    /**
     * Find all transactions with a specific status
     */
    List<PaymentTransaction> findByStatus(String status);

    /**
     * Count transactions by status
     */
    long countByStatus(String status);

    /**
     * Find all successful transactions
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = 'SUCCESS'")
    List<PaymentTransaction> findAllSuccessful();

    /**
     * Find all pending transactions
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.status = 'PENDING'")
    List<PaymentTransaction> findAllPending();

    /**
     * Find transactions by date range
     */
    @Query("SELECT p FROM PaymentTransaction p WHERE p.processedAt BETWEEN :startDate AND :endDate")
    List<PaymentTransaction> findByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * Get total amount processed by a specific gateway
     */
    @Query("SELECT SUM(p.amount) FROM PaymentTransaction p WHERE p.gateway = :gateway AND p.status = 'SUCCESS'")
    Double getTotalAmountByGateway(@Param("gateway") String gateway);

    /**
     * Check if a transaction exists for a specific accounting record
     */
    boolean existsByAccountingRecordIdAndStatus(String accountingRecordId, String status);
}
