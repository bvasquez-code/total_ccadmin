package com.ccadmin.app.shared.model.dto;

public class SearchDto {
    public String Query;
    public int Page;
    public int Limit;
    public int Init;
    public String StoreCod;

    public SearchDto(String query, int page) {
        this.Query = query == null ? "" : query;
        this.Page = Math.max(page, 1);
    }

    public void setLimit(int limit) {
        this.Limit = limit;
        this.Init = (this.Page - 1) * this.Limit;
    }
}
