package com.frwss.system.payment.service;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.payment.dto.PaymentRequestDto;
import com.frwss.system.payment.dto.PaymentResponseDto;
import com.frwss.system.payment.model.PaymentTransaction;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Simulated Payment Gateway Service Unit Tests")
class SimulatedPaymentGatewayServiceTest {

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @InjectMocks
    private SimulatedPaymentGatewayService paymentGatewayService;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // processPayment()
    // =========================================================================

    @Test
    @DisplayName("Should record a SUCCESS transaction with settlement and audit log whenever the simulated payment succeeds")
    void processPayment_whenSimulationSucceeds_recordsSuccessAndAuditsProcessed() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequestDto request = paymentRequest("ACC-001", new BigDecimal("1500.00"), "LKR", "CARD");

        // ---------------------------------------------------------------------
        // Act: the 95% simulated success rate isn't mocked (mocking Math.random()
        // globally is unsafe - it can crash the JVM, since core JDK code also
        // calls into java.lang.Math). Instead we call processPayment enough
        // times to be virtually certain of seeing a SUCCESS outcome, and only
        // assert on the SUCCESS runs.
        // ---------------------------------------------------------------------

        PaymentResponseDto successResponse = null;
        for (int attempt = 0; attempt < 200 && successResponse == null; attempt++) {
            PaymentResponseDto response = paymentGatewayService.processPayment(request);
            if ("SUCCESS".equals(response.getStatus())) {
                successResponse = response;
            }
        }

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(successResponse, "Expected at least one simulated SUCCESS in 200 attempts");
        assertNotNull(successResponse.getTransactionId());
        assertEquals("ACC-001", successResponse.getAccountingRecordId());
        assertNotNull(successResponse.getGatewayReference());

