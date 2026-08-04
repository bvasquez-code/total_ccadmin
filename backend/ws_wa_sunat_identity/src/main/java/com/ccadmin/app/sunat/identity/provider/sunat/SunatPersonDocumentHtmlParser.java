package com.ccadmin.app.sunat.identity.provider.sunat;

import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.RelatedTaxpayerDto;
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
public class SunatPersonDocumentHtmlParser {

    private static final Pattern RUC_TEXT_PATTERN = Pattern.compile("(?i)^RUC\\s*:\\s*(\\d{11})$");

    public PersonIdentityResponseDto parse(
            IdentityDocumentType documentType,
            String documentNumber,
            String html
    ) {
        Document document = Jsoup.parse(html == null ? "" : html);
        Elements elements = document.select("a.aRucs[data-ruc]");
        List<RelatedTaxpayerDto> relatedTaxpayers = new ArrayList<>();

        for (Element element : elements) {
            String ruc = SunatHtmlParserSupport.cleanText(element.attr("data-ruc"));
            Elements headings = element.select("h4.list-group-item-heading");
            if (ruc.isBlank() && !headings.isEmpty()) {
                ruc = extractRuc(headings.getFirst().text());
            }

            String legalName = headings.size() >= 2
                    ? SunatHtmlParserSupport.cleanText(headings.get(1).text())
                    : null;
            String location = null;
            String status = null;

            for (Element paragraph : element.select("p.list-group-item-text")) {
                String text = SunatHtmlParserSupport.cleanText(paragraph.text());
                String normalizedLabel = SunatHtmlParserSupport.normalizeLabel(text);

                if (normalizedLabel.startsWith("ubicacion")) {
                    location = removePrefix(text, "Ubicación");
                } else if (normalizedLabel.startsWith("estado")) {
                    Element statusValue = paragraph.selectFirst("strong, span");
                    status = statusValue == null
                            ? removePrefix(text, "Estado")
                            : SunatHtmlParserSupport.cleanText(statusValue.text());
                }
            }

            relatedTaxpayers.add(new RelatedTaxpayerDto(ruc, legalName, location, status));
        }

        String queryDate = SunatHtmlParserSupport.queryDate(document);
        if (relatedTaxpayers.isEmpty()) {
            return new PersonIdentityResponseDto(
                    false,
                    errorMessage(document),
                    documentType.referenceCode(),
                    documentType.displayName(),
                    documentNumber,
                    0,
                    List.of(),
                    queryDate
            );
        }

        String message = relatedTaxpayers.size() == 1
                ? "Se encontró un contribuyente asociado."
                : "Se encontraron " + relatedTaxpayers.size() + " contribuyentes asociados.";

        return new PersonIdentityResponseDto(
                true,
                message,
                documentType.referenceCode(),
                documentType.displayName(),
                documentNumber,
                relatedTaxpayers.size(),
                List.copyOf(relatedTaxpayers),
                queryDate
        );
    }

    private String extractRuc(String text) {
        if (text == null) {
            return null;
        }

        Matcher matcher = RUC_TEXT_PATTERN.matcher(SunatHtmlParserSupport.cleanText(text));
        return matcher.matches() ? matcher.group(1) : text.replaceAll("\\D", "");
    }

    private String removePrefix(String text, String prefix) {
        if (text == null) {
            return null;
        }

        String result = text.replaceFirst(
                "(?i)^" + Pattern.quote(prefix) + "\\s*:\\s*",
                ""
        );
        result = SunatHtmlParserSupport.cleanText(result);
        return result.isBlank() ? null : result;
    }

    private String errorMessage(Document document) {
        String alertMessage = SunatHtmlParserSupport.alertMessage(document);
        if (alertMessage != null) {
            return alertMessage;
        }

        String normalizedPageText = SunatHtmlParserSupport.normalizeLabel(
                SunatHtmlParserSupport.pageText(document)
        );
        if (normalizedPageText.contains("no se encontraron")) {
            return "No se encontraron contribuyentes asociados al documento.";
        }
        if (normalizedPageText.contains("ingrese el codigo mostrado")) {
            return "SUNAT rechazó o expiró el token de consulta.";
        }
        return "SUNAT respondió, pero no se encontraron contribuyentes asociados al documento.";
    }
}
