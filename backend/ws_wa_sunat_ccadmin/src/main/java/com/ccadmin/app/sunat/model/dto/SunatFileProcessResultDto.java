package com.ccadmin.app.sunat.model.dto;

public class SunatFileProcessResultDto {
    public String SunatDocumentCod;
    public String ElectronicStatus;
    public String FileType;
    public String FileName;
    public String FilePath;
    public Long SizeBytes;
    public String Sha256Hash;

    public SunatFileProcessResultDto(String sunatDocumentCod, String electronicStatus, String fileType,
                                     String fileName, String filePath, Long sizeBytes, String sha256Hash) {
        SunatDocumentCod = sunatDocumentCod;
        ElectronicStatus = electronicStatus;
        FileType = fileType;
        FileName = fileName;
        FilePath = filePath;
        SizeBytes = sizeBytes;
        Sha256Hash = sha256Hash;
    }
}
