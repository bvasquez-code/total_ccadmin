package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.constants.ReportConstants;
import com.ccadmin.app.shared.model.dto.SearchDto;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/** Filtros del reporte sobre el contrato de búsqueda paginada existente. */
public class ReportSearchDto extends SearchDto {
    public Date DateFrom;
    public Date DateTo;
    public String CurrencyCod;
    public String State;
    public String UserCod;

    private ReportSearchDto(ReportFilterDto filters, String userCod) {
        if (filters == null) {
            throw new IllegalArgumentException("Los filtros son obligatorios");
        }
        String query = normalize(filters.Query);
        CurrencyCod = normalize(filters.CurrencyCod);
        if (filters.Page < 1 || query.length() > ReportConstants.MAX_QUERY_LENGTH
                || CurrencyCod.length() > ReportConstants.MAX_CURRENCY_LENGTH) {
            throw new IllegalArgumentException("Página o filtros no válidos");
        }
        Query = "%" + query.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Page = filters.Page;
        StoreCod = normalize(filters.StoreCod);
        State = normalize(filters.State);
        UserCod = userCod;
    }

    public static ReportSearchDto forPeriod(ReportFilterDto filters, String userCod) {
        ReportSearchDto search = new ReportSearchDto(filters, userCod);
        LocalDate from;
        LocalDate to;
        try {
            from = LocalDate.parse(normalize(filters.DateFrom));
            to = LocalDate.parse(normalize(filters.DateTo));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("Ingrese fechas válidas (aaaa-mm-dd)");
        }
        if (from.isAfter(to) || ChronoUnit.DAYS.between(from, to) > ReportConstants.MAX_DATE_RANGE_DAYS
                || from.getYear() < 1000 || to.getYear() > 9998) {
            throw new IllegalArgumentException("El rango de fechas debe ser ordenado y no superar "
                    + ReportConstants.MAX_DATE_RANGE_DAYS + " días");
        }
        search.DateFrom = Date.valueOf(from);
        search.DateTo = Date.valueOf(to.plusDays(1));
        return search;
    }

    public static ReportSearchDto forStock(ReportFilterDto filters, String userCod) {
        return new ReportSearchDto(filters, userCod);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
