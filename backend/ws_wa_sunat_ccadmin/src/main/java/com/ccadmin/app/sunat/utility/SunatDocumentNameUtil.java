package com.ccadmin.app.sunat.utility;

public class SunatDocumentNameUtil {

    public static String fullDocumentNumber(String series, int correlative) {
        return series + "-" + String.format("%08d", correlative);
    }

    public static String xmlName(String issuerRuc, String documentType, String series, int correlative) {
        return baseName(issuerRuc, documentType, series, correlative) + ".xml";
    }

    public static String zipName(String issuerRuc, String documentType, String series, int correlative) {
        return baseName(issuerRuc, documentType, series, correlative) + ".zip";
    }

    public static String baseName(String issuerRuc, String documentType, String series, int correlative) {
        return issuerRuc + "-" + documentType + "-" + fullDocumentNumber(series, correlative);
    }
}
