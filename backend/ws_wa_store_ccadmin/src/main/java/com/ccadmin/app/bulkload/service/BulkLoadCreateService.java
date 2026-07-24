package com.ccadmin.app.bulkload.service;

import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadRegisterDto;
import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.repository.BulkLoadHeadRepository;
import com.ccadmin.app.inventory.service.StockEntryCreateService;
import com.ccadmin.app.product.service.ProductConfigCreateService;
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
    private final ProductConfigCreateService productConfigCreateService;
    private final StockEntryCreateService stockEntryCreateService;

    public BulkLoadCreateService(BulkLoadHeadRepository headRepository,
                                 BulkLoadPersistenceService persistenceService,
                                 ProductConfigCreateService productConfigCreateService,
                                 StockEntryCreateService stockEntryCreateService) {
        this.headRepository = headRepository;
        this.persistenceService = persistenceService;
        this.productConfigCreateService = productConfigCreateService;
        this.stockEntryCreateService = stockEntryCreateService;
    }

    public BulkLoadRegisterDto saveParsed(BulkLoadParsedRequestDto request) {
        normalizeRequest(request);
        BulkLoadPreparedDto prepared = switch (request.BulkLoadType) {
            case BulkLoadConstants.TYPE_PRODUCT_PRICE ->
                    productConfigCreateService.prepareBulkPriceLoad(request);
            case BulkLoadConstants.TYPE_STOCK_ENTRY ->
                    stockEntryCreateService.prepareBulkStockLoad(request);
            default -> throw new IllegalArgumentException(
                    "Tipo de carga no soportado: " + request.BulkLoadType
            );
        };
        String code = headRepository.createCode();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException(
                    "No se pudo generar el codigo de carga masiva. Verifique table_sequence"
            );
        }
        return persistenceService.savePrepared(code, request, prepared);
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
