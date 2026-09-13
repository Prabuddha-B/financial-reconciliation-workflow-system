package com.frwss.system.controller;

import com.frwss.system.dto.ManualEntryForm;
import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.repository.AccountingRecordRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Manual Entry Controller Unit Tests")
class ManualEntryControllerTest {

    @Mock
    private ReceiptRepository receiptRepository;

    @Mock
    private PayrollRepository payrollRepository;

    @Mock
    private StockPurchaseRepository stockPurchaseRepository;

    @Mock
    private AccountingRecordRepository accountingRecordRepository;

    @InjectMocks
    private ManualEntryController controller;

    // =========================================================================
    // manualHome()
    // =========================================================================

    @Test
    @DisplayName("Should display the manual entry page")
    void manualHome_validRequest_returnsManualEntryView() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = controller.manualHome("RECEIPT", "ADD", model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "ingestion/manual-entry",
                result
        );
        assertEquals(
                "RECEIPT",
                model.getAttribute("dataType")
        );
        assertNotNull(
                model.getAttribute("record")
        );
    }

    // =========================================================================
    // searchForUpdate()
    // =========================================================================

    @Test
    @DisplayName("Should find an existing receipt")
    void searchForUpdate_existingReceipt_returnsSuccess() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Receipt receipt = new Receipt();
        receipt.setReceiptId("R001");
        when(receiptRepository.findById("R001")).thenReturn(Optional.of(receipt));
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = controller.searchForUpdate("RECEIPT", "R001", null, null, null, null, null, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "ingestion/manual-entry",
                result
        );
        assertEquals(
                true,
                model.getAttribute("updateReady")
        );
        assertNotNull(
                model.getAttribute("record")
        );
    }

    @Test
    @DisplayName("Should report a missing receipt")
    void searchForUpdate_missingReceipt_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(receiptRepository.findById("R001")).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = controller.searchForUpdate("RECEIPT", "R001", null, null, null, null, null, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "ingestion/manual-entry",
                result
        );
        assertEquals(
                "No record found for the given keys.",
                model.getAttribute("errorMessage")
        );
    }

    @Test
    @DisplayName("Should report a missing search key")
    void searchForUpdate_missingKey_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.searchForUpdate("RECEIPT", " ", null, null, null, null, null, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Receipt ID is required to search receipts.",
                model.getAttribute("errorMessage")
        );
        verify(receiptRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should find an existing payroll during update search")
    void searchForUpdate_existingPayroll_returnsSuccess() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        Payroll payroll = new Payroll();
        payroll.setPayrollId("P001");
        when(payrollRepository.findById("P001")).thenReturn(Optional.of(payroll));
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        String result = controller.searchForUpdate("PAYROLL", null, null, "P001", null, null, null, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("ingestion/manual-entry", result);
        assertEquals(true, model.getAttribute("updateReady"));
        verify(payrollRepository).findById("P001");
    }

    @Test
    @DisplayName("Should find an existing stock purchase during update search")
    void searchForUpdate_existingStockPurchase_returnsSuccess() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        StockPurchase purchase = new StockPurchase();
        purchase.setPurchaseId("S001");
        when(stockPurchaseRepository.findById("S001")).thenReturn(Optional.of(purchase));
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        String result = controller.searchForUpdate("STOCK", null, null, null, null, "S001", null, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("ingestion/manual-entry", result);
        assertEquals(true, model.getAttribute("updateReady"));
        verify(stockPurchaseRepository).findById("S001");
    }

    @Test
    @DisplayName("Should find an existing accounting record during update search")
    void searchForUpdate_existingAccountingRecord_returnsSuccess() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        AccountingRecord record = new AccountingRecord();
        record.setRecordId("A001");
        when(accountingRecordRepository.findById("A001")).thenReturn(Optional.of(record));
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        String result = controller.searchForUpdate("ACCOUNTING", null, null, null, null, null, "A001", model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("ingestion/manual-entry", result);
        assertEquals(true, model.getAttribute("updateReady"));
        verify(accountingRecordRepository).findById("A001");
    }

    // =========================================================================
    // addNew()
    // =========================================================================

    @Test
    @DisplayName("Should add a valid receipt")
    void addNew_validReceipt_savesReceipt() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = receiptForm();
        Receipt receipt = new Receipt();
        receipt.setReceiptId("R001");
        when(receiptRepository.existsById("R001")).thenReturn(false);
        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        String result = controller.addNew("RECEIPT", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "ingestion/manual-form",
                result
        );
        assertEquals(
                "Saved successfully.",
                model.getAttribute("successMessage")
        );
        verify(receiptRepository).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should reject a duplicate receipt")
    void addNew_duplicateReceipt_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(receiptRepository.existsById("R001")).thenReturn(true);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("RECEIPT", receiptForm(), model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Duplicate primary key. A record with this ID already exists.",
                model.getAttribute("errorMessage")
        );
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should reject an invalid receipt")
    void addNew_missingRequiredKey_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = receiptForm();
        form.setReceiptId(null);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("RECEIPT", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Receipt ID is required.",
                model.getAttribute("errorMessage")
        );
        verify(receiptRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Should reject an unsupported data type")
    void addNew_unsupportedDataType_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("UNKNOWN", new ManualEntryForm(), model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Invalid data type: UNKNOWN",
                model.getAttribute("errorMessage")
        );
    }

    @Test
    @DisplayName("Should reject payroll with a missing employee ID")
    void addNew_missingPayrollEmployeeId_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = new ManualEntryForm();
        form.setPayrollId("P001");
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("PAYROLL", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Employee ID is required.",
                model.getAttribute("errorMessage")
        );
        verify(payrollRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Should reject stock purchase with a missing purchase ID")
    void addNew_missingStockPurchaseId_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("STOCK", new ManualEntryForm(), model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Purchase ID is required.",
                model.getAttribute("errorMessage")
        );
        verify(stockPurchaseRepository, never()).existsById(any());
    }

    @Test
    @DisplayName("Should reject accounting records with a missing reference number")
    void addNew_missingAccountingReference_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = new ManualEntryForm();
        form.setRecordId("A001");
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("ACCOUNTING", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Reference No is required.",
                model.getAttribute("errorMessage")
        );
        verify(accountingRecordRepository, never()).existsById(any());
    }

    // =========================================================================
    // updateExisting()
    // =========================================================================

    @Test
    @DisplayName("Should update a valid receipt")
    void updateExisting_validReceipt_updatesReceipt() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Receipt receipt = new Receipt();
        receipt.setReceiptId("R001");
        ManualEntryForm form = receiptForm();
        form.setReferenceNo("REF002");
        when(receiptRepository.findById("R001")).thenReturn(Optional.of(receipt));
        when(receiptRepository.save(any(Receipt.class))).thenReturn(receipt);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.updateExisting("RECEIPT", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Updated successfully.",
                model.getAttribute("successMessage")
        );
        assertEquals(
                "REF002",
                receipt.getReferenceNo()
        );
        verify(receiptRepository).save(receipt);
    }

    @Test
    @DisplayName("Should report a record that cannot be found during update")
    void updateExisting_recordNotFound_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        when(receiptRepository.findById("R001")).thenReturn(Optional.empty());
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.updateExisting("RECEIPT", receiptForm(), model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Record not found (cannot update).",
                model.getAttribute("errorMessage")
        );
        verify(receiptRepository, never()).save(any(Receipt.class));
    }

    @Test
    @DisplayName("Should reject an invalid update")
    void updateExisting_missingKey_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = receiptForm();
        form.setReceiptId(" ");
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.updateExisting("RECEIPT", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Record not found (cannot update).",
                model.getAttribute("errorMessage")
        );
        verify(receiptRepository, never()).findById(any());
    }

    @Test
    @DisplayName("Should reject an unsupported update data type")
    void updateExisting_unsupportedDataType_returnsError() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.updateExisting("UNKNOWN", new ManualEntryForm(), model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Invalid data type: UNKNOWN",
                model.getAttribute("errorMessage")
        );
    }

    @Test
    @DisplayName("Should update a payroll")
    void updateExisting_validPayroll_updatesPayroll() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        Payroll payroll = new Payroll();
        payroll.setPayrollId("P001");
        ManualEntryForm form = new ManualEntryForm();
        form.setPayrollId("P001");
        form.setEmployeeId("E002");
        form.setEmployeeName("Updated Employee");
        when(payrollRepository.findById("P001")).thenReturn(Optional.of(payroll));
        when(payrollRepository.save(any(Payroll.class))).thenReturn(payroll);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        controller.updateExisting("PAYROLL", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("Updated successfully.", model.getAttribute("successMessage"));
        assertEquals("Updated Employee", payroll.getEmployeeName());
        verify(payrollRepository).save(payroll);
    }

    @Test
    @DisplayName("Should update a stock purchase")
    void updateExisting_validStockPurchase_updatesStockPurchase() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        StockPurchase purchase = new StockPurchase();
        purchase.setPurchaseId("S001");
        ManualEntryForm form = new ManualEntryForm();
        form.setPurchaseId("S001");
        form.setVendorName("Updated Vendor");
        when(stockPurchaseRepository.findById("S001")).thenReturn(Optional.of(purchase));
        when(stockPurchaseRepository.save(any(StockPurchase.class))).thenReturn(purchase);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        controller.updateExisting("STOCK", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("Updated successfully.", model.getAttribute("successMessage"));
        assertEquals("Updated Vendor", purchase.getVendorName());
        verify(stockPurchaseRepository).save(purchase);
    }

    @Test
    @DisplayName("Should update an accounting record")
    void updateExisting_validAccountingRecord_updatesAccountingRecord() {
        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------
        AccountingRecord record = new AccountingRecord();
        record.setRecordId("A001");
        ManualEntryForm form = new ManualEntryForm();
        form.setRecordId("A001");
        form.setReferenceNo("REF002");
        when(accountingRecordRepository.findById("A001")).thenReturn(Optional.of(record));
        when(accountingRecordRepository.save(any(AccountingRecord.class))).thenReturn(record);
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------
        controller.updateExisting("ACCOUNTING", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------
        assertEquals("Updated successfully.", model.getAttribute("successMessage"));
        assertEquals("REF002", record.getReferenceNo());
        verify(accountingRecordRepository).save(record);
    }

    // =========================================================================
    // Data type branches
    // =========================================================================

    @Test
    @DisplayName("Should add payroll records")
    void addNew_validPayroll_savesPayroll() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = new ManualEntryForm();
        form.setPayrollId("P001");
        form.setEmployeeId("E001");
        when(payrollRepository.existsById("P001")).thenReturn(false);
        when(payrollRepository.save(any(Payroll.class))).thenReturn(new Payroll());
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("PAYROLL", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Saved successfully.",
                model.getAttribute("successMessage")
        );
        verify(payrollRepository).save(any(Payroll.class));
    }

    @Test
    @DisplayName("Should add stock purchases")
    void addNew_validStockPurchase_savesStockPurchase() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = new ManualEntryForm();
        form.setPurchaseId("S001");
        when(stockPurchaseRepository.existsById("S001")).thenReturn(false);
        when(stockPurchaseRepository.save(any(StockPurchase.class))).thenReturn(new StockPurchase());
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("STOCK", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Saved successfully.",
                model.getAttribute("successMessage")
        );
        verify(stockPurchaseRepository).save(any(StockPurchase.class));
    }

    @Test
    @DisplayName("Should add accounting records")
    void addNew_validAccountingRecord_savesAccountingRecord() {

        // ---------------------------------------------------------------------
        // Arrange
        // ---------------------------------------------------------------------

        ManualEntryForm form = new ManualEntryForm();
        form.setRecordId("A001");
        form.setReferenceNo("REF001");
        when(accountingRecordRepository.existsById("A001")).thenReturn(false);
        when(accountingRecordRepository.save(any(AccountingRecord.class))).thenReturn(new AccountingRecord());
        Model model = new ConcurrentModel();

        // ---------------------------------------------------------------------
        // Act
        // ---------------------------------------------------------------------

        controller.addNew("ACCOUNTING", form, model);

        // ---------------------------------------------------------------------
        // Assert
        // ---------------------------------------------------------------------

        assertEquals(
                "Saved successfully.",
                model.getAttribute("successMessage")
        );
        verify(accountingRecordRepository).save(any(AccountingRecord.class));
    }

    private ManualEntryForm receiptForm() {

        ManualEntryForm form = new ManualEntryForm();
        form.setReceiptId("R001");
        form.setReferenceNo("REF001");
        form.setPayerName("Test Payer");
        form.setAmount(BigDecimal.valueOf(1000));
        form.setReceiptDate(LocalDate.of(2026, 1, 1));
        form.setEnteredBy("tester");

        return form;
    }
}
