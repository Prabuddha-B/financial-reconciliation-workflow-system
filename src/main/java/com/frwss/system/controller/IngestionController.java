package com.frwss.system.controller;

import com.frwss.system.model.IngestionResult;
import com.frwss.system.model.Receipt;
import com.frwss.system.service.IngestionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    // Dashboard
    @GetMapping
    public String ingestionPage(){
        return "ingestion/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model){
        addSummaryAttributes(model, List.of());
        return "ingestion/dashboard";
    }

    // Upload page
    @GetMapping("/upload")
    public String uploadPage(){
        return "ingestion/upload";
    }

    // MAIN upload handler (CSV / Excel processing)
    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {
        if (file.isEmpty()) {
            model.addAttribute("uploadError", "Please choose a CSV or XLSX file before uploading.");
            return "ingestion/upload";
        }

        try {
            IngestionResult result = ingestionService.processFile(file);
            model.addAttribute("records", result.getReceiptRecords());
            model.addAttribute("dataType", result.getDataType());
            model.addAttribute("total", result.getTotal());
            model.addAttribute("validCount", result.getValidCount());
            model.addAttribute("invalidCount", result.getInvalidCount());
            model.addAttribute("insertedCount", result.getInsertedCount());
            model.addAttribute("skippedCount", result.getSkippedCount());
            model.addAttribute("replacedCount", result.getReplacedCount());
            return "ingestion/result";
        } catch (IllegalArgumentException exception) {
            model.addAttribute("uploadError", exception.getMessage());
            return "ingestion/upload";
        }
    }

    private void addSummaryAttributes(Model model, List<Receipt> records) {
        int total = records.size();
        int validCount = 0;
        int invalidCount = 0;

        for (Receipt receipt : records) {
            if (receipt.isValid()) {
                validCount++;
            } else {
                invalidCount++;
            }
        }

        model.addAttribute("total", total);
        model.addAttribute("validCount", validCount);
        model.addAttribute("invalidCount", invalidCount);
    }
}
