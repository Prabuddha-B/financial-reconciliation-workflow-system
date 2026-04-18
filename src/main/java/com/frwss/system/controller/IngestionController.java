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
@RequestMapping("/ingestion")
public class IngestionController {

    private final IngestionService ingestionService;

//    Constructor
    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

//    Loads the ingestion dashboard with summary stat
    @GetMapping
    public String ingestionPage(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

//    Alternative route for dashboard
    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

//    Display the file upload page for ingestion
    @GetMapping("/upload")
    public String showUploadPage() {
        return "ingestion/upload";
    }

//    Handle File Uploads
    // STEP 1: PREVIEW
    @PostMapping({"/preview", "/upload"})
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

//    Save only valid records
    // STEP 2: SAVE
    @PostMapping("/save")
    public String saveFile(HttpSession session, RedirectAttributes ra) {
        // Retrieve the List we stored in Step 1
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

//    Fetch dashboard summary stat and add to the model
    private void populateDashboardStats(Model model) {
        IngestionService.DashboardSummary summary = ingestionService.getDashboardSummary();
        model.addAttribute("total", summary.totalCount());
        model.addAttribute("validCount", summary.validCount());
        model.addAttribute("invalidCount", summary.invalidCount());
    }
}
