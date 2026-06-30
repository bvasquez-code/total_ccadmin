package com.ccadmin.app.sunat.service;

import com.ccadmin.app.sunat.model.constants.SunatElectronicStatusConst;
import com.ccadmin.app.sunat.model.constants.SunatFileTypeConst;
import com.ccadmin.app.sunat.model.dto.SunatCdrResultDto;
import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;
import com.ccadmin.app.sunat.model.entity.SunatDocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.StringJoiner;
import java.util.zip.ZipInputStream;

@Service
public class SunatCdrProcessService {

    @Autowired
    private SunatFileStorageService sunatFileStorageService;

    public SunatCdrResultDto processBase64Cdr(SunatConfigEntity config, SunatDocumentEntity document, String cdrBase64) {
        if (cdrBase64 == null || cdrBase64.isBlank()) {
            throw new IllegalArgumentException("CDR SUNAT requerido");
        }
        byte[] cdrZip = Base64.getDecoder().decode(cdrBase64);
        String cdrZipFileName = "R-" + document.IssuerRuc + "-" + document.SunatDocumentType + "-" + document.FullDocumentNumber + ".zip";
        this.sunatFileStorageService.saveZip(config, document, SunatFileTypeConst.CDR_ZIP, cdrZipFileName, cdrZip);

        ExtractedXml extractedXml = extractXml(cdrZip);
        this.sunatFileStorageService.saveText(
                config,
                document,
                SunatFileTypeConst.CDR_XML,
                extractedXml.fileName,
                "application/xml",
                extractedXml.xml
        );

        SunatCdrResultDto result = parseCdr(extractedXml.xml);
        result.CdrZipFileName = cdrZipFileName;
        result.CdrXmlFileName = extractedXml.fileName;
        return result;
    }

    private ExtractedXml extractXml(byte[] cdrZip) {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(cdrZip))) {
            var entry = zip.getNextEntry();
            while (entry != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".xml")) {
                    byte[] content = zip.readAllBytes();
                    return new ExtractedXml(entry.getName(), new String(content, StandardCharsets.UTF_8));
                }
                entry = zip.getNextEntry();
            }
            throw new IllegalArgumentException("CDR no contiene XML");
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo extraer XML del CDR: " + ex.getMessage(), ex);
        }
    }

    private SunatCdrResultDto parseCdr(String xml) {
        try {
            Document document = parseXml(xml);
            SunatCdrResultDto result = new SunatCdrResultDto();
            result.ResponseCode = firstText(document, "ResponseCode");
            result.Description = firstText(document, "Description");
            result.Observations = allText(document, "Note");
            result.Accepted = "0".equals(result.ResponseCode);
            result.AcceptedWithObservations = result.Accepted && result.Observations != null && !result.Observations.isBlank();
            result.Rejected = !result.Accepted;
            if (result.AcceptedWithObservations) {
                result.ElectronicStatus = SunatElectronicStatusConst.ACEPTADO_OBSERVADO;
            } else if (result.Accepted) {
                result.ElectronicStatus = SunatElectronicStatusConst.ACEPTADO;
            } else {
                result.ElectronicStatus = SunatElectronicStatusConst.RECHAZADO;
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("No se pudo leer XML CDR: " + ex.getMessage(), ex);
        }
    }

    private Document parseXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private String firstText(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        return nodes.item(0).getTextContent();
    }

    private String allText(Document document, String localName) {
        var nodes = document.getElementsByTagNameNS("*", localName);
        StringJoiner joiner = new StringJoiner("\n");
        for (int i = 0; i < nodes.getLength(); i++) {
            String text = nodes.item(i).getTextContent();
            if (text != null && !text.isBlank()) {
                joiner.add(text);
            }
        }
        String value = joiner.toString();
        return value.isBlank() ? null : value;
    }

    private record ExtractedXml(String fileName, String xml) {
    }
}
