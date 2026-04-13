package com.frwss.system.controller;

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

        List<String[]> records = new ArrayList<>();

        try {
            String fileName = file.getOriginalFilename();

//            For CSV Files
            if (fileName.endsWith(".csv")) {

                BufferedReader reader = new BufferedReader( new InputStreamReader(file.getInputStream(), "UTF-8"));

                String line;

                while ((line = reader.readLine()) != null) {

                    if(line.startsWith("\uFEFF")){
                        line = line.substring(1);
                    }
                    if(line.trim().isEmpty()) continue;

//                    System.out.println("Line : " + line);

                    String[] data = line.split(",");
                    records.add(data);
                }

            }
//            For XLSX Files
            else if (fileName.endsWith(".xlsx")) {

//                Apache POI for XLSX Handling
                org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(file.getInputStream());

                org.apache.poi.xssf.usermodel.XSSFSheet sheet = workbook.getSheetAt(0);

                for (org.apache.poi.ss.usermodel.Row row : sheet) {

                    List<String> rowData = new ArrayList<>();

                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        rowData.add(cell.toString());
                    }

                    records.add(rowData.toArray(new String[0]));
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        model.addAttribute("records", records);

        return "ingestion/result";
    }
}
