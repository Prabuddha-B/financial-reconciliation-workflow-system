package com.frwss.system.controller;

import com.frwss.system.dto.IngestionResult;
import com.frwss.system.service.IngestionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class IngestionController {

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/ingestion")
    public String ingestionPage(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

    @GetMapping("/ingestion/dashboard")
    public String dashboard(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

    @GetMapping("/ingestion/upload")
    public String showUploadPage() {
        return "ingestion/upload";
    }

    @PostMapping({"/ingestion/preview", "/ingestion/upload"})
    public String previewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType,
            Model model,
            HttpSession session) {
        try {
            IngestionResult result = ingestionService.processFileForPreview(file, dataType);

            model.addAttribute("dataType", dataType);
            model.addAttribute("records", result.getRecords());
            model.addAttribute("total", result.getTotalRecords());
            model.addAttribute("validCount", result.getValidRecords());
            model.addAttribute("invalidCount", result.getInvalidRecords());

            session.setAttribute("lastFileName", file.getOriginalFilename());
            session.setAttribute("pendingType", dataType);
            session.setAttribute("pendingRecords", result.getRecords());

            return "ingestion/result";

        } catch (Exception e) {
            model.addAttribute("error", "Preview failed: " + e.getMessage());
            return "ingestion/upload";
        }
    }

    @PostMapping("/ingestion/save")
    public String saveFile(HttpSession session, RedirectAttributes ra) {
        List<?> records = (List<?>) session.getAttribute("pendingRecords");
        String dataType = (String) session.getAttribute("pendingType");

        if (records == null || dataType == null) {
            ra.addFlashAttribute("error", "Session expired or file missing.");
            return "redirect:/ingestion/upload";
        }

        try {
            IngestionService.SaveSummary summary = ingestionService.saveProcessedRecords(records, dataType);

            String message = "Saved " + summary.savedCount() + " records.";
            if (summary.skippedDuplicateCount() > 0) {
                message += " Skipped " + summary.skippedDuplicateCount() + " duplicate records.";
            }
            if (summary.skippedInvalidCount() > 0) {
                message += " Skipped " + summary.skippedInvalidCount() + " invalid records.";
            }

            ra.addFlashAttribute("message", message);
            session.removeAttribute("pendingRecords");
            session.removeAttribute("pendingType");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Save failed: " + e.getMessage());
        }

        return "redirect:/ingestion/upload";
    }

    @PostMapping("/upload-csv")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        try {
            ingestionService.processCSV(file);
            ra.addFlashAttribute("message", "File processed and records saved to pgAdmin!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to process: " + e.getMessage());
        }
        return "redirect:/ingestion/upload";
    }

    private void populateDashboardStats(Model model) {
        IngestionService.DashboardSummary summary = ingestionService.getDashboardSummary();
        model.addAttribute("total", summary.totalCount());
        model.addAttribute("validCount", summary.validCount());
        model.addAttribute("invalidCount", summary.invalidCount());
    }
}
