package com.frwss.system.dto;

import java.util.List;

public class IngestionResult {
    private List<?> records;
    private int totalRecords;
    private int validRecords;
    private int invalidRecords;

    public IngestionResult(List<?> records, int totalRecords, int validRecords, int invalidRecords) {
        this.records = records;
        this.totalRecords = totalRecords;
        this.validRecords = validRecords;
        this.invalidRecords = invalidRecords;
    }

    // Getters and Setters
    public List<?> getRecords() { return records; }
    public int getTotalRecords() { return totalRecords; }
    public int getValidRecords() { return validRecords; }
    public int getInvalidRecords() { return invalidRecords; }
}