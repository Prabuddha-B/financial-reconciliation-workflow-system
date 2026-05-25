package com.frwss.system.controller;

import com.frwss.system.dto.ManualEntryForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.frwss.system.model.Receipt;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.StockPurchase;
import com.frwss.system.model.AccountingRecord;

import com.frwss.system.repository.ReceiptRepository;
import com.frwss.system.repository.PayrollRepository;
import com.frwss.system.repository.StockPurchaseRepository;
import com.frwss.system.repository.AccountingRecordRepository;


@Controller
@RequestMapping("/ingestion")
public class ManualEntryController {

    @Autowired private ReceiptRepository receiptRepository;
    @Autowired private PayrollRepository payrollRepository;
    @Autowired private StockPurchaseRepository stockPurchaseRepository;
    @Autowired private AccountingRecordRepository accountingRecordRepository;

    @GetMapping("/manual")
    public String manualHome(@RequestParam(value = "dataType", required = false, defaultValue = "RECEIPT") String dataType,
                             @RequestParam(value = "mode", required = false, defaultValue = "ADD") String mode,
                             Model model) {
        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", mode);
        model.addAttribute("updateReady", false);
        model.addAttribute("record", makeEmptyRecord(dataType));

        return "ingestion/manual-entry";
    }

    @PostMapping("/manual/update/search")
    public String searchForUpdate(@RequestParam("dataType") String dataType,
                                  @RequestParam(value = "referenceNo", required = false) String referenceNo,
                                  @RequestParam(value = "payrollId", required = false) String payrollId,
                                  @RequestParam(value = "employeeId", required = false) String employeeId,
                                  @RequestParam(value = "purchaseId", required = false) String purchaseId,
                                  @RequestParam(value = "recordId", required = false) String recordId,
                                  Model model) {

        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", "UPDATE");

        // Keep user-entered search values for showing back in the UI.
        model.addAttribute("referenceNo", referenceNo);
        model.addAttribute("payrollId", payrollId);
        model.addAttribute("employeeId", employeeId);
        model.addAttribute("purchaseId", purchaseId);
        model.addAttribute("recordId", recordId);

        try {
            Object found = fetchExistingByRequiredKeys(dataType, referenceNo, payrollId, employeeId, purchaseId, recordId);
            if (found == null) {
                throw new IllegalArgumentException("No record found for the given keys.");
            }

            model.addAttribute("record", toForm(found));
            model.addAttribute("updateReady", true);
            model.addAttribute("successMessage", "Record loaded. Now update allowed fields and click Update.");
        } catch (Exception e) {
            model.addAttribute("record", makeEmptyRecord(dataType));
            model.addAttribute("updateReady", false);
            model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Search failed.");
        }
        return "ingestion/manual-entry";
    }

    @PostMapping("/manual/add")
    public String addNew(@RequestParam("dataType") String dataType,
                         @ModelAttribute("record") ManualEntryForm record,
                         Model model) {

        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", "ADD");

        try {
            validateRequiredKeysForAdd(dataType, record);

            if (existsByPk(dataType, record)) {
                throw new IllegalArgumentException("Duplicate ID: primary key already exists.");
            }

            Object saved = saveNew(dataType, record);
            model.addAttribute("record", toForm(saved));
            model.addAttribute("successMessage", "Saved successfully.");
            model.addAttribute("showSummaryModal", true);
        } catch (Exception e) {
            model.addAttribute("record", record);
            model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Add failed.");
        }
        return "ingestion/manual-form";
    }

    @PostMapping("/manual/update")
    public String updateExisting(@RequestParam("dataType") String dataType,
                                 @ModelAttribute("record") ManualEntryForm incoming,
                                 Model model) {

        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", "UPDATE");

        try {
            Object existing = fetchExistingByPkFromRecord(dataType, incoming);
            if (existing == null) {
                throw new IllegalArgumentException("Record not found (cannot update).");
            }

            Object merged = applyAllowedUpdates(dataType, existing, incoming);

            Object saved = saveUpdated(dataType, merged);

            model.addAttribute("record", toForm(saved));
            model.addAttribute("successMessage", "Updated successfully.");
            model.addAttribute("showSummaryModal", true);
        } catch (Exception e) {
            model.addAttribute("record", incoming);
            model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Update failed.");
        }
        return "ingestion/manual-form";
    }


    private ManualEntryForm makeEmptyRecord(String dataType) {
        return new ManualEntryForm();
    }

