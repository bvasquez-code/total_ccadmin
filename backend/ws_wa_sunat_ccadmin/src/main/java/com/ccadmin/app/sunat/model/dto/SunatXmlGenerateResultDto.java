package com.ccadmin.app.sunat.model.dto;

public class SunatXmlGenerateResultDto {
    public String SunatDocumentCod;
    public String FullDocumentNumber;
    public String ElectronicStatus;
    public String XmlFileName;
    public String XmlContent;

    public SunatXmlGenerateResultDto(String sunatDocumentCod, String fullDocumentNumber, String electronicStatus, String xmlFileName, String xmlContent) {
        SunatDocumentCod = sunatDocumentCod;
        FullDocumentNumber = fullDocumentNumber;
        ElectronicStatus = electronicStatus;
        XmlFileName = xmlFileName;
        XmlContent = xmlContent;
    }
}
