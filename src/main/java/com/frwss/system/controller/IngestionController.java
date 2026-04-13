package com.frwss.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IngestionController {
    @GetMapping("/ingestion")
    public String ingestionPage(){
        return "ingestion/dashboard";
    }
}
