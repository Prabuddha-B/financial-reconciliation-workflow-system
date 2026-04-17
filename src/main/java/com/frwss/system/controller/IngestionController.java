package com.frwss.system.controller;

import com.frwss.system.model.AccountingRecord;
import com.frwss.system.model.Payroll;
import com.frwss.system.model.Receipt;
import com.frwss.system.model.StockPurchase;
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
    public String handleFileUpload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dataType") String dataType,
            Model model) {

        List<?> records = ingestionService.processFile(file, dataType);

        model.addAttribute("records", records);
        model.addAttribute("dataType", dataType);


        if ("RECEIPT".equals(dataType)) {

            List<Receipt> list = (List<Receipt>) records;

            int total = list.size();
            int validCount = 0;
            int invalidCount = 0;

            for (Receipt r : list) {
                if (r.isValid()) validCount++;
                else invalidCount++;
            }

            model.addAttribute("total", total);
            model.addAttribute("validCount", validCount);
            model.addAttribute("invalidCount", invalidCount);
        }

        else if ("PAYROLL".equals(dataType)) {

            List<Payroll> list = (List<Payroll>) records;

            int total = list.size();
            int validCount = 0;
            int invalidCount = 0;

            for (Payroll p : list) {
                if (p.isValid()) validCount++;
                else invalidCount++;
            }

            model.addAttribute("total", total);
            model.addAttribute("validCount", validCount);
            model.addAttribute("invalidCount", invalidCount);
        }

        else if ("STOCK".equals(dataType)) {

            List<StockPurchase> list = (List<StockPurchase>) records;

            int total = list.size();
            int validCount = 0;
            int invalidCount = 0;

            for (StockPurchase s : list) {
                if (s.isValid()) validCount++;
                else invalidCount++;
            }

            model.addAttribute("total", total);
            model.addAttribute("validCount", validCount);
            model.addAttribute("invalidCount", invalidCount);
        }

        else if ("ACCOUNTING".equals(dataType)) {

            List<AccountingRecord> list = (List<AccountingRecord>) records;

            int total = list.size();
            int validCount = 0;
            int invalidCount = 0;

            for (AccountingRecord a : list) {
                if (a.isValid()) validCount++;
                else invalidCount++;
            }

            model.addAttribute("total", total);
            model.addAttribute("validCount", validCount);
            model.addAttribute("invalidCount", invalidCount);
        }

        return "ingestion/result";
    }


    @GetMapping("/dashboard")
    public String dashboard(){
        return "ingestion/dashboard";
    }
}