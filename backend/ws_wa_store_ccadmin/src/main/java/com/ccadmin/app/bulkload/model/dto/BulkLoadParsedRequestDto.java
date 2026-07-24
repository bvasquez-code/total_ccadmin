package com.ccadmin.app.bulkload.model.dto;

import java.util.ArrayList;
import java.util.List;

public class BulkLoadParsedRequestDto {
    public String BulkLoadType;
    public Integer SchemaVersion = 1;
    public String OriginalFileName;
    public List<BulkLoadSourceRowDto> RowList = new ArrayList<>();
    public List<BulkLoadStoreRowDto> StoreList = new ArrayList<>();
}
