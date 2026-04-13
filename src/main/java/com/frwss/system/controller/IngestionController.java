package com.frwss.system.controller;

import com.frwss.system.model.Receipt;
import com.frwss.system.service.IngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/ingestion")
public class IngestionController {

    @Autowired
    private IngestionService ingestionService;

    @GetMapping
    public String ingestionPage(){
        return "ingestion/dashboard";
    }

    @GetMapping("/upload")
    public String uploadPage(){
        return "ingestion/upload";
    }

    @PostMapping("/upload")
    public String handleFileUpload(@RequestParam("file") MultipartFile file, Model model) {

        List<Receipt> records = ingestionService.processFile(file);

        model.addAttribute("records", records);

        return "ingestion/result";
    }
}