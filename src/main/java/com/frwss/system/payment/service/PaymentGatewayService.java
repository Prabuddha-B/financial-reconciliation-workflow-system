package com.frwss.system.payment.service;

import com.frwss.system.payment.dto.PaymentRequestDto;
import com.frwss.system.payment.dto.PaymentResponseDto;

/**
 * Interface for payment gateway implementations
 * Allows swapping between different payment providers
 */

public interface PaymentGatewayService {
    /**
     * Process a payment transaction
     */
    PaymentResponseDto processPayment(PaymentRequestDto request);

    /**
     * Get the status of a payment
     */
    PaymentResponseDto getPaymentStatus(String transactionId);

    /**
     * Refund a payment
     */
    PaymentResponseDto refundPayment(String transactionId);

    /**
     * Check if the gateway is available
     */
    boolean isAvailable();

    /**
     * Get the gateway name
     */
    String getGatewayName();

}
