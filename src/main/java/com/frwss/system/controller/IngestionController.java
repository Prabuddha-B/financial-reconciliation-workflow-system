package com.frwss.system.controller;

import com.frwss.system.service.IngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IngestionController {

    @Autowired
    private IngestionService ingestionService;

    @PostMapping("/upload-csv")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        try {
            ingestionService.processCSV(file);
            ra.addFlashAttribute("message", "File processed and records saved to pgAdmin!");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Failed to process: " + e.getMessage());
        }
        // Redirect back to your upload page
        return "redirect:/ingestion/upload";
    }

    @PostMapping("/ingestion/upload/payroll")
    public String uploadPayrollCsv(@RequestParam("file") MultipartFile file, RedirectAttributes attributes){
        if(file.isEmpty()){
            attributes.addFlashAttribute("message", "Please select a payroll CSV file to upload.");
            return "redirect:/ingestion/upload";
        }

        try{
            ingestionService.savePayrollCsv(file);
            attributes.addFlashAttribute("message", "Payroll data uploaded successfully!");
        } catch(Exception e){
            attributes.addFlashAttribute("message", "Upload failed : " + e.getMessage());
        }

        return "redirect:/ingestion/upload";
    }

    @GetMapping("/ingestion/upload")
    public String showUploadPage() {
        return "ingestion/upload"; // This looks for the HTML file you just made
    }
}