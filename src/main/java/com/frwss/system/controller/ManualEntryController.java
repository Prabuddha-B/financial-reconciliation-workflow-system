package com.frwss.system.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/ingestion")
public class ManualEntryController {

    @GetMapping("/manual-entry")
    public String manualEntryPage() {
        return "ingestion/manual-entry";
    }
}
