package com.frwss.system.integration;

import com.frwss.system.dto.IngestionResult;
import com.frwss.system.dto.ReceiptDto;
import com.frwss.system.model.Receipt;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.service.IngestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Ingestion Integration Tests")
class IngestionIntegrationTest {

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private ReceiptRepository receiptRepository;

    // =========================================================================
    // Receipt ingestion flow
    // =========================================================================

    @Test
    @DisplayName("Should persist receipt records through the real ingestion flow")
    void ingestReceiptCsv_validFile_persistsExpectedReceipt() throws Exception {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        long countBefore = receiptRepository.count();

        String csv = "receipt_id,reference_no,payer_name,amount,receipt_date,entered_by\n"
                + "INTEGRATION-R001,INTEGRATION-REF001,Integration Payer,1250.75,2026-07-01, integration-test\n";

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "integration-receipts.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8)
        );

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        IngestionResult preview = ingestionService.processFileForPreview(file, "RECEIPT");
        IngestionService.SaveSummary summary = ingestionService.saveProcessedRecords(
                preview.getRecords(),
                "RECEIPT"
        );

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                1,
                preview.getValidRecords()
        );

        assertEquals(
                1,
                summary.savedCount()
        );

        assertEquals(
                countBefore + 1,
                receiptRepository.count()
        );

        List<Receipt> storedReceipts = receiptRepository.findAll()
                .stream()
                .filter(receipt -> "INTEGRATION-R001".equals(receipt.getReceiptId()))
                .toList();

        assertEquals(
                1,
                storedReceipts.size()
        );

        Receipt storedReceipt = storedReceipts.get(0);

        assertEquals(
                "INTEGRATION-R001",
                storedReceipt.getReceiptId()
        );

        assertEquals(
                "INTEGRATION-REF001",
                storedReceipt.getReferenceNo()
        );

        assertEquals(
                "Integration Payer",
                storedReceipt.getPayerName()
        );

        assertEquals(
                new BigDecimal("1250.75"),
                storedReceipt.getAmount()
        );

        assertNotNull(
                storedReceipt.getReceiptDate()
        );

        assertEquals(
                "integration-test",
                storedReceipt.getEnteredBy()
        );
    }
}
