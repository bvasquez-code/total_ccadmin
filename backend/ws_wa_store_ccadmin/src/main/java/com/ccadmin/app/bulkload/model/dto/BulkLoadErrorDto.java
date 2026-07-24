package com.ccadmin.app.bulkload.model.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class BulkLoadErrorDto {
    public String Sheet;
    public Integer RowNumber;
    public String StoreCod;
    public String Field;
    public String Value;
    public String ErrorCode;
    public String ErrorDetail;
    public String WarningDetail;

    public BulkLoadErrorDto(String sheet, Integer rowNumber, String storeCod, String field,
                            String value, String errorCode, String errorDetail) {
        this.Sheet = sheet;
        this.RowNumber = rowNumber;
        this.StoreCod = storeCod;
        this.Field = field;
        this.Value = value;
        this.ErrorCode = errorCode;
        this.ErrorDetail = errorDetail;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("Sheet", Sheet);
        result.put("RowNumber", RowNumber);
        result.put("StoreCod", StoreCod);
        result.put("Field", Field);
        result.put("Value", Value);
        result.put("ErrorCode", ErrorCode);
        result.put("ErrorDetail", ErrorDetail);
        if (WarningDetail != null && !WarningDetail.isBlank()) {
            result.put("WarningDetail", WarningDetail);
        }
        return result;
    }
}
