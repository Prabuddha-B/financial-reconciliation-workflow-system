package com.frwss.system.payment.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.payment.dto.PaymentRequestDto;
import com.frwss.system.payment.dto.PaymentResponseDto;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.payment.service.PaymentGatewayService;
import com.frwss.system.payment.service.PaymentReconciliationService;
import com.frwss.system.payment.service.SimulatedPaymentGatewayService;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/payment")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentGatewayService paymentGatewayService;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final PaymentReconciliationService paymentReconciliationService;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;
    private final AccountingRecordRepository accountingRecordRepository;

    public PaymentController(
            SimulatedPaymentGatewayService paymentGatewayService,
            PaymentTransactionRepository paymentTransactionRepository,
            PaymentReconciliationService paymentReconciliationService,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter,
            AccountingRecordRepository accountingRecordRepository) {
        this.paymentGatewayService = paymentGatewayService;
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.paymentReconciliationService = paymentReconciliationService;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
        this.accountingRecordRepository = accountingRecordRepository;
    }

    /**
     * Display the payment dashboard
     */
    @GetMapping("/dashboard")
    public String paymentDashboard(Model model) {
        String performedBy = securityContextAdapter.getCurrentUsername();

        // Get all transactions
        List<PaymentTransaction> transactions = paymentTransactionRepository.findAll();
        model.addAttribute("transactions", transactions);

        // Get reconciliation stats
        Map<String, Object> stats = paymentReconciliationService.getReconciliationStats();
        model.addAttribute("stats", stats);

        // Count recent transactions (last 7 days)
        long recentCount = transactions.stream()
                .filter(t -> t.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(7)))
                .count();
        model.addAttribute("recentCount", recentCount);

        return "payment/dashboard";
    }

    /**
     * API: Process a payment for an accounting record
     */
    @PostMapping("/api/process")
    @ResponseBody
    public ResponseEntity<?> processPayment(@RequestBody PaymentRequestDto request) {
        String performedBy = securityContextAdapter.getCurrentUsername();

        // Reject bad input before anything is charged or written
        String validationError = validatePaymentRequest(request);
        if (validationError != null) {
            log.warn("API: Payment request rejected: {}", validationError);
            return ResponseEntity.badRequest().body(Map.of("error", validationError));
        }

        log.info("API: Processing payment for record: {}", request.getAccountingRecordId());

        try {
            PaymentResponseDto response = paymentGatewayService.processPayment(request);

            auditService.log(
                    "SUCCESS".equals(response.getStatus()) ?
                            AuditActionType.PAYMENT_PROCESSED : AuditActionType.PAYMENT_FAILED,
                    "PaymentAPI",
                    response.getTransactionId() != null ? response.getTransactionId() : "N/A",
                    performedBy,
                    String.format("API payment request for record %s: %s",
                            request.getAccountingRecordId(), response.getStatus())
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Payment failed: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Get payment status
     */
    @GetMapping("/api/status/{transactionId}")
    @ResponseBody
    public ResponseEntity<?> getPaymentStatus(@PathVariable String transactionId) {
        try {
            PaymentResponseDto response = paymentGatewayService.getPaymentStatus(transactionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * API: Refund a payment
     */
    @PostMapping("/api/refund/{transactionId}")
    @ResponseBody
    public ResponseEntity<?> refundPayment(@PathVariable String transactionId) {
        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("API: Refunding payment: {}", transactionId);

        try {
            PaymentResponseDto response = paymentGatewayService.refundPayment(transactionId);

            auditService.log(
                    AuditActionType.PAYMENT_REFUNDED,
                    "PaymentAPI",
                    transactionId,
                    performedBy,
                    "Payment refunded via API"
            );

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error refunding payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Refund failed: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Run payment reconciliation
     */
    @PostMapping("/api/reconcile")
    @ResponseBody
    public ResponseEntity<?> reconcilePayments() {
        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("API: Running payment reconciliation");

        try {
            Map<String, Object> results = paymentReconciliationService.reconcileAllPayments();
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            log.error("Error reconciling payments: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Reconciliation failed: " + e.getMessage()
            ));
        }
    }

    /**
     * API: Get reconciliation statistics
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> getReconciliationStats() {
        try {
            Map<String, Object> stats = paymentReconciliationService.getReconciliationStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Checks a payment request before it is processed.
     *
     * @return an error message, or null when the request is acceptable
     */
    private String validatePaymentRequest(PaymentRequestDto request) {
        if (request == null) {
            return "Payment request is required";
        }

        String recordId = request.getAccountingRecordId();
        if (recordId == null || recordId.isBlank()) {
            return "Accounting record ID is required";
        }

        BigDecimal amount = request.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Amount must be greater than zero";
        }

        if (!accountingRecordRepository.existsById(recordId)) {
            return "Accounting record not found: " + recordId;
        }

        // A record that is already paid must not be paid again (a refunded payment
        // no longer counts, so it can be paid again after a refund)
        if (paymentTransactionRepository.existsByAccountingRecordIdAndStatus(recordId, "SUCCESS")) {
            return "A successful payment already exists for record " + recordId;
        }

        return null;
    }
}