        final String successTransactionId = successResponse.getTransactionId();

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, atLeastOnce()).save(captor.capture());

        PaymentTransaction saved = captor.getAllValues().stream()
                .filter(t -> t.getTransactionId().equals(successTransactionId))
                .findFirst()
                .orElseThrow();

        assertEquals("SUCCESS", saved.getStatus());
        assertEquals("ACC-001", saved.getAccountingRecordId());
        assertEquals(new BigDecimal("1500.00"), saved.getAmount());
        assertEquals("LKR", saved.getCurrency());
        assertEquals("CARD", saved.getPaymentMethod());
        assertEquals("SIMULATED", saved.getGateway());
        assertNotNull(saved.getGatewayReference());
        assertNotNull(saved.getSettledAt());

        verify(auditService, atLeastOnce()).log(
                eq(AuditActionType.PAYMENT_PROCESSED),
                eq("PaymentTransaction"),
                eq(successResponse.getTransactionId()),
                eq("cashier1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should record a FAILED transaction with no settlement and audit log whenever the simulated payment fails")
    void processPayment_whenSimulationFails_recordsFailureAndAuditsFailed() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");
        when(paymentTransactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentRequestDto request = paymentRequest("ACC-002", new BigDecimal("500.00"), null, null);

        // ---------------------------------------------------------------------
        // Act: same reasoning as the SUCCESS case above, but this time looking
        // for a FAILED outcome. With a 95% success rate, the odds of not seeing
        // a single failure across 200 attempts are astronomically small.
        // ---------------------------------------------------------------------

        PaymentResponseDto failureResponse = null;
        for (int attempt = 0; attempt < 200 && failureResponse == null; attempt++) {
            PaymentResponseDto response = paymentGatewayService.processPayment(request);
            if ("FAILED".equals(response.getStatus())) {
                failureResponse = response;
            }
        }

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertNotNull(failureResponse, "Expected at least one simulated FAILURE in 200 attempts");
        assertNull(failureResponse.getTransactionId());

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository, atLeastOnce()).save(captor.capture());

        PaymentTransaction failedTransaction = captor.getAllValues().stream()
                .filter(t -> "FAILED".equals(t.getStatus()))
                .findFirst()
                .orElseThrow();

        assertNull(failedTransaction.getSettledAt());
        // defaults are applied when currency/paymentMethod are not supplied
        assertEquals("LKR", failedTransaction.getCurrency());
        assertEquals("CARD", failedTransaction.getPaymentMethod());

        verify(auditService, atLeastOnce()).log(
                eq(AuditActionType.PAYMENT_FAILED),
                eq("PaymentTransaction"),
                anyString(),
                eq("cashier1"),
                anyString()
        );
    }

    // =========================================================================
    // getPaymentStatus()
    // =========================================================================

    @Test
    @DisplayName("Should return a friendly message for a successful transaction")
    void getPaymentStatus_successfulTransaction_returnsSuccessMessage() {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("TXN-001");
        transaction.setAccountingRecordId("ACC-001");
        transaction.setStatus("SUCCESS");
        transaction.setGatewayReference("SIM-REF-001");

        when(paymentTransactionRepository.findByTransactionId("TXN-001")).thenReturn(Optional.of(transaction));

        PaymentResponseDto response = paymentGatewayService.getPaymentStatus("TXN-001");

        assertEquals("SUCCESS", response.getStatus());
        assertEquals("Payment was successful.", response.getMessage());
        assertEquals("SIM-REF-001", response.getGatewayReference());
    }

    @Test
    @DisplayName("Should return a friendly message for a failed transaction")
    void getPaymentStatus_failedTransaction_returnsFailureMessage() {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("TXN-002");
        transaction.setStatus("FAILED");

        when(paymentTransactionRepository.findByTransactionId("TXN-002")).thenReturn(Optional.of(transaction));

        PaymentResponseDto response = paymentGatewayService.getPaymentStatus("TXN-002");

        assertEquals("Payment failed.", response.getMessage());
    }

    @Test
    @DisplayName("Should throw when the transaction does not exist")
    void getPaymentStatus_transactionMissing_throwsRuntimeException() {

        when(paymentTransactionRepository.findByTransactionId("TXN-999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentGatewayService.getPaymentStatus("TXN-999"));
    }

    // =========================================================================
    // refundPayment()
    // =========================================================================

    @Test
    @DisplayName("Should refund a successful transaction and record the audit log")
    void refundPayment_successfulTransaction_marksRefundedAndAudits() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("supervisor1");

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("TXN-003");
        transaction.setAccountingRecordId("ACC-003");
        transaction.setStatus("SUCCESS");
        transaction.setAmount(new BigDecimal("750.00"));
        transaction.setCurrency("LKR");
        transaction.setGatewayReference("SIM-REF-003");

        when(paymentTransactionRepository.findByTransactionId("TXN-003")).thenReturn(Optional.of(transaction));

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        PaymentResponseDto response = paymentGatewayService.refundPayment("TXN-003");

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        ArgumentCaptor<PaymentTransaction> captor = ArgumentCaptor.forClass(PaymentTransaction.class);
        verify(paymentTransactionRepository).save(captor.capture());
        assertEquals("REFUNDED", captor.getValue().getStatus());

        assertEquals("REFUNDED", response.getStatus());
        assertEquals("TXN-003", response.getTransactionId());
        assertEquals("SIM-REF-003", response.getGatewayReference());

        verify(auditService).log(
                eq(AuditActionType.PAYMENT_REFUNDED),
                eq("PaymentTransaction"),
                eq("TXN-003"),
                eq("supervisor1"),
                anyString()
        );
    }

    @Test
    @DisplayName("Should refuse to refund a transaction that was not successful")
    void refundPayment_notSuccessful_throwsRuntimeException() {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("TXN-004");
        transaction.setStatus("FAILED");

        when(paymentTransactionRepository.findByTransactionId("TXN-004")).thenReturn(Optional.of(transaction));

        assertThrows(RuntimeException.class, () -> paymentGatewayService.refundPayment("TXN-004"));

        verify(paymentTransactionRepository, never()).save(any());
        verifyNoInteractions(auditService);
    }

    @Test
    @DisplayName("Should refuse to refund a transaction with a zero amount")
    void refundPayment_zeroAmount_throwsRuntimeException() {

        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setTransactionId("TXN-005");
        transaction.setStatus("SUCCESS");
        transaction.setAmount(BigDecimal.ZERO);

        when(paymentTransactionRepository.findByTransactionId("TXN-005")).thenReturn(Optional.of(transaction));

        assertThrows(RuntimeException.class, () -> paymentGatewayService.refundPayment("TXN-005"));

        verify(paymentTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when refunding a transaction that does not exist")
    void refundPayment_transactionMissing_throwsRuntimeException() {

        when(paymentTransactionRepository.findByTransactionId("TXN-999")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> paymentGatewayService.refundPayment("TXN-999"));

        verify(paymentTransactionRepository, never()).save(any());
    }

    // =========================================================================
    // isAvailable() / getGatewayName()
    // =========================================================================

    @Test
    @DisplayName("Should always report the simulated gateway as available")
    void isAvailable_alwaysReturnsTrue() {
        assertTrue(paymentGatewayService.isAvailable());
    }

    @Test
    @DisplayName("Should report SIMULATED as the gateway name")
    void getGatewayName_returnsSimulated() {
        assertEquals("SIMULATED", paymentGatewayService.getGatewayName());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PaymentRequestDto paymentRequest(String accountingRecordId, BigDecimal amount,
                                             String currency, String paymentMethod) {
        PaymentRequestDto request = new PaymentRequestDto();
        request.setAccountingRecordId(accountingRecordId);
        request.setAmount(amount);
        request.setCurrency(currency);
        request.setPaymentMethod(paymentMethod);
        return request;
    }
}