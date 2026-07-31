package com.ccadmin.app.bulkload.model.dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class BulkLoadSourceRowDto {
    public Integer RowNumber;
    public String BusinessKey;
    public Map<String, Object> Payload = new LinkedHashMap<>();

    /**
     * Campos conservados para mantener compatibilidad con los formatos
     * iniciales de precio y stock.
     */
    public String ProductCod;
    public String Value;
}
