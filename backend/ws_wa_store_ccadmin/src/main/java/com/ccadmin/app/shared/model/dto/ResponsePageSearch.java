package com.ccadmin.app.shared.model.dto;

public class ResponsePageSearch {

    public Object resultSearch;
    public int TotalPages;
    public int TotalResult;
    public int StarResult;
    public int EndResult;
    public int Page;


    public ResponsePageSearch(Object resultSearch, int Page, int Limit, int TotalResult)
    {
        int PageExtra = ( TotalResult % Limit > 0 ) ? 1 : 0;

        this.resultSearch = resultSearch;
        this.TotalResult = TotalResult;
        this.TotalPages = (TotalResult /  Limit) + PageExtra;
        this.StarResult = (Page - 1) * Limit + 1;
        this.EndResult = (Page - 1) * Limit + Limit;
        this.Page = Page;
    }

}
