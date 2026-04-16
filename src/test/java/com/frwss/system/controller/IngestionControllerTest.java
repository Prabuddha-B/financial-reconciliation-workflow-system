package com.frwss.system.controller;

import com.frwss.system.model.IngestionResult;
import com.frwss.system.model.Receipt;
import com.frwss.system.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest({HomeController.class, IngestionController.class, ManualEntryController.class})
class IngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IngestionService ingestionService;

    @Test
    void rootRedirectsToDashboard() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ingestion/dashboard"));
    }

    @Test
    void dashboardRendersWithDefaultCounts() throws Exception {
        mockMvc.perform(get("/ingestion/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingestion/dashboard"))
                .andExpect(model().attribute("total", 0))
                .andExpect(model().attribute("validCount", 0))
                .andExpect(model().attribute("invalidCount", 0))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Data Ingestion Dashboard")));
    }

    @Test
    void manualEntryPageRenders() throws Exception {
        mockMvc.perform(get("/ingestion/manual-entry"))
                .andExpect(status().isOk())
                .andExpect(view().name("ingestion/manual-entry"));
    }

    @Test
    void uploadResultRendersSummaryAndRows() throws Exception {
        Receipt validReceipt = new Receipt(1, "REF-100", "Alice", 100.50, "2026-04-17", "Admin");
        Receipt invalidReceipt = new Receipt(2, "", "Bob", -10.0, "2026-04-17", "Admin");
        invalidReceipt.setValid(false);
        invalidReceipt.setErrorMessage("Invalid amount");

        given(ingestionService.processFile(any()))
                .willReturn(IngestionResult.receiptResult(List.of(validReceipt, invalidReceipt), 1, 1));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "receipts.csv",
                "text/csv",
                "receiptId,referenceNo,payerName,amount,date,enteredBy".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/ingestion/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(view().name("ingestion/result"))
                .andExpect(model().attribute("total", 2))
                .andExpect(model().attribute("validCount", 1))
                .andExpect(model().attribute("invalidCount", 1))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Uploaded Data")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("REF-100")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid amount")));
    }

    @Test
    void payrollUploadRendersDatabaseImportSummary() throws Exception {
        Receipt insertedPayrollRow = new Receipt(1, "PAY-001", "Alice", 3500.00, "2026-04-17", "System");
        Receipt replacedPayrollRow = new Receipt(2, "PAY-002", "Bob", 2800.00, "2026-04-17", "System");
        replacedPayrollRow.setErrorMessage("Replaced existing payroll_id");
        Receipt skippedPayrollRow = new Receipt(3, "PAY-002", "Bob Duplicate", 2800.00, "2026-04-17", "System");
        skippedPayrollRow.setValid(false);
        skippedPayrollRow.setErrorMessage("Skipped: duplicate payroll_id in upload");

        given(ingestionService.processFile(any()))
                .willReturn(IngestionResult.payrollResult(List.of(insertedPayrollRow, replacedPayrollRow, skippedPayrollRow), 2, 1, 1));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "payroll.csv",
                "text/csv",
                "payroll_id,employee_id,employee_name,salary,payment_date,reference_no,status,created_at,entered_by"
                        .getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/ingestion/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(view().name("ingestion/result"))
                .andExpect(model().attribute("dataType", "payroll"))
                .andExpect(model().attribute("total", 3))
                .andExpect(model().attribute("insertedCount", 2))
                .andExpect(model().attribute("skippedCount", 1))
                .andExpect(model().attribute("replacedCount", 1))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Payroll Upload Result")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("payroll table")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PAY-001")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Replaced existing payroll_id")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Skipped: duplicate payroll_id in upload")));
    }

    @Test
    void emptyUploadReturnsUploadPageWithError() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/csv", new byte[0]);

        mockMvc.perform(multipart("/ingestion/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(view().name("ingestion/upload"))
                .andExpect(model().attributeExists("uploadError"));
    }
}
