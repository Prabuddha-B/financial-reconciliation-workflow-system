package com.frwss.system.service;

import com.frwss.system.model.*;
import com.frwss.system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ManualIngestionServiceImpl
        implements ManualIngestionService{

    @Autowired
    private ReceiptRepository receiptRepository;

    @Autowired
    private PayrollRepository payrollRepository;

    @Autowired
    private StockPurchaseRepository stockRepository;

    public Receipt saveReceipt(Receipt receipt){
        return receiptRepository.save(receipt);
    }

    public Payroll savePayroll(Payroll payroll){
        return payrollRepository.save(payroll);
    }

    public StockPurchase savePurchase(
            StockPurchase purchase){
        return stockRepository.save(purchase);
    }

}