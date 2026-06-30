package com.ccadmin.app.sunat.service;

import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SunatZipService {

    public byte[] zip(String xmlFileName, byte[] signedXmlContent) {
        if (xmlFileName == null || xmlFileName.isBlank()) {
            throw new IllegalArgumentException("Nombre de XML firmado requerido");
        }
        if (signedXmlContent == null || signedXmlContent.length == 0) {
            throw new IllegalArgumentException("Contenido de XML firmado requerido");
        }
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(output)) {
                ZipEntry entry = new ZipEntry(xmlFileName);
                zip.putNextEntry(entry);
                zip.write(signedXmlContent);
                zip.closeEntry();
            }
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo generar ZIP SUNAT: " + ex.getMessage(), ex);
        }
    }
}
