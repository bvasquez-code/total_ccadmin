package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadRegisterDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadCorrectionRequestDto;
import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.bulkload.service.handler.BulkLoadTypeHandlerRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Mantiene la generacion global de codigo fuera de la transaccion de
 * persistencia porque get_cod_seq administra su propio autocommit.
 */
@Service
public class BulkLoadCreateService {
    private final BulkLoadHeadRepository headRepository;
    private final BulkLoadPersistenceService persistenceService;
    private final BulkLoadTypeHandlerRegistry handlerRegistry;

    public BulkLoadCreateService(BulkLoadHeadRepository headRepository,
                                 BulkLoadPersistenceService persistenceService,
                                 BulkLoadTypeHandlerRegistry handlerRegistry) {
        this.headRepository = headRepository;
        this.persistenceService = persistenceService;
        this.handlerRegistry = handlerRegistry;
    }

    public BulkLoadRegisterDto saveParsed(BulkLoadParsedRequestDto request) {
        normalizeRequest(request);
        BulkLoadPreparedDto prepared = prepare(request);
        String code = headRepository.createCode();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException(
                    "No se pudo generar el codigo de carga masiva. Verifique table_sequence"
            );
        }
        return persistenceService.savePrepared(code, request, prepared);
    }

    public BulkLoadRegisterDto correctParsed(BulkLoadCorrectionRequestDto request) {
        if (request == null || request.BulkLoadCod == null
                || request.BulkLoadCod.isBlank()) {
            throw new IllegalArgumentException("Codigo de carga masiva requerido");
        }
        normalizeRequest(request);
        BulkLoadPreparedDto prepared = prepare(request);
        return persistenceService.replacePrepared(
                request.BulkLoadCod.trim(), request, prepared
        );
    }

    private BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        return handlerRegistry.getRequired(request.BulkLoadType)
                .prepare(request);
    }

    private void normalizeRequest(BulkLoadParsedRequestDto request) {
        if (request == null || request.BulkLoadType == null
                || request.BulkLoadType.isBlank()) {
            throw new IllegalArgumentException("Tipo de carga requerido");
        }
        request.BulkLoadType = request.BulkLoadType.trim().toUpperCase(Locale.ROOT);
        if (!BulkLoadConstants.isSupportedType(request.BulkLoadType)) {
            throw new IllegalArgumentException(
                    "Tipo de carga no soportado: " + request.BulkLoadType
            );
        }
        if (request.SchemaVersion != null && request.SchemaVersion != 1) {
            throw new IllegalArgumentException("Version de formato no soportada");
        }
        if (request.RowList == null) request.RowList = new ArrayList<>();
        if (request.StoreList == null) request.StoreList = new ArrayList<>();
    }
}
