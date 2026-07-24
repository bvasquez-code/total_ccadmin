package com.ccadmin.app.bulkload.model.dto;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BulkLoadPreparedDetailDto {
    public Integer SourceRowNumber;
    public String StoreCod;
    public String BusinessKey;
    public Map<String, Object> Payload = new LinkedHashMap<>();
    public List<BulkLoadErrorDto> ErrorList = new ArrayList<>();
    public List<BulkLoadErrorDto> WarningList = new ArrayList<>();
}
