package com.frwss.system.model;

import java.util.List;

public class IngestionResult {

    private final String dataType;
    private final List<Receipt> receiptRecords;
    private final int total;
    private final int validCount;
    private final int invalidCount;
    private final int insertedCount;
    private final int skippedCount;
    private final int replacedCount;

    private IngestionResult(String dataType,
                            List<Receipt> receiptRecords,
                            int total,
                            int validCount,
                            int invalidCount,
                            int insertedCount,
                            int skippedCount,
                            int replacedCount) {
        this.dataType = dataType;
        this.receiptRecords = receiptRecords;
        this.total = total;
        this.validCount = validCount;
        this.invalidCount = invalidCount;
        this.insertedCount = insertedCount;
        this.skippedCount = skippedCount;
        this.replacedCount = replacedCount;
    }

    public static IngestionResult receiptResult(List<Receipt> receiptRecords, int validCount, int invalidCount) {
        return new IngestionResult("receipt", receiptRecords, receiptRecords.size(), validCount, invalidCount, 0, 0, 0);
    }

    public static IngestionResult payrollResult(List<Receipt> receiptRecords, int insertedCount, int skippedCount, int replacedCount) {
        return new IngestionResult(
                "payroll",
                receiptRecords,
                receiptRecords.size(),
                insertedCount,
                skippedCount,
                insertedCount,
                skippedCount,
                replacedCount
        );
    }

    public String getDataType() {
        return dataType;
    }

    public List<Receipt> getReceiptRecords() {
        return receiptRecords;
    }

    public int getTotal() {
        return total;
    }

    public int getValidCount() {
        return validCount;
    }

    public int getInvalidCount() {
        return invalidCount;
    }

    public int getInsertedCount() {
        return insertedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public int getReplacedCount() {
        return replacedCount;
    }

    public boolean isPayroll() {
        return "payroll".equals(dataType);
    }
}
