package com.ccadmin.app.shared.model.dto;

public class SearchDto {

    public String Query;
    public int Page;

    public String StoreCod;
    public int Limit;
    public int Init;

    public SearchDto()
    {
        this.Limit = 10;
    }

    public SearchDto(String Query,int Page)
    {
        this.Query = Query;
        this.Page = Page;
        this.Limit = 10;
    }
    public SearchDto(String Query,int Page,String StoreCod)
    {
        this.Query = Query;
        this.Page = Page;
        this.StoreCod =  StoreCod;
        this.Limit = 10;
    }

    public void setLimit(int Limit)
    {
        this.Limit = Limit;
        this.Init = (Page - 1) * this.Limit;
    }

}