    private void validateRequiredKeysForAdd(String dataType, ManualEntryForm record) {
        switch (dataType) {
            case "RECEIPT": {
                if (isBlank(record.getReceiptId())) throw new IllegalArgumentException("Receipt ID is required.");
                if (isBlank(record.getReferenceNo())) throw new IllegalArgumentException("Reference No is required.");
                break;
            }
            case "PAYROLL": {
                if (isBlank(record.getPayrollId())) throw new IllegalArgumentException("Payroll ID is required.");
                if (isBlank(record.getEmployeeId())) throw new IllegalArgumentException("Employee ID is required.");
                break;
            }
            case "STOCK": {
                if (isBlank(record.getPurchaseId())) throw new IllegalArgumentException("Purchase ID is required.");
                break;
            }
            case "ACCOUNTING": {
                if (isBlank(record.getRecordId())) throw new IllegalArgumentException("Record ID is required.");
                if (isBlank(record.getReferenceNo())) throw new IllegalArgumentException("Reference No is required.");
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object fetchExistingByRequiredKeys(String dataType,String referenceNo,String payrollId,String employeeId, String purchaseId, String recordId) {
        switch (dataType) {
            case "RECEIPT":
                if (isBlank(referenceNo)) throw new IllegalArgumentException("Reference No is required to search receipts.");
                return receiptRepository.findByReferenceNo(referenceNo).orElse(null);

            case "PAYROLL":
                if (isBlank(payrollId) || isBlank(employeeId)) {
                    throw new IllegalArgumentException("Payroll ID and Employee ID are required to search payroll.");
                }
                Payroll p = payrollRepository.findById(payrollId).orElse(null);
                if (p == null) return null;
                if (p.getEmployeeId() == null || !p.getEmployeeId().equals(employeeId)) {
                    throw new IllegalArgumentException("Employee ID does not match the Payroll ID.");
                }
                return p;

            case "STOCK":
                if (isBlank(purchaseId)) throw new IllegalArgumentException("Purchase ID is required to search stock purchases.");
                return stockPurchaseRepository.findById(purchaseId).orElse(null);

            case "ACCOUNTING":
                if (isBlank(recordId) || isBlank(referenceNo)) {
                    throw new IllegalArgumentException("Record ID and Reference No are required to search accounting.");
                }
                AccountingRecord ar = accountingRecordRepository.findById(recordId).orElse(null);
                if (ar == null) return null;
                if (ar.getReferenceNo() == null || !ar.getReferenceNo().equals(referenceNo)) {
                    throw new IllegalArgumentException("Reference No does not match the Record ID.");
                }
                return ar;
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object fetchExistingByPkFromRecord(String dataType, ManualEntryForm incoming) {
        switch (dataType) {
            case "RECEIPT":
                if (isBlank(incoming.getReceiptId())) return null;
                return receiptRepository.findById(incoming.getReceiptId()).orElse(null);
            case "PAYROLL":
                if (isBlank(incoming.getPayrollId())) return null;
                return payrollRepository.findById(incoming.getPayrollId()).orElse(null);
            case "STOCK":
                if (isBlank(incoming.getPurchaseId())) return null;
                return stockPurchaseRepository.findById(incoming.getPurchaseId()).orElse(null);
            case "ACCOUNTING":
                if (isBlank(incoming.getRecordId())) return null;
                return accountingRecordRepository.findById(incoming.getRecordId()).orElse(null);
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private boolean existsByPk(String dataType, ManualEntryForm record) {
        switch (dataType) {
            case "RECEIPT":
                if (isBlank(record.getReceiptId())) throw new IllegalArgumentException("Receipt ID is required.");
                return receiptRepository.existsById(record.getReceiptId());
            case "PAYROLL":
                if (isBlank(record.getPayrollId())) throw new IllegalArgumentException("Payroll ID is required.");
                return payrollRepository.existsById(record.getPayrollId());
            case "STOCK":
                if (isBlank(record.getPurchaseId())) throw new IllegalArgumentException("Purchase ID is required.");
                return stockPurchaseRepository.existsById(record.getPurchaseId());
            case "ACCOUNTING":
                if (isBlank(record.getRecordId())) throw new IllegalArgumentException("Record ID is required.");
                return accountingRecordRepository.existsById(record.getRecordId());
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object saveNew(String dataType, ManualEntryForm record) {
        switch (dataType) {
            case "RECEIPT": return receiptRepository.save(toReceipt(record));
            case "PAYROLL": return payrollRepository.save(toPayroll(record));
            case "STOCK": return stockPurchaseRepository.save(toStockPurchase(record));
            case "ACCOUNTING": return accountingRecordRepository.save(toAccountingRecord(record));
            default: throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object saveUpdated(String dataType, Object record) {
        switch (dataType) {
            case "RECEIPT": return receiptRepository.save((Receipt) record);
            case "PAYROLL": return payrollRepository.save((Payroll) record);
            case "STOCK": return stockPurchaseRepository.save((StockPurchase) record);
            case "ACCOUNTING": return accountingRecordRepository.save((AccountingRecord) record);
            default: throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object applyAllowedUpdates(String dataType, Object existing, ManualEntryForm incoming) {
        switch (dataType) {
            case "RECEIPT": {
                Receipt ex = (Receipt) existing;
                ex.setPayerName(incoming.getPayerName());
                ex.setAmount(incoming.getAmount());
                return ex;
            }
            case "PAYROLL": {
                Payroll ex = (Payroll) existing;
                ex.setEmployeeName(incoming.getEmployeeName());
                ex.setSalary(incoming.getSalary());
                ex.setStatus(incoming.getStatus());
                return ex;
            }
            case "STOCK": {
                StockPurchase ex = (StockPurchase) existing;
                ex.setVendorName(incoming.getVendorName());
                ex.setAmount(incoming.getAmount());
                return ex;
            }
            case "ACCOUNTING": {
                AccountingRecord ex = (AccountingRecord) existing;
                ex.setModule(incoming.getModule());
                ex.setAmount(incoming.getAmount());
                return ex;
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Receipt toReceipt(ManualEntryForm form) {
        Receipt receipt = new Receipt();
        receipt.setReceiptId(form.getReceiptId());
        receipt.setReferenceNo(form.getReferenceNo());
        receipt.setPayerName(form.getPayerName());
        receipt.setAmount(form.getAmount());
        receipt.setReceiptDate(form.getReceiptDate());
        receipt.setEnteredBy(form.getEnteredBy());
        receipt.setCreatedAt(form.getCreatedAt());
        return receipt;
    }

    private Payroll toPayroll(ManualEntryForm form) {
        Payroll payroll = new Payroll();
        payroll.setPayrollId(form.getPayrollId());
        payroll.setEmployeeId(form.getEmployeeId());
        payroll.setEmployeeName(form.getEmployeeName());
        payroll.setSalary(form.getSalary());
        payroll.setPaymentDate(form.getPaymentDate());
        payroll.setReferenceNo(form.getReferenceNo());
        payroll.setStatus(form.getStatus());
        payroll.setEnteredBy(form.getEnteredBy());
        payroll.setCreatedAt(form.getCreatedAt());
        return payroll;
    }

    private StockPurchase toStockPurchase(ManualEntryForm form) {
        StockPurchase purchase = new StockPurchase();
        purchase.setPurchaseId(form.getPurchaseId());
        purchase.setVendorName(form.getVendorName());
        purchase.setInvoiceNo(form.getInvoiceNo());
        purchase.setAmount(form.getAmount());
        purchase.setPurchaseDate(form.getPurchaseDate());
        purchase.setEnteredBy(form.getEnteredBy());
        purchase.setCreatedAt(form.getCreatedAt());
        return purchase;
    }

    private AccountingRecord toAccountingRecord(ManualEntryForm form) {
        AccountingRecord record = new AccountingRecord();
        record.setRecordId(form.getRecordId());
        record.setReferenceNo(form.getReferenceNo());
        record.setModule(form.getModule());
        record.setAmount(form.getAmount());
        record.setRecordDate(form.getRecordDate());
        record.setEnteredBy(form.getEnteredBy());
        record.setCreatedAt(form.getCreatedAt());
        return record;
    }

    private ManualEntryForm toForm(Object entity) {
        ManualEntryForm form = new ManualEntryForm();
        if (entity instanceof Receipt receipt) {
            form.setReceiptId(receipt.getReceiptId());
            form.setReferenceNo(receipt.getReferenceNo());
            form.setPayerName(receipt.getPayerName());
            form.setAmount(receipt.getAmount());
            form.setReceiptDate(receipt.getReceiptDate());
            form.setEnteredBy(receipt.getEnteredBy());
            form.setCreatedAt(receipt.getCreatedAt());
        } else if (entity instanceof Payroll payroll) {
            form.setPayrollId(payroll.getPayrollId());
            form.setEmployeeId(payroll.getEmployeeId());
            form.setEmployeeName(payroll.getEmployeeName());
            form.setSalary(payroll.getSalary());
            form.setPaymentDate(payroll.getPaymentDate());
            form.setReferenceNo(payroll.getReferenceNo());
            form.setStatus(payroll.getStatus());
            form.setEnteredBy(payroll.getEnteredBy());
            form.setCreatedAt(payroll.getCreatedAt());
        } else if (entity instanceof StockPurchase purchase) {
            form.setPurchaseId(purchase.getPurchaseId());
            form.setVendorName(purchase.getVendorName());
            form.setInvoiceNo(purchase.getInvoiceNo());
            form.setAmount(purchase.getAmount());
            form.setPurchaseDate(purchase.getPurchaseDate());
            form.setEnteredBy(purchase.getEnteredBy());
            form.setCreatedAt(purchase.getCreatedAt());
        } else if (entity instanceof AccountingRecord record) {
            form.setRecordId(record.getRecordId());
            form.setReferenceNo(record.getReferenceNo());
            form.setModule(record.getModule());
            form.setAmount(record.getAmount());
            form.setRecordDate(record.getRecordDate());
            form.setEnteredBy(record.getEnteredBy());
            form.setCreatedAt(record.getCreatedAt());
        }
        return form;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @GetMapping("/manual-entry")
    public String showManualEntryPage() {
        return "redirect:/ingestion/manual";
    }
}
