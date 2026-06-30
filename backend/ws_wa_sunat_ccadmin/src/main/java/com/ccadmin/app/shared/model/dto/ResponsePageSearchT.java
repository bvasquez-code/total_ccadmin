package com.ccadmin.app.shared.model.dto;

import java.util.List;

public class ResponsePageSearchT<T> {
    public List<T> List;
    public int Page;
    public int Limit;
    public int TotalRows;
    public int TotalPages;

    public ResponsePageSearchT(List<T> list, int page, int limit, int totalRows) {
        this.List = list;
        this.Page = page;
        this.Limit = limit;
        this.TotalRows = totalRows;
        this.TotalPages = limit <= 0 ? 0 : (int) Math.ceil((double) totalRows / limit);
    }
}
