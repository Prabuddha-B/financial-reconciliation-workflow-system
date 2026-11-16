// -----------------------------------------------------------------------------------------
//       Coordinates financial file uploads, preview, and record-saving requests
// ----------------------------------------------------------------------------------------
package com.frwss.system.controller;

import com.frwss.system.dto.IngestionResult;
import com.frwss.system.service.IngestionService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    // Constructor
    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    /**
     * Displays the main financial data ingestion dashboard.
     * */
    @GetMapping("/ingestion")
    public String ingestionPage(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

    /**
     *  Displays the ingestion dashboard through its dedicated dashboard route.
     *  */
    @GetMapping("/ingestion/dashboard")
    public String dashboard(Model model) {
        populateDashboardStats(model);
        return "ingestion/dashboard";
    }

    /**
     * Displays the file upload interface.
     * */
    @GetMapping("/ingestion/upload")
    public String showUploadPage(Model model) {
        return "ingestion/upload";
    }

    /**
     * Processes an uploaded file for preview and temporarily stores the
     * processed records in the user's session until they are saved.
     */
    @PostMapping({"/ingestion/preview", "/ingestion/upload"})
    public String previewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType,
            Model model,
            HttpSession session) {

        log.info("Upload request received: fileName={}, size={}, dataType={}",
                file != null ? file.getOriginalFilename() : null,
                file != null ? file.getSize() : null,
                dataType);

        try {
            if (file == null || file.isEmpty()) {
                model.addAttribute("error", "Please select a file before uploading.");
                return "ingestion/upload";
            }

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
            log.error("Preview failed for fileName={}, dataType={}",
                    file != null ? file.getOriginalFilename() : null,
                    dataType, e);
            model.addAttribute("error", "Preview failed: " + e.getMessage());
            return "ingestion/upload";
        }
    }

    /**
     * Handles errors raised during multipart file uploads.
     * */
    @ExceptionHandler({MultipartException.class, MaxUploadSizeExceededException.class})
    public String handleMultipartFailure(Exception e, Model model) {
        log.error("Multipart upload failed", e);
        model.addAttribute("error", "Upload failed: " + e.getMessage());
        return "ingestion/upload";
    }

    /**
     * Retrieves pending preview records from the session and delegates
     * their persistence to the ingestion service.
     */
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

    /**
     * Processes and saves an uploaded CSV file without the preview workflow.
     * */
    @PostMapping("/upload-csv")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        try {
            ingestionService.processCSV(file);
            ra.addFlashAttribute("message", "File processed and records saved!");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to process: " + e.getMessage());
        }
        return "redirect:/ingestion/upload";
    }

    /**
     * Loads the record counts displayed on the ingestion dashboard.
     * */
    private void populateDashboardStats(Model model) {
        IngestionService.DashboardSummary summary = ingestionService.getDashboardSummary();
        model.addAttribute("total", summary.totalCount());
        model.addAttribute("validCount", summary.validCount());
        model.addAttribute("invalidCount", summary.invalidCount());
    }

    /**
     * Optimized bulk upload for large CSV files (10,000+ records)
     * Uses batch processing for fast ingestion
     */
    @PostMapping("/ingestion/bulk-upload")
    public String bulkUpload(
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {

        try {
            log.info("Received bulk upload request: fileName={}, size={}",
                    file.getOriginalFilename(), file.getSize());

            long startTime = System.currentTimeMillis();
            IngestionResult result = ingestionService.processLargeAccountingFile(file);
            long endTime = System.currentTimeMillis();

            String message = String.format(
                    "Bulk upload completed in %d ms! " +
                            "Total: %d, Saved: %d, Invalid: %d, Duplicates: %d",
                    (endTime - startTime),
                    result.getTotalRecords(),
                    result.getValidRecords(),
                    result.getInvalidRecords(),
                    result.getTotalRecords() - result.getValidRecords() - result.getInvalidRecords()
            );

            log.info(message);
            redirectAttributes.addFlashAttribute("message", message);

        } catch (Exception e) {
            log.error("Bulk upload failed", e);
            redirectAttributes.addFlashAttribute("error",
                    "Bulk upload failed: " + e.getMessage());
        }

        return "redirect:/ingestion/upload";
    }
}
