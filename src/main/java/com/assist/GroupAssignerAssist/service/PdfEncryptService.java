package com.assist.GroupAssignerAssist.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.assist.GroupAssignerAssist.dto.EncryptedFileResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class PdfEncryptService {

    public List<EncryptedFileResult> encryptMultiplePdfs(List<MultipartFile> files, String password) throws IOException {
        List<EncryptedFileResult> resultList = new ArrayList<>();

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드된 파일이 없습니다.");
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;

            byte[] encryptedBytes = encryptSinglePdf(file, password);
            
            String originalFilename = file.getOriginalFilename();
            String newFilename = formatEncryptedFileName(originalFilename, password);

            resultList.add(new EncryptedFileResult(newFilename, encryptedBytes));
        }

        return resultList;
    }

    private byte[] encryptSinglePdf(MultipartFile file, String password) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getInputStream().readAllBytes());
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            AccessPermission ap = new AccessPermission();
            
            StandardProtectionPolicy spp = new StandardProtectionPolicy(password, password, ap);
            spp.setEncryptionKeyLength(128); // 필요 시 256비트로 조정 가능
            spp.setPermissions(ap);

            document.protect(spp);
            
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String formatEncryptedFileName(String originalFilename, String password) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "encrypted (PW" + password + ").pdf";
        }

        int dotIndex = originalFilename.lastIndexOf(".");
        if (dotIndex != -1) {
            String nameWithoutExt = originalFilename.substring(0, dotIndex);
            String ext = originalFilename.substring(dotIndex);
            return nameWithoutExt + " (PW" + password + ")" + ext;
        } else {
            return originalFilename + " (PW" + password + ").pdf";
        }
    }
}