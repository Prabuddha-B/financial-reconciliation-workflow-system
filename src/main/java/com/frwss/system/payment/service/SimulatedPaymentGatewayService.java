package com.frwss.system.payment.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.payment.dto.PaymentRequestDto;
import com.frwss.system.payment.dto.PaymentResponseDto;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Simulated payment gateway for testing and demonstration
 * Implements the PaymentGatewayService interface
 */

@Service
public class SimulatedPaymentGatewayService implements PaymentGatewayService{

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGatewayService.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    // Simulate 95% success rate
    private static final double SUCCESS_RATE = 0.95;

    public SimulatedPaymentGatewayService(
            PaymentTransactionRepository paymentTransactionRepository,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    @Override
    @Transactional
    public PaymentResponseDto processPayment(PaymentRequestDto request){

        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("💳 Processing payment for record: {} with amount: {}",
                request.getAccountingRecordId(), request.getAmount());

        PaymentResponseDto response = new PaymentResponseDto();
        response.setAccountingRecordId(request.getAccountingRecordId());
        response.setProcessedAt(LocalDateTime.now());

        // Simulate payment processing
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        boolean isSuccess = Math.random() < SUCCESS_RATE;

        // Create payment transaction record
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId(transactionId);
        transaction.setAccountingRecordId(request.getAccountingRecordId());
        transaction.setAmount(request.getAmount());
        transaction.setCurrency(request.getCurrency() != null ? request.getCurrency() : "LKR");
        transaction.setPaymentMethod(request.getPaymentMethod() != null ? request.getPaymentMethod() : "CARD");
        transaction.setGateway("SIMULATED");
        transaction.setGatewayReference("SIM-REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        transaction.setCustomerName(request.getCustomerName());
        transaction.setCustomerEmail(request.getCustomerEmail());
        transaction.setDescription(request.getDescription() != null ? request.getDescription() : "Payment for record " + request.getAccountingRecordId());
        transaction.setProcessedAt(LocalDateTime.now());

        if(isSuccess){
            transaction.setStatus("SUCCESS");
            transaction.setSettledAt(LocalDateTime.now().plusMinutes(30)); // Simulate settlement delay

            response.setTransactionId(transactionId);
            response.setStatus("SUCCESS");
            response.setMessage("Payment processed successfully.");
            response.setGatewayReference(transaction.getGatewayReference());

            log.info("Payment successful. Transaction ID: {}", transactionId);
        } else {
            transaction.setStatus("FAILED");

            response.setTransactionId(null);
            response.setStatus("FAILED");
            response.setMessage("Payment failed due to a simulated error.");

            log.info("Payment failed. Transaction ID: {}", transactionId);

        }

        // Save transaction
        paymentTransactionRepository.save(transaction);

        // Audit log
        auditService.log(
                isSuccess ? AuditActionType.PAYMENT_PROCESSED : AuditActionType.PAYMENT_FAILED,
                "PaymentTransaction",
                transactionId,
                performedBy,
                String.format("Payment %s for record %s. Amount: %s %s",
                        isSuccess ? "processed" : "failed",
                        request.getAccountingRecordId(),
                        request.getAmount(),
                        request.getCurrency() != null ? request.getCurrency() : "LKR")
        );


        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDto getPaymentStatus(String transactionId){
        log.info("🔍 Checking payment status for transaction: {}", transactionId);

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        PaymentResponseDto response = new PaymentResponseDto();
        response.setTransactionId(transaction.getTransactionId());
        response.setAccountingRecordId(transaction.getAccountingRecordId());
        response.setStatus(transaction.getStatus());
        response.setProcessedAt(transaction.getProcessedAt());
        response.setGatewayReference(transaction.getGatewayReference());

        if ("SUCCESS".equals(transaction.getStatus())) {
            response.setMessage("Payment was successful.");
        } else if ("PENDING".equals(transaction.getStatus())) {
            response.setMessage("Payment is pending.");
        } else if ("FAILED".equals(transaction.getStatus())) {
            response.setMessage("Payment failed.");
        } else if ("REFUNDED".equals(transaction.getStatus())) {
            response.setMessage("Payment has been refunded.");
        } else {
            response.setMessage("Unknown payment status.");
        }

        return response;
    }

    @Override
    @Transactional
    public PaymentResponseDto refundPayment(String transactionId){
        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("💳 Processing refund for transaction: {}", transactionId);

        PaymentTransaction transaction = paymentTransactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found: " + transactionId));

        if (!"SUCCESS".equals(transaction.getStatus())) {
            throw new RuntimeException("Only successful transactions can be refunded.");
        }

        if (transaction.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Cannot refund zero or negative amount.");
        }

        // Update transaction status
        transaction.setStatus("REFUNDED");
        paymentTransactionRepository.save(transaction);

        PaymentResponseDto response = new PaymentResponseDto();
        response.setTransactionId(transaction.getTransactionId());
        response.setAccountingRecordId(transaction.getAccountingRecordId());
        response.setStatus("REFUNDED");
        response.setProcessedAt(LocalDateTime.now());
        response.setMessage("Payment refunded successfully.");
        response.setGatewayReference(transaction.getGatewayReference());

        // Audit log
        auditService.log(
                AuditActionType.PAYMENT_REFUNDED,
                "PaymentTransaction",
                transactionId,
                performedBy,
                String.format("Payment refunded for record %s. Amount: %s %s",
                        transaction.getAccountingRecordId(),
                        transaction.getAmount(),
                        transaction.getCurrency())
        );

        return response;
    }

    @Override
    public boolean isAvailable() {
        return true; // Simulated gateway is always available
    }

    @Override
    public String getGatewayName() {
        return "SIMULATED";
    }

}
