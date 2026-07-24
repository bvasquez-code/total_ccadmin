package com.ccadmin.app.bulkload.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Resultado generico de la validacion especializada de un tipo de carga.
 * BulkLoad solamente persiste esta estructura sin interpretar su Payload.
 */
public class BulkLoadPreparedDto {
    public List<String> StoreCodList = new ArrayList<>();
    public List<BulkLoadPreparedDetailDto> DetailList = new ArrayList<>();
    public List<BulkLoadErrorDto> ErrorList = new ArrayList<>();
}
