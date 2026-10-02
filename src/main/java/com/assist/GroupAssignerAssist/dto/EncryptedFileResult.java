package com.assist.GroupAssignerAssist.dto;

public class EncryptedFileResult {
    private String fileName;
    private byte[] fileData;

    public EncryptedFileResult(String fileName, byte[] fileData) {
        this.fileName = fileName;
        this.fileData = fileData;
    }

    public String getFileName() {
        return fileName;
    }

    public byte[] getFileData() {
        return fileData;
    }
}