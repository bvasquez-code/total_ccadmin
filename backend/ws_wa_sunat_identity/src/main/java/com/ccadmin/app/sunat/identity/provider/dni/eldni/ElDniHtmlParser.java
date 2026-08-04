package com.ccadmin.app.sunat.identity.provider.dni.eldni;

import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ElDniHtmlParser {

    public Optional<DniIdentityData> parse(String dni, String html) {
        Document document = Jsoup.parse(html == null ? "" : html);

        for (Element row : document.select("table tr")) {
            Elements columns = row.select("td");
            if (columns.size() < 4 || !dni.equals(onlyDigits(columns.get(0).text()))) {
                continue;
            }

            String names = clean(columns.get(1).text());
            String paternalSurname = clean(columns.get(2).text());
            String maternalSurname = clean(columns.get(3).text());
            if (names == null || paternalSurname == null) {
                continue;
            }

            return Optional.of(new DniIdentityData(
                    dni,
                    names,
                    paternalSurname,
                    maternalSurname
            ));
        }

        return Optional.empty();
    }

    private String onlyDigits(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.isBlank() ? null : normalized;
    }
}
