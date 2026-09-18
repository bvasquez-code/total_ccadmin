package com.ccadmin.app.report.model.dto;

public class ReportFilterDto {
    public String DateFrom;
    public String DateTo;
    public String StoreCod;
    public String CurrencyCod;
    public String Query;
    public String State;
    public int Page = 1;

    public ReportFilterDto() {
    }

    public ReportFilterDto(String dateFrom, String dateTo, String storeCod, String currencyCod,
                           String query, String state, int page) {
        this.DateFrom = dateFrom;
        this.DateTo = dateTo;
        this.StoreCod = storeCod;
        this.CurrencyCod = currencyCod;
        this.Query = query;
        this.State = state;
        this.Page = page;
    }
}
