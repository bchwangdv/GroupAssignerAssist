package com.assist.GroupAssignerAssist.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.assist.GroupAssignerAssist.model.Person;
import com.assist.GroupAssignerAssist.dto.GroupingResult; // DTO 임포트 추가
import com.assist.GroupAssignerAssist.service.GroupAssignerAssistService; // Service 임포트 추가

import jakarta.servlet.http.HttpSession;

@Controller
public class GroupAssignerAssistController {

    // 👉 Service 의존성 주입 (이제 계산은 Service가 담당합니다)
    private final GroupAssignerAssistService groupAssignerAssistService;

    public GroupAssignerAssistController(GroupAssignerAssistService groupAssignerAssistService) {
        this.groupAssignerAssistService = groupAssignerAssistService;
    }

    // 엑셀에서 학생 목록 읽어오기
    public List<Person> loadStudentFromProfile(MultipartFile profile) {
        List<Person> people = new ArrayList<>();

        try (InputStream is = profile.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0); // 첫 번째 시트
            boolean isFirstRow = true;

            for (Row row : sheet) {
                if (isFirstRow) {
                    isFirstRow = false; // 헤더 스킵
                    continue;
                }

                Cell nameCell = row.getCell(0, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
                Cell genderCell = row.getCell(1, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                String name = nameCell.toString().trim();
                String gender = genderCell.toString().trim();

                // 빈 값 스킵
                if (name.isEmpty() || gender.isEmpty()) continue;

                people.add(new Person(name, gender));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return people;
    }

    // 엑셀에서 이전 조 편성 결과 읽어오기
    public List<List<List<String>>> loadPreviousGroupings(MultipartFile file) {
        List<List<List<String>>> result = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(0);
            if (header == null) {
                return result;
            }

            int columnCount = header.getPhysicalNumberOfCells();
            int roundCount = columnCount / 2;

            for (int round = 0; round < roundCount; round++) {
                int groupCol = round * 2;
                int nameCol = groupCol + 1;

                Map<Integer, List<String>> groupMap = new HashMap<>();

                for (int rowIdx = 1; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                    Row row = sheet.getRow(rowIdx);
                    if (row == null) continue;

                    Cell groupCell = row.getCell(groupCol);
                    Cell nameCell = row.getCell(nameCol);
                    if (groupCell == null || nameCell == null) continue;

                    String name = nameCell.toString().trim();
                    if (name.isEmpty()) continue;

                    int groupNum;
                    try {
                        groupNum = (int) groupCell.getNumericCellValue();
                    } catch (Exception e) {
                        continue;
                    }

                    groupMap.computeIfAbsent(groupNum, k -> new ArrayList<>()).add(name);
                }

                List<Integer> sortedKeys = new ArrayList<>(groupMap.keySet());
                Collections.sort(sortedKeys);
                List<List<String>> groups = new ArrayList<>();
                for (Integer key : sortedKeys) {
                    groups.add(groupMap.get(key));
                }

                result.add(groups);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    // 👉 핵심 비즈니스 로직 호출 부 (깔끔하게 정리됨)
    @PostMapping("result")
    public String result(
            @RequestParam("numGroups") int numGroups,
            @RequestParam("profile") MultipartFile profile,
            @RequestParam(value = "criteria", required = false) List<String> criteria,
            @RequestParam(value = "previousGroups", required = false) MultipartFile previousGroupFile,
            HttpSession session,
            Model model) {

        List<Person> people = loadStudentFromProfile(profile);
        List<List<List<String>>> previousGroupings = new ArrayList<>();

        if (criteria != null && criteria.contains("previous") && previousGroupFile != null && !previousGroupFile.isEmpty()) {
            previousGroupings = loadPreviousGroupings(previousGroupFile);
        }

        // Service에 데이터 전달 및 결과(DTO) 받기
        GroupingResult resultData = groupAssignerAssistService.assignBestGrouping(
                people,
                numGroups,
                criteria != null ? criteria : Collections.emptyList(),
                previousGroupings
        );

        // View(Thymeleaf 등)로 전달할 데이터 세팅
        model.addAttribute("numGroups", numGroups);
        model.addAttribute("bestGroups", resultData.getGroups());
        model.addAttribute("overlapCount", resultData.getOverlapCount()); // 겹친 횟수 알림용
        model.addAttribute("overlappingPairs", resultData.getOverlappingPairs()); // 👉 추가: 이름 조합 목록

        // 엑셀 다운로드를 위해 세션에 임시 저장 (추후 고유 ID 캐시 방식으로 개선 권장)
        session.setAttribute("bestGroups", resultData.getGroups());

        return "result";
    }

    // 엑셀 다운로드 (기존 로직 유지)
    @GetMapping("downloadExcel")
    public ResponseEntity<byte[]> downloadExcel(HttpSession session) {

        @SuppressWarnings("unchecked")
        List<List<Person>> bestGroups = (List<List<Person>>) session.getAttribute("bestGroups");

        if (bestGroups == null || bestGroups.isEmpty()) {
            return ResponseEntity.badRequest().body("No groups to export.".getBytes(StandardCharsets.UTF_8));
        }

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("조편성 결과");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 12);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setAlignment(HorizontalAlignment.CENTER);
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Group", "Name", "Gender"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (int i = 0; i < bestGroups.size(); i++) {
                List<Person> group = bestGroups.get(i);
                int groupNumber = i + 1;

                int groupStartRow = rowIdx; 

                for (Person p : group) {
                    Row row = sheet.createRow(rowIdx++);
                    row.createCell(0).setCellValue(groupNumber);
                    row.createCell(1).setCellValue(p.getName());
                    row.createCell(2).setCellValue(p.getGender());

                    for (int j = 0; j < columns.length; j++) {
                        row.getCell(j).setCellStyle(dataStyle);
                    }
                }

                if (group.size() > 1) {
                    sheet.addMergedRegion(new CellRangeAddress(
                        groupStartRow,        
                        rowIdx - 1,           
                        0,                    
                        0                     
                    ));
                }
            }

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            byte[] excelBytes = bos.toByteArray();

            String filename = URLEncoder.encode("조편성_결과.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(excelBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("엑셀 생성 오류".getBytes(StandardCharsets.UTF_8));
        }
    }

    // 템플릿 파일 다운로드 (기존 로직 유지)
    @GetMapping("downloadProfileTemplate")
    public ResponseEntity<byte[]> downloadProfileTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/profile.xlsx");

        byte[] fileData;
        try (InputStream inputStream = resource.getInputStream()) {
            fileData = inputStream.readAllBytes();
        }

        String filename = URLEncoder.encode("profile.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileData);
    }

    @GetMapping("downloadPreTemplate")
    public ResponseEntity<byte[]> downloadPreTemplate() throws IOException {
        ClassPathResource resource = new ClassPathResource("static/previous.xlsx");

        byte[] fileData;
        try (InputStream inputStream = resource.getInputStream()) {
            fileData = inputStream.readAllBytes();
        }

        String filename = URLEncoder.encode("previous.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(fileData);
    }
}