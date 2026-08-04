package com.ccadmin.app.sunat.identity.provider.sunat;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.text.Normalizer;
import java.util.Locale;

final class SunatHtmlParserSupport {

    private SunatHtmlParserSupport() {
    }

    static String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }

        String withoutAccents = Normalizer.normalize(label, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    static String queryDate(Document document) {
        Element element = document.selectFirst(".panel-footer small");
        if (element == null) {
            return null;
        }

        String text = cleanText(element.text());
        String value = text.replaceFirst("(?i)^Fecha\\s+consulta\\s*:\\s*", "").trim();
        return value.isBlank() ? null : value;
    }

    static String alertMessage(Document document) {
        Element alert = document.selectFirst(
                ".alert-danger,.alert-warning,.alert-info,.alert,.error,.mensaje,[role=alert]"
        );
        if (alert == null) {
            return null;
        }

        String message = cleanText(alert.text());
        return message.isBlank() ? null : message;
    }

    static String pageText(Document document) {
        return document.body() == null ? "" : cleanText(document.body().text());
    }

    static String limitText(String text, int maximumLength) {
        String clean = cleanText(text);
        return clean.length() <= maximumLength
                ? clean
                : clean.substring(0, maximumLength) + "...";
    }
}
