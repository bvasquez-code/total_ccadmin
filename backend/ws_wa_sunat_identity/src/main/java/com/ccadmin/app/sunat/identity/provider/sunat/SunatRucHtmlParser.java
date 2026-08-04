package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityDto;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SunatRucHtmlParser {

    private static final Pattern RUC_LEGAL_NAME_PATTERN = Pattern.compile("^(\\d{11})\\s*-\\s*(.+)$");

    public CompanyIdentityResponseDto parse(String requestedRuc, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);
        String completeRuc = textValue(document, "Número de RUC");

        if (completeRuc == null || completeRuc.isBlank()) {
            return new CompanyIdentityResponseDto(false, errorMessage(document), null);
        }

        String ruc = requestedRuc;
        String legalName = null;
        Matcher matcher = RUC_LEGAL_NAME_PATTERN.matcher(completeRuc.trim());
        if (matcher.matches()) {
            ruc = matcher.group(1).trim();
            legalName = matcher.group(2).trim();
        }

        CompanyIdentityDto company = new CompanyIdentityDto(
                ruc,
                legalName,
                textValue(document, "Tipo Contribuyente"),
                textValue(document, "Nombre Comercial"),
                textValue(document, "Fecha de Inscripción"),
                textValue(document, "Fecha de Inicio de Actividades"),
                textValue(document, "Estado del Contribuyente"),
                textValue(document, "Condición del Contribuyente"),
                textValue(document, "Domicilio Fiscal"),
                textValue(document, "Sistema Emisión de Comprobante"),
                textValue(document, "Actividad Comercio Exterior"),
                textValue(document, "Sistema Contabilidad"),
                listValue(document, "Actividad(es) Económica(s)"),
                listValue(document, "Comprobantes de Pago c/aut. de impresión (F. 806 u 816)"),
                listValue(document, "Sistema de Emisión Electrónica"),
                textValue(document, "Emisor electrónico desde"),
                splitCommas(textValue(document, "Comprobantes Electrónicos")),
                textValue(document, "Afiliado al PLE desde"),
                listValue(document, "Padrones"),
                SunatHtmlParserSupport.queryDate(document)
        );

        return new CompanyIdentityResponseDto(
                true,
                "Consulta realizada correctamente.",
                company
        );
    }

    private String textValue(Document document, String label) {
        Element container = findValueContainer(document, label);
        if (container == null) {
            return null;
        }

        String text = SunatHtmlParserSupport.cleanText(container.text());
        return text.isBlank() ? null : text;
    }

    private List<String> listValue(Document document, String label) {
        Element container = findValueContainer(document, label);
        if (container == null) {
            return List.of();
        }

        Elements cells = container.select("table tbody tr td");
        if (cells.isEmpty()) {
            String text = SunatHtmlParserSupport.cleanText(container.text());
            return text.isBlank() ? List.of() : List.of(text);
        }

        List<String> values = new ArrayList<>();
        for (Element cell : cells) {
            String value = SunatHtmlParserSupport.cleanText(cell.text());
            if (!value.isBlank()) {
                values.add(value);
            }
        }
        return List.copyOf(values);
    }

    private Element findValueContainer(Document document, String requestedLabel) {
        String normalizedRequestedLabel = SunatHtmlParserSupport.normalizeLabel(requestedLabel);
        Elements items = document.select(".panel-primary .list-group > .list-group-item");

        for (Element item : items) {
            for (Element row : item.children()) {
                if (!row.hasClass("row")) {
                    continue;
                }

                Elements columns = row.children();
                for (int index = 0; index < columns.size(); index++) {
                    Element heading = columns.get(index).selectFirst("h4.list-group-item-heading");
                    if (heading == null) {
                        continue;
                    }

                    String currentLabel = SunatHtmlParserSupport.normalizeLabel(heading.text());
                    if (currentLabel.equals(normalizedRequestedLabel) && index + 1 < columns.size()) {
                        return columns.get(index + 1);
                    }
                }
            }
        }
        return null;
    }

    private List<String> splitCommas(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Pattern.compile("\\s*,\\s*")
                .splitAsStream(value)
                .map(SunatHtmlParserSupport::cleanText)
                .filter(text -> !text.isBlank())
                .toList();
    }

    private String errorMessage(Document document) {
        String alertMessage = SunatHtmlParserSupport.alertMessage(document);
        if (alertMessage != null) {
            return alertMessage;
        }

        String pageText = SunatHtmlParserSupport.pageText(document);
        String normalizedPageText = SunatHtmlParserSupport.normalizeLabel(pageText);
        if (normalizedPageText.contains("no se encontraron")) {
            return "No se encontraron resultados para el RUC consultado.";
        }
        if (normalizedPageText.contains("ingrese el codigo mostrado")) {
            return "SUNAT rechazó o expiró el token de consulta.";
        }
        if (!pageText.isBlank()) {
            return "SUNAT no devolvió el resultado esperado. Respuesta: "
                    + SunatHtmlParserSupport.limitText(pageText, 300);
        }
        return "SUNAT respondió, pero no fue posible identificar la información del contribuyente.";
    }
}
