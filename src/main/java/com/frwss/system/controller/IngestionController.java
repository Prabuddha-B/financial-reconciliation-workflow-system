package com.frwss.system.controller;

import com.frwss.system.model.Receipt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Controller
public class IngestionController {

    @GetMapping("/ingestion")
    public String ingestionPage(){
        return "ingestion/dashboard";
    }

    @GetMapping("/ingestion/upload")
    public String uploadPage(){
        return "ingestion/upload";
    }

//    POST Method
    @PostMapping("/ingestion/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file, Model model) {

        List<com.frwss.system.model.Receipt> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

//            For CSV Files
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader( new InputStreamReader(file.getInputStream(), "UTF-8"));

                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {

                    if(line.startsWith("\uFEFF")){
                        line = line.substring(1);
                    }
                    if(line.trim().isEmpty()) continue;

//                    System.out.println("Line : " + line);

//                    Skip Header
                    if(isFirstLine){
                        isFirstLine = false;
                        continue;
                    }

                    String[] data = line.split(",");

                    Receipt receipt = new Receipt(
                            Integer.parseInt((data[0])),
                            data[1],
                            data[2],
                            Double.parseDouble(data[3]),
                            data[4],
                            data[5]
                    );
                    records.add(receipt);
                }

            }
//            For XLSX Files
            else if (fileName.endsWith(".xlsx")) {

                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                boolean isFirstRow = true;

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    // Skip header row
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

//                        Validation
                        // Validation
                        if (receipt.getAmount() <= 0) {
                            receipt.setValid(false);
                            receipt.setErrorMessage("Invalid amount");
                        }

                        if (receipt.getReferenceNo() == null || receipt.getReferenceNo().isEmpty()) {
                            receipt.setValid(false);
                            receipt.setErrorMessage("Missing reference");
                        }

                        records.add(receipt);

                    } catch (Exception e) {
                        System.out.println("Error reading row: " + e.getMessage());
                    }
                }

                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("records", records);

        return "ingestion/result";
    }


}
