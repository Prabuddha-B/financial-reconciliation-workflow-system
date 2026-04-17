package com.frwss.system.controller;

import com.frwss.system.dto.IngestionResult; // Or .dto depending on your package
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

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @GetMapping("/upload")
    public String showUploadPage() {
        return "ingestion/upload";
    }

    // STEP 1: PREVIEW
    @PostMapping("/preview")
    public String previewFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType,
            Model model,
            HttpSession session
    ) {
        try {
            // Use our new IngestionResult class
            IngestionResult result = ingestionService.processFileForPreview(file, dataType);

            model.addAttribute("result", result);
            model.addAttribute("dataType", dataType);

            // IMPORTANT: Instead of storing the 'file', we process it here or
            // store the data. For now, we'll assume the user will re-upload
            // or we use a 'Direct Save' approach if you don't want to re-parse.
            // For a UoP project, re-parsing in Step 2 is the easiest "Safe" way:
            session.setAttribute("lastFileName", file.getOriginalFilename());
            session.setAttribute("pendingType", dataType);

            // To avoid the 'File is deleted' error, for now, your Service
            // should probably save the file to a temp folder, OR you can
            // just use the List from the 'result' object.
            session.setAttribute("pendingRecords", result.getRecords());

            return "ingestion/result";

        } catch (Exception e) {
            model.addAttribute("error", "Preview failed: " + e.getMessage());
            return "ingestion/upload";
        }
    }

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
            // Updated Service method to save the List directly
            ingestionService.saveProcessedRecords(records, dataType);

            ra.addFlashAttribute("message", "Successfully saved records!");
            session.removeAttribute("pendingRecords");
            session.removeAttribute("pendingType");

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Save failed: " + e.getMessage());
        }

        return "redirect:/ingestion/upload";
    }
}