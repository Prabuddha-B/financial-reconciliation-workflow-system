package com.frwss.system.payment.controller;

import com.frwss.system.audit.model.AuditActionType;
import com.frwss.system.audit.service.AuditService;
import com.frwss.system.payment.dto.PaymentRequestDto;
import com.frwss.system.payment.dto.PaymentResponseDto;
import com.frwss.system.payment.repository.PaymentTransactionRepository;
import com.frwss.system.payment.service.PaymentReconciliationService;
import com.frwss.system.payment.service.SimulatedPaymentGatewayService;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.security.SecurityContextAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Payment Controller Unit Tests")
class PaymentControllerTest {

    @Mock
    private SimulatedPaymentGatewayService paymentGatewayService;

    @Mock
    private PaymentTransactionRepository paymentTransactionRepository;

    @Mock
    private PaymentReconciliationService paymentReconciliationService;

    @Mock
    private AuditService auditService;

    @Mock
    private SecurityContextAdapter securityContextAdapter;

    @Mock
    private AccountingRecordRepository accountingRecordRepository;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        // Mockito injects mocked dependencies.
    }

    // =========================================================================
    // processPayment() - input validation
    // =========================================================================

    @Test
    @DisplayName("Should reject a payment request with no accounting record ID")
    void processPayment_missingRecordId_returnsBadRequest() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");

        ResponseEntity<?> response = paymentController.processPayment(request(null, "100.00"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Accounting record ID is required", errorOf(response));
        verifyNoInteractions(paymentGatewayService);
    }

    @Test
    @DisplayName("Should reject a payment request with no amount")
    void processPayment_missingAmount_returnsBadRequest() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");

        ResponseEntity<?> response = paymentController.processPayment(request("ACC-001", null));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Amount must be greater than zero", errorOf(response));
        verifyNoInteractions(paymentGatewayService);
    }

    @Test
    @DisplayName("Should reject a payment request with a zero amount")
    void processPayment_zeroAmount_returnsBadRequest() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");

        ResponseEntity<?> response = paymentController.processPayment(request("ACC-001", "0"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Amount must be greater than zero", errorOf(response));
        verifyNoInteractions(paymentGatewayService);
    }

    @Test
    @DisplayName("Should reject a payment request with a negative amount")
    void processPayment_negativeAmount_returnsBadRequest() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");

        ResponseEntity<?> response = paymentController.processPayment(request("ACC-001", "-50.00"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Amount must be greater than zero", errorOf(response));
        verifyNoInteractions(paymentGatewayService);
    }

    @Test
    @DisplayName("Should reject a payment for an accounting record that does not exist")
    void processPayment_unknownRecord_returnsBadRequest() {

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");
        when(accountingRecordRepository.existsById("ACC-9999")).thenReturn(false);

        ResponseEntity<?> response = paymentController.processPayment(request("ACC-9999", "500.00"));

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Accounting record not found: ACC-9999", errorOf(response));
        verifyNoInteractions(paymentGatewayService);
        verifyNoInteractions(auditService);
    }

    // =========================================================================
    // processPayment() - valid request
    // =========================================================================

    @Test
    @DisplayName("Should process a valid payment and write the audit entry")
    void processPayment_validRequest_processesAndAudits() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(securityContextAdapter.getCurrentUsername()).thenReturn("cashier1");
        when(accountingRecordRepository.existsById("ACC-0008")).thenReturn(true);

        PaymentResponseDto gatewayResponse = new PaymentResponseDto();
        gatewayResponse.setStatus("SUCCESS");
        gatewayResponse.setTransactionId("TXN-001");

        PaymentRequestDto request = request("ACC-0008", "114893.74");
        when(paymentGatewayService.processPayment(request)).thenReturn(gatewayResponse);

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        ResponseEntity<?> response = paymentController.processPayment(request);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(200, response.getStatusCode().value());
        assertSame(gatewayResponse, response.getBody());

        verify(auditService).log(
                eq(AuditActionType.PAYMENT_PROCESSED),
                eq("PaymentAPI"),
                eq("TXN-001"),
                eq("cashier1"),
                anyString()
        );
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PaymentRequestDto request(String recordId, String amount) {
        PaymentRequestDto dto = new PaymentRequestDto();
        dto.setAccountingRecordId(recordId);
        dto.setAmount(amount == null ? null : new BigDecimal(amount));
        return dto;
    }

    private String errorOf(ResponseEntity<?> response) {
        return (String) ((Map<?, ?>) response.getBody()).get("error");
    }
}