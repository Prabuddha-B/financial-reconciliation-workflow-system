package com.frwss.system.service;

import com.frwss.system.model.*;

public interface ManualIngestionService {

    Receipt saveReceipt(Receipt receipt);

    Payroll savePayroll(Payroll payroll);

    StockPurchase savePurchase(StockPurchase purchase);
}