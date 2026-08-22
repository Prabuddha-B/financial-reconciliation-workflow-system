package com.frwss.system.payment.webhook;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * Webhook controller to simulate receiving payment notifications from payment gateways
 * In a real system, this would be an endpoint exposed to the payment gateway
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final PaymentTransactionRepository paymentTransactionRepository;
    private final AuditService auditService;
    private final SecurityContextAdapter securityContextAdapter;

    public WebhookController(
            PaymentTransactionRepository paymentTransactionRepository,
            AuditService auditService,
            SecurityContextAdapter securityContextAdapter) {
        this.paymentTransactionRepository = paymentTransactionRepository;
        this.auditService = auditService;
        this.securityContextAdapter = securityContextAdapter;
    }

    /**
     * Simulate receiving a webhook from a payment gateway
     * Example payload:
     * {
     *   "event": "payment.success",
     *   "transactionId": "TXN-ABC123",
     *   "status": "SUCCESS",
     *   "amount": 1000.00,
     *   "currency": "LKR",
     *   "gatewayReference": "REF-12345"
     * }
     */
    @PostMapping("/payment")
    public ResponseEntity<?> handlePaymentWebhook(@RequestBody Map<String, Object> payload) {
        String performedBy = securityContextAdapter.getCurrentUsername();
        log.info("🔔 Webhook received: {}", payload);

        try {
            String event = (String) payload.get("event");
            String transactionId = (String) payload.get("transactionId");
            String status = (String) payload.get("status");

            if (transactionId == null) {
                log.warn("Webhook received without transactionId");
                return ResponseEntity.badRequest().body("Missing transactionId");
            }

            Optional<PaymentTransaction> optionalTransaction =
                    paymentTransactionRepository.findByTransactionId(transactionId);

            if (optionalTransaction.isEmpty()) {
                log.warn("Transaction {} not found for webhook", transactionId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Transaction not found: " + transactionId);
            }

            PaymentTransaction transaction = optionalTransaction.get();
            String oldStatus = transaction.getStatus();

            // Update transaction status based on webhook
            if ("payment.success".equals(event) || "PAID".equals(status) || "SUCCESS".equals(status)) {
                transaction.setStatus("SUCCESS");
                transaction.setSettledAt(LocalDateTime.now());
                transaction.setPaymentWebhookReceivedAt(LocalDateTime.now());
            } else if ("payment.failed".equals(event) || "FAILED".equals(status)) {
                transaction.setStatus("FAILED");
                transaction.setPaymentWebhookReceivedAt(LocalDateTime.now());
            } else if ("payment.refunded".equals(event) || "REFUNDED".equals(status)) {
                transaction.setStatus("REFUNDED");
                transaction.setPaymentWebhookReceivedAt(LocalDateTime.now());
            } else {
                log.warn("Unknown webhook event: {}", event);
                return ResponseEntity.badRequest().body("Unknown event: " + event);
            }

            paymentTransactionRepository.save(transaction);

            // Audit log
            auditService.log(
                    AuditActionType.PAYMENT_WEBHOOK_RECEIVED,
                    "PaymentTransaction",
                    transactionId,
                    performedBy,
                    String.format("Webhook received for transaction %s. Status changed from %s to %s",
                            transactionId, oldStatus, transaction.getStatus())
            );

            log.info("Webhook processed for transaction {}", transactionId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Webhook processed successfully",
                    "transactionId", transactionId,
                    "newStatus", transaction.getStatus()
            ));

        } catch (Exception e) {
            log.error("Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Endpoint to test webhook functionality with a sample payload
     */
    @PostMapping("/test")
    public ResponseEntity<?> testWebhook() {
        Map<String, Object> testPayload = Map.of(
                "event", "payment.success",
                "transactionId", "TXN-TEST-001",
                "status", "SUCCESS",
                "amount", 1500.00,
                "currency", "LKR",
                "gatewayReference", "REF-TEST-12345",
                "timestamp", LocalDateTime.now().toString()
        );

        log.info("🧪 Test webhook payload: {}", testPayload);
        return ResponseEntity.ok(testPayload);
    }
}