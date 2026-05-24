package com.frwss.system.controller;

import com.frwss.system.model.*;
import com.frwss.system.service.ManualIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ManualIngestionController {

    @Autowired
    private ManualIngestionService service;

    @PostMapping("/receipts")
    public Receipt addReceipt(
            @RequestBody Receipt receipt){
        return service.saveReceipt(receipt);
    }

    @PostMapping("/payroll")
    public Payroll addPayroll(
            @RequestBody Payroll payroll){
        return service.savePayroll(payroll);
    }

    @PostMapping("/purchases")
    public StockPurchase addPurchase(
            @RequestBody StockPurchase purchase){
        return service.savePurchase(purchase);
    }

}