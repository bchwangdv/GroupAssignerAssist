package com.assist.GroupAssignerAssist.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.assist.GroupAssignerAssist.dto.EncryptedFileResult;
import com.assist.GroupAssignerAssist.service.PdfEncryptService;

@Controller
@RequestMapping("encrypt")
public class PdfEncryptController {

    private final PdfEncryptService pdfEncryptService;

    public PdfEncryptController(PdfEncryptService pdfEncryptService) {
        this.pdfEncryptService = pdfEncryptService;
    }
    @GetMapping
    public String encryptForm() {
        return "encrypt";
    }

    @PostMapping("process")
    @ResponseBody
    public ResponseEntity<?> processEncrypt(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("password") String password) {

        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("업로드된 파일이 없습니다.");
        }
        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        }

        try {
            List<EncryptedFileResult> results = pdfEncryptService.encryptMultiplePdfs(files, password);

            List<Map<String, String>> responseList = new ArrayList<>();
            for (EncryptedFileResult result : results) {
                Map<String, String> map = new HashMap<>();
                map.put("fileName", result.getFileName());
                map.put("base64Data", Base64.getEncoder().encodeToString(result.getFileData()));
                responseList.add(map);
            }

            return ResponseEntity.ok(responseList);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("PDF 암호화 처리 중 오류가 발생했습니다: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("서버 오류가 발생했습니다.");
        }
    }
    
    /**
     * 전체 ZIP 다운로드 API
     * POST /encrypt/zip
     */
    @PostMapping("zip")
    public ResponseEntity<byte[]> downloadZip(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("password") String password) {

        if (files == null || files.isEmpty() || password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 1. 서비스 호출하여 PDF 암호화 수행
            List<EncryptedFileResult> results = pdfEncryptService.encryptMultiplePdfs(files, password);

            // 2. 메모리 상에서 ZIP 파일 생성
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                for (EncryptedFileResult result : results) {
                    ZipEntry zipEntry = new ZipEntry(result.getFileName());
                    zos.putNextEntry(zipEntry);
                    zos.write(result.getFileData());
                    zos.closeEntry();
                }
            }

            byte[] zipBytes = baos.toByteArray();

            // 3. 한글 파일명 인코딩을 안전하게 처리하는 ContentDisposition 객체 생성
            org.springframework.http.ContentDisposition contentDisposition = 
                    org.springframework.http.ContentDisposition.builder("attachment")
                            .filename("암호화_PDF_목록.zip", java.nio.charset.StandardCharsets.UTF_8)
                            .build();

            // 4. HTTP 헤더 설정 및 ZIP 바이트 배열 반환
            return ResponseEntity.ok()
                    .header("Content-Disposition", contentDisposition.toString())
                    .contentType(MediaType.parseMediaType("application/zip"))
                    .body(zipBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}