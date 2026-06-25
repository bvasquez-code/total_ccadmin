package com.ccadmin.app.shared.model.dto;

import java.util.ArrayList;
import java.util.List;

public class ResponsePageSearchT<T> {

    public List<T> resultSearch;
    public int TotalPages;
    public int TotalResult;
    public int StarResult;
    public int EndResult;
    public int Page;

    public ResponsePageSearchT(){

    }

    public ResponsePageSearchT(List<T> resultSearch, int Page, int Limit, int TotalResult)
    {
        int PageExtra = ( TotalResult % Limit > 0 ) ? 1 : 0;

        this.resultSearch = resultSearch;
        this.TotalResult = TotalResult;
        this.TotalPages = (TotalResult /  Limit) + PageExtra;
        this.StarResult = (Page - 1) * Limit + 1;
        this.EndResult = (Page - 1) * Limit + Limit;
        this.Page = Page;
    }

    public ResponsePageSearchT(T oneSearch,int Page, int Limit)
    {
        int TotalResult = 1;
        int PageExtra = ( TotalResult % Limit > 0 ) ? 1 : 0;

        this.resultSearch = new ArrayList<>();
        this.resultSearch.add(oneSearch);
        this.TotalResult = TotalResult;
        this.TotalPages = (TotalResult /  Limit) + PageExtra;
        this.StarResult = (Page - 1) * Limit + 1;
        this.EndResult = (Page - 1) * Limit + Limit;
        this.Page = Page;
    }

    public <R> ResponsePageSearchT<T> clone(List<T> resultSearch,ResponsePageSearchT<R> origin){
        this.resultSearch = resultSearch;
        this.TotalPages = origin.TotalPages;
        this.TotalResult = origin.TotalResult;
        this.StarResult = origin.StarResult;
        this.EndResult = origin.EndResult;
        this.Page = origin.Page;
        return this;
    }
}
