package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;

import java.util.List;

/**
 * Adaptador entre la mascara generica de carga masiva y un dominio concreto.
 */
public interface BulkLoadTypeHandler {
    String type();

    BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request);

    default String prepareResource(String bulkLoadCod) {
        return null;
    }

    void execute(BulkLoadHeadEntity head,
                 List<BulkLoadDetEntity> detailList,
                 String userCod,
                 String resourceCode);
}
