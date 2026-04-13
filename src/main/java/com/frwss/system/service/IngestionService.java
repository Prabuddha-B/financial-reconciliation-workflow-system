package com.frwss.system.service;

import com.frwss.system.model.Receipt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestionService {

    public List<Receipt> processFile(MultipartFile file) {

        List<Receipt> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

            // For CSV
            assert fileName != null;
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {

                    if (line.startsWith("\uFEFF")) {
                        line = line.substring(1);
                    }

                    if (line.trim().isEmpty()) continue;

                    if (isFirstLine) {
                        isFirstLine = false;
                        continue;
                    }

                    String[] data = line.split(",");

                    Receipt receipt = new Receipt(
                            Integer.parseInt(data[0]),
                            data[1],
                            data[2],
                            Double.parseDouble(data[3]),
                            data[4],
                            data[5]
                    );

                    validate(receipt);

                    records.add(receipt);
                }
            }

            // for XLSX
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook =
                        new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                boolean isFirstRow = true;

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    if (isFirstRow) {
                        isFirstRow = false;
                        continue;
                    }

                    try {
                        Receipt receipt = new Receipt(
                                (int) row.getCell(0).getNumericCellValue(),
                                row.getCell(1).toString(),
                                row.getCell(2).toString(),
                                row.getCell(3).getNumericCellValue(),
                                row.getCell(4).toString(),
                                row.getCell(5).toString()
                        );

                        validate(receipt);

                        records.add(receipt);

                    } catch (Exception e) {
                        System.out.println("Row error: " + e.getMessage());
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return records;
    }


    private void validate(Receipt receipt) {

        if (receipt.getAmount() <= 0) {
            receipt.setValid(false);
            receipt.setErrorMessage("Invalid amount");
        }

        if (receipt.getReferenceNo() == null || receipt.getReferenceNo().isEmpty()) {
            receipt.setValid(false);
            receipt.setErrorMessage("Missing reference");
        }
    }
}