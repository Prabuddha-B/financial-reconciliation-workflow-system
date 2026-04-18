package com.frwss.system.controller;

import com.frwss.system.service.IngestionService;
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

    @Autowired private IngestionService ingestionService;

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
        model.addAttribute("updateReady", false); // ✅ important for update flow
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

        // ✅ keep user-entered search values for showing back in the UI
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

            model.addAttribute("record", found);
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
                         @ModelAttribute("record") Object record,
                         Model model) {

        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", "ADD");

        try {
            validateRequiredKeysForAdd(dataType, record);

//            ingestionService.validate(record);
//            Line Removed because validation is already handled inside service.

            if (existsByPk(dataType, record)) {
                throw new IllegalArgumentException("Duplicate ID: primary key already exists.");
            }

            Object saved = saveNew(dataType, record);
            model.addAttribute("record", saved);
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
                                 @ModelAttribute("record") Object incoming,
                                 Model model) {

        model.addAttribute("dataType", dataType);
        model.addAttribute("mode", "UPDATE");

        try {
            Object existing = fetchExistingByPkFromRecord(dataType, incoming);
            if (existing == null) {
                throw new IllegalArgumentException("Record not found (cannot update).");
            }

            Object merged = applyAllowedUpdates(dataType, existing, incoming);

//            ingestionService.validate(merged);

            Object saved = saveUpdated(dataType, merged);

            model.addAttribute("record", saved);
            model.addAttribute("successMessage", "Updated successfully.");
            model.addAttribute("showSummaryModal", true);
        } catch (Exception e) {
            model.addAttribute("record", incoming);
            model.addAttribute("errorMessage", e.getMessage() != null ? e.getMessage() : "Update failed.");
        }

        return "ingestion/manual-form";
    }


    private Object makeEmptyRecord(String dataType) {
        switch (dataType) {
            case "RECEIPT": return new Receipt();
            case "PAYROLL": return new Payroll();
            case "STOCK": return new StockPurchase();
            case "ACCOUNTING": return new AccountingRecord();
            default: return new Receipt();
        }
    }

    private void validateRequiredKeysForAdd(String dataType, Object record) {
        switch (dataType) {
            case "RECEIPT": {
                Receipt r = (Receipt) record;
                if (isBlank(r.getReceiptId())) throw new IllegalArgumentException("Receipt ID is required.");
                if (isBlank(r.getReferenceNo())) throw new IllegalArgumentException("Reference No is required.");
                break;
            }
            case "PAYROLL": {
                Payroll p = (Payroll) record;
                if (isBlank(p.getPayrollId())) throw new IllegalArgumentException("Payroll ID is required.");
                if (isBlank(p.getEmployeeId())) throw new IllegalArgumentException("Employee ID is required.");
                break;
            }
            case "STOCK": {
                StockPurchase s = (StockPurchase) record;
                if (isBlank(s.getPurchaseId())) throw new IllegalArgumentException("Purchase ID is required.");
                break;
            }
            case "ACCOUNTING": {
                AccountingRecord a = (AccountingRecord) record;
                if (isBlank(a.getRecordId())) throw new IllegalArgumentException("Record ID is required.");
                if (isBlank(a.getReferenceNo())) throw new IllegalArgumentException("Reference No is required.");
                break;
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object fetchExistingByRequiredKeys(String dataType,
                                               String referenceNo,
                                               String payrollId,
                                               String employeeId,
                                               String purchaseId,
                                               String recordId) {
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

    private Object fetchExistingByPkFromRecord(String dataType, Object incoming) {
        switch (dataType) {
            case "RECEIPT": {
                Receipt r = (Receipt) incoming;
                if (isBlank(r.getReceiptId())) return null;
                return receiptRepository.findById(r.getReceiptId()).orElse(null);
            }
            case "PAYROLL": {
                Payroll p = (Payroll) incoming;
                if (isBlank(p.getPayrollId())) return null;
                return payrollRepository.findById(p.getPayrollId()).orElse(null);
            }
            case "STOCK": {
                StockPurchase s = (StockPurchase) incoming;
                if (isBlank(s.getPurchaseId())) return null;
                return stockPurchaseRepository.findById(s.getPurchaseId()).orElse(null);
            }
            case "ACCOUNTING": {
                AccountingRecord a = (AccountingRecord) incoming;
                if (isBlank(a.getRecordId())) return null;
                return accountingRecordRepository.findById(a.getRecordId()).orElse(null);
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private boolean existsByPk(String dataType, Object record) {
        switch (dataType) {
            case "RECEIPT": {
                Receipt r = (Receipt) record;
                if (isBlank(r.getReceiptId())) throw new IllegalArgumentException("Receipt ID is required.");
                return receiptRepository.existsById(r.getReceiptId());
            }
            case "PAYROLL": {
                Payroll p = (Payroll) record;
                if (isBlank(p.getPayrollId())) throw new IllegalArgumentException("Payroll ID is required.");
                return payrollRepository.existsById(p.getPayrollId());
            }
            case "STOCK": {
                StockPurchase s = (StockPurchase) record;
                if (isBlank(s.getPurchaseId())) throw new IllegalArgumentException("Purchase ID is required.");
                return stockPurchaseRepository.existsById(s.getPurchaseId());
            }
            case "ACCOUNTING": {
                AccountingRecord a = (AccountingRecord) record;
                if (isBlank(a.getRecordId())) throw new IllegalArgumentException("Record ID is required.");
                return accountingRecordRepository.existsById(a.getRecordId());
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object saveNew(String dataType, Object record) {
        switch (dataType) {
            case "RECEIPT": return receiptRepository.save((Receipt) record);
            case "PAYROLL": return payrollRepository.save((Payroll) record);
            case "STOCK": return stockPurchaseRepository.save((StockPurchase) record);
            case "ACCOUNTING": return accountingRecordRepository.save((AccountingRecord) record);
            default: throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private Object saveUpdated(String dataType, Object record) {
        return saveNew(dataType, record);
    }

    private Object applyAllowedUpdates(String dataType, Object existing, Object incoming) {
        switch (dataType) {
            case "RECEIPT": {
                Receipt ex = (Receipt) existing;
                Receipt in = (Receipt) incoming;
                ex.setPayerName(in.getPayerName());
                ex.setAmount(in.getAmount());
                return ex;
            }
            case "PAYROLL": {
                Payroll ex = (Payroll) existing;
                Payroll in = (Payroll) incoming;
                ex.setEmployeeName(in.getEmployeeName());
                ex.setSalary(in.getSalary());
                ex.setStatus(in.getStatus());
                return ex;
            }
            case "STOCK": {
                StockPurchase ex = (StockPurchase) existing;
                StockPurchase in = (StockPurchase) incoming;
                ex.setVendorName(in.getVendorName());
                ex.setAmount(in.getAmount());
                return ex;
            }
            case "ACCOUNTING": {
                AccountingRecord ex = (AccountingRecord) existing;
                AccountingRecord in = (AccountingRecord) incoming;
                ex.setModule(in.getModule());
                ex.setAmount(in.getAmount());
                return ex;
            }
            default:
                throw new IllegalArgumentException("Invalid data type: " + dataType);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @GetMapping("/manual-entry")
    public String showManualEntryPage() {
        return "ingestion/manual-entry";
    }
}