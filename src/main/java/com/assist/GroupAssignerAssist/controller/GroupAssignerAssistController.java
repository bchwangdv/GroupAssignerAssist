package com.assist.GroupAssignerAssist.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

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

import jakarta.servlet.http.HttpSession;


@Controller
public class GroupAssignerAssistController {
	private static final int TRIAL_COUNT = 1000; // 조편성 시도 횟수
	
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
	            Cell jobCell = row.getCell(2, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

	            String name = nameCell.toString().trim();
	            String gender = genderCell.toString().trim();
	            String job = jobCell.toString().trim();

	            // 빈 값 스킵
	            if (name.isEmpty() || gender.isEmpty() || job.isEmpty()) continue;

	            people.add(new Person(name, gender, job));
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    return people;
	}
	
    public static List<List<Person>> splitIntoGroup(List<Person> people, int numGroups) {

        List<Person> shuffled = new ArrayList<>(people);
        Collections.shuffle(shuffled);
        List<List<Person>> groups = new ArrayList<>();
        for (int i = 0; i < numGroups; i++) {
            groups.add(new ArrayList<>());
        }

        for (int i = 0; i < shuffled.size(); i++) {
           groups.get(i % numGroups).add(shuffled.get(i));
        }

        return groups;
    }
    
    public static int evaluateGroupScore(List<List<Person>> groups) {
        int totalMale = 0;
        int totalFemale = 0;

        // 전체 성비 계산
        for (List<Person> group : groups) {
            for (Person p : group) {
                if (p.getGender().equals("M")) totalMale++;
                else if (p.getGender().equals("F")) totalFemale++;
            }
        }

        int numGroups = groups.size();
        double idealMalePerGroup = (double) totalMale / numGroups;
        double idealFemalePerGroup = (double) totalFemale / numGroups;

        int score = 0;

        for (List<Person> group : groups) {
            int maleCount = 0;
            int femaleCount = 0;

            for (Person p : group) {
                if (p.getGender().equals("M")) maleCount++;
                else if (p.getGender().equals("F")) femaleCount++;
            }

            // 이상적인 수와의 차이를 줄일수록 높은 점수
            score -= Math.abs(maleCount - idealMalePerGroup);
            score -= Math.abs(femaleCount - idealFemalePerGroup);
        }

        return score;
    }

    public static List<List<Person>> assignBestGrouping(List<Person> people, int numGroups, int trialCount) {

        int bestScore = Integer.MIN_VALUE;
        List<List<Person>> bestGroups= null;

        for (int t = 0; t < trialCount; t++) {

            List<List<Person>> groups = splitIntoGroup(people, numGroups);
            int score = evaluateGroupScore(groups);
            if(score > bestScore) {
                bestScore = score;
                bestGroups = groups;
            }
        }
        if (bestGroups != null) {
            for (List<Person> group : bestGroups) {
                group.sort(Comparator.comparing(Person::getName));
            }
        }
        return bestGroups;
    }
    
	@PostMapping("result")
	public String result(
		@RequestParam("numGroups") int numGroups,
		@RequestParam("profile") MultipartFile profile,
		HttpSession session,
		Model model) {
		
		int trialCount = TRIAL_COUNT;
		
		List<Person> people = loadStudentFromProfile(profile);
		List<List<Person>> bestGroups = assignBestGrouping(people, numGroups, trialCount);
		
		model.addAttribute("numGroups", numGroups);
		model.addAttribute("bestGroups", bestGroups);
		
		session.setAttribute("bestGroups", bestGroups); // 다운로드를 위한 세션 저장
		return "result";
	}
	
	@GetMapping("downloadExcel")
	public ResponseEntity<byte[]> downloadExcel(HttpSession session) {

	    @SuppressWarnings("unchecked")
	    List<List<Person>> bestGroups = (List<List<Person>>) session.getAttribute("bestGroups");

	    if (bestGroups == null || bestGroups.isEmpty()) {
	        return ResponseEntity.badRequest().body("No groups to export.".getBytes(StandardCharsets.UTF_8));
	    }

	    try (Workbook workbook = new XSSFWorkbook()) {
	        Sheet sheet = workbook.createSheet("조편성 결과");

	        // 👉 헤더 스타일: 진한 보라색 + 흰 글씨 + 가운데 정렬
	        CellStyle headerStyle = workbook.createCellStyle();
	        Font headerFont = workbook.createFont();
	        headerFont.setBold(true);
	        headerFont.setFontHeightInPoints((short) 12);
	        headerFont.setColor(IndexedColors.WHITE.getIndex());
	        headerStyle.setFont(headerFont);
	        headerStyle.setFillForegroundColor(IndexedColors.VIOLET.getIndex()); // 진한 보라색
	        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
	        headerStyle.setAlignment(HorizontalAlignment.CENTER);
	        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	        headerStyle.setBorderBottom(BorderStyle.THIN);
	        headerStyle.setBorderTop(BorderStyle.THIN);
	        headerStyle.setBorderLeft(BorderStyle.THIN);
	        headerStyle.setBorderRight(BorderStyle.THIN);

	        // 👉 데이터 스타일: 가운데 정렬 + 테두리
	        CellStyle dataStyle = workbook.createCellStyle();
	        dataStyle.setAlignment(HorizontalAlignment.CENTER);
	        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
	        dataStyle.setBorderBottom(BorderStyle.THIN);
	        dataStyle.setBorderTop(BorderStyle.THIN);
	        dataStyle.setBorderLeft(BorderStyle.THIN);
	        dataStyle.setBorderRight(BorderStyle.THIN);

	        // 👉 헤더 작성
	        Row headerRow = sheet.createRow(0);
	        String[] columns = {"Group", "Name", "Gender"};
	        for (int i = 0; i < columns.length; i++) {
	            Cell cell = headerRow.createCell(i);
	            cell.setCellValue(columns[i]);
	            cell.setCellStyle(headerStyle);
	        }

	        // 👉 데이터 작성
	        int rowIdx = 1;
	        for (int i = 0; i < bestGroups.size(); i++) {
	            List<Person> group = bestGroups.get(i);
	            int groupNumber = i + 1;

	            int groupStartRow = rowIdx; // 그룹 시작 위치 기억

	            for (Person p : group) {
	                Row row = sheet.createRow(rowIdx++);
	                row.createCell(0).setCellValue(groupNumber);
	                row.createCell(1).setCellValue(p.getName());
	                row.createCell(2).setCellValue(p.getGender());

	                for (int j = 0; j < columns.length; j++) {
	                    row.getCell(j).setCellStyle(dataStyle);
	                }
	            }

	            // 그룹번호 셀 병합 (1개 초과인 경우만)
	            if (group.size() > 1) {
	                sheet.addMergedRegion(new CellRangeAddress(
	                    groupStartRow,        // 시작 행
	                    rowIdx - 1,           // 끝 행
	                    0,                    // 시작 열 (Group 컬럼)
	                    0                     // 끝 열
	                ));
	            }
	        }

	        // 👉 완성된 XLSX를 ByteArray로 변환
	        ByteArrayOutputStream bos = new ByteArrayOutputStream();
	        workbook.write(bos);
	        byte[] excelBytes = bos.toByteArray();
	        
	        String filename = URLEncoder.encode("조편성_결과.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");

	        return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
	            .contentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
	            .body(excelBytes);

	    } catch (Exception e) {
	        e.printStackTrace();
	        return ResponseEntity.internalServerError().body("엑셀 생성 오류".getBytes(StandardCharsets.UTF_8));
	    }
	}
    
	@GetMapping("downloadProfileTemplate")
	public ResponseEntity<byte[]> downloadProfileTemplate() throws IOException {
	    ClassPathResource resource = new ClassPathResource("static/profile.xlsx");

	    // InputStream을 통해 파일 내용을 읽어들임
	    byte[] fileData;
	    try (InputStream inputStream = resource.getInputStream()) {
	        fileData = inputStream.readAllBytes();
	    }

	    String filename = URLEncoder.encode("profile.xlsx", StandardCharsets.UTF_8)
	                                 .replaceAll("\\+", "%20");

	    return ResponseEntity.ok()
	            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
	            .contentType(MediaType.parseMediaType(
	                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
	            .body(fileData);
	}
}
