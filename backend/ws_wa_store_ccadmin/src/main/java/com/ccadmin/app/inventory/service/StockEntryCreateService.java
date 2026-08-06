package com.ccadmin.app.inventory.service;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.*;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;
import com.ccadmin.app.inventory.repository.StockEntryDetRepository;
import com.ccadmin.app.inventory.repository.StockEntryHeadRepository;
import com.ccadmin.app.product.model.constants.KardexZoneConstants;
import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductVariantEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.model.entity.id.ProductInfoId;
import com.ccadmin.app.product.model.entity.id.ProductInfoWarehouseId;
import com.ccadmin.app.product.model.entity.id.ProductVariantId;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.repository.ProductRepository;
import com.ccadmin.app.product.repository.ProductVariantRepository;
import com.ccadmin.app.product.service.KardexCreateService;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.WarehouseShared;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class StockEntryCreateService extends SessionService {
    private final StockEntryHeadRepository stockEntryHeadRepository;
    private final StockEntryDetRepository stockEntryDetRepository;
    private final StockMovementValidationService stockMovementValidationService;
    private final BusinessConfigRepository businessConfigRepository;
    private final KardexCreateService kardexCreateService;
    private final ProductRepository productRepository;
    private final ProductConfigRepository productConfigRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductInfoRepository productInfoRepository;
    private final ProductInfoWarehouseRepository productInfoWarehouseRepository;
    private final StoreRepository storeRepository;
    private final WarehouseShared warehouseShared;
    private final StockEntrySearchService stockEntrySearchService;

    public StockEntryCreateService(
            StockEntryHeadRepository stockEntryHeadRepository,
            StockEntryDetRepository stockEntryDetRepository,
            StockMovementValidationService stockMovementValidationService,
            BusinessConfigRepository businessConfigRepository,
            KardexCreateService kardexCreateService,
            ProductRepository productRepository,
            ProductConfigRepository productConfigRepository,
            ProductVariantRepository productVariantRepository,
            ProductInfoRepository productInfoRepository,
            ProductInfoWarehouseRepository productInfoWarehouseRepository,
            StoreRepository storeRepository,
            WarehouseShared warehouseShared,
            StockEntrySearchService stockEntrySearchService
    ) {
        this.stockEntryHeadRepository = stockEntryHeadRepository;
        this.stockEntryDetRepository = stockEntryDetRepository;
        this.stockMovementValidationService = stockMovementValidationService;
        this.businessConfigRepository = businessConfigRepository;
        this.kardexCreateService = kardexCreateService;
        this.productRepository = productRepository;
        this.productConfigRepository = productConfigRepository;
        this.productVariantRepository = productVariantRepository;
        this.productInfoRepository = productInfoRepository;
        this.productInfoWarehouseRepository = productInfoWarehouseRepository;
        this.storeRepository = storeRepository;
        this.warehouseShared = warehouseShared;
        this.stockEntrySearchService = stockEntrySearchService;
    }

    @Transactional(rollbackOn = Exception.class)
    public StockEntryRegisterDto save(StockEntryRegisterDto request) {
        requireRequest(request);
        String userCod = getUserCod();
        StockEntryHeadEntity stockEntryHead = request.Head;
        stockEntryHead.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        stockEntryHead.OriginStockEntryCod = null;
        boolean isNew = stockEntryHead.StockEntryCod == null
                || stockEntryHead.StockEntryCod.isBlank();
        if (isNew) {
            stockEntryHead.StockEntryCod =
                    stockEntryHeadRepository.createCode(getStoreCod());
            stockEntryHead.ProcessStatus = StatusConst.PENDING;
        } else {
            StockEntryHeadEntity current = stockEntryHeadRepository.findForUpdate(
                    stockEntryHead.StockEntryCod
            );
            if (current == null) {
                throw new IllegalArgumentException("No existe la entrada de stock");
            }
            requireStore(current.StoreCod);
            if (!StatusConst.PENDING.equals(current.ProcessStatus)) {
                throw new IllegalStateException(
                        "Solo se puede editar un documento pendiente"
                );
            }
            stockEntryHead.CreationUser = current.CreationUser;
            stockEntryHead.CreationDate = current.CreationDate;
            stockEntryHead.ProcessStatus = current.ProcessStatus;
        }
        stockEntryHead.StoreCod = getStoreCod();
        normalizeAndValidate(stockEntryHead, request.DetailList);
        stockEntryDetRepository.deleteByCode(stockEntryHead.StockEntryCod);
        persistEntry(stockEntryHead, request.DetailList, userCod);
        return stockEntrySearchService.findById(stockEntryHead.StockEntryCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockEntryRegisterDto confirm(String code) {
        StockEntryHeadEntity stockEntryHead =
                stockEntryHeadRepository.findForUpdate(code);
        requirePending(stockEntryHead);
        List<StockEntryDetEntity> stockEntryDetails =
                stockEntryDetRepository.findByCode(code);
        confirmEntry(stockEntryHead, stockEntryDetails, getUserCod());
        return stockEntrySearchService.findById(code);
    }

    public String createCode(String storeCod) {
        String code = stockEntryHeadRepository.createCode(storeCod);
        if (code == null || code.isBlank()) {
            throw new IllegalStateException(
                    "No se pudo generar el codigo de entrada de stock para " + storeCod
            );
        }
        return code;
    }

    /**
     * Valida y convierte una carga de stock a la mascara generica de BulkLoad.
     * No persiste la carga ni modifica stock.
     */
    public BulkLoadPreparedDto prepareBulkStockLoad(BulkLoadParsedRequestDto request) {
        StockPreparationContext context = new StockPreparationContext();
        resolveBulkStore(request, context);
        validateBulkStockRows(request, context);

        BulkLoadPreparedDto prepared = new BulkLoadPreparedDto();
        if (context.store != null) prepared.StoreCodList.add(context.store.StoreCod);
        prepared.ErrorList.addAll(context.errorList);
        String storeCod = context.store == null ? null : context.store.StoreCod;

        for (BulkLoadSourceRowDto row : request.RowList) {
            BulkLoadPreparedDetailDto detail = new BulkLoadPreparedDetailDto();
            detail.SourceRowNumber = sourceRow(row.RowNumber);
            detail.StoreCod = storeCod;
            detail.BusinessKey = trimToLength(
                    clean(row.ProductCod) + (storeCod == null ? "" : "|" + storeCod), 128
            );
            detail.Payload.put("ProductCod", clean(row.ProductCod));
            detail.Payload.put("StoreCod", storeCod);
            detail.Payload.put("NumPhysicalStock",
                    context.quantityMap.getOrDefault(row.RowNumber, row.Value));
            detail.Payload.put("Variant", BulkLoadConstants.DEFAULT_VARIANT);
            detail.Payload.put("WarehouseCod",
                    context.warehouse == null ? null : context.warehouse.WarehouseCod);
            ProductConfigEntity config = context.configMap.get(row.RowNumber);
            detail.Payload.put("ProductUnitName",
                    config == null ? "NIU" : config.ProductUnitName);
            detail.Payload.put("ProductUnitFactor",
                    config == null ? 1 : config.ProductUnitFactor);
            detail.ErrorList.addAll(context.errorsFor(row.RowNumber));
            prepared.DetailList.add(detail);
        }
        return prepared;
    }

    /**
     * Crea y confirma una entrada directa originada por una carga masiva.
     * No depende del SecurityContext porque el trabajo se ejecuta en segundo plano.
     */
    @Transactional(rollbackOn = Exception.class)
    public StockEntryBulkResultDto createAndConfirmBulk(
            StockEntryBulkCreateDto request,
            String userCod
    ) {
        validateBulkCreateRequest(request);
        String auditUser = clean(userCod).isEmpty() ? "SISTEMA" : userCod.trim();
        Map<Integer, Integer> referenceByDetailIndex = new LinkedHashMap<>();
        List<StockEntryDetEntity> stockEntryDetails = buildBulkDetails(
                request, referenceByDetailIndex
        );
        StockEntryHeadEntity stockEntryHead = buildBulkHead(request);

        normalizeAndValidate(stockEntryHead, stockEntryDetails);
        persistEntry(stockEntryHead, stockEntryDetails, auditUser);
        confirmEntry(stockEntryHead, stockEntryDetails, auditUser);

        return buildBulkResult(
                stockEntryHead, stockEntryDetails, referenceByDetailIndex
        );
    }

    private void validateBulkCreateRequest(StockEntryBulkCreateDto request) {
        if (request == null || clean(request.StockEntryCod).isEmpty()) {
            throw new IllegalArgumentException(
                    "El codigo de entrada de stock es obligatorio"
            );
        }
        if (clean(request.StoreCod).isEmpty()) {
            throw new IllegalArgumentException("La tienda es obligatoria");
        }
        if (request.DetailList == null || request.DetailList.isEmpty()) {
            throw new IllegalArgumentException(
                    "La entrada de stock no tiene detalles"
            );
        }
    }

    private StockEntryHeadEntity buildBulkHead(StockEntryBulkCreateDto request) {
        StockEntryHeadEntity stockEntryHead = new StockEntryHeadEntity();
        stockEntryHead.StockEntryCod = request.StockEntryCod;
        stockEntryHead.StoreCod = request.StoreCod;
        stockEntryHead.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        stockEntryHead.MovementMode = StockMovementConstants.MODE_DIRECT;
        stockEntryHead.ReasonCode = BulkLoadConstants.STOCK_REASON;
        stockEntryHead.OriginStockEntryCod = null;
        stockEntryHead.ProcessStatus = StatusConst.PENDING;
        stockEntryHead.Observation =
                "Generado por carga masiva " + request.BulkLoadCod;
        return stockEntryHead;
    }

    private List<StockEntryDetEntity> buildBulkDetails(
            StockEntryBulkCreateDto request,
            Map<Integer, Integer> referenceByDetailIndex
    ) {
        List<StockEntryDetEntity> stockEntryDetails = new ArrayList<>();
        for (int index = 0; index < request.DetailList.size(); index++) {
            StockEntryBulkLineDto line = request.DetailList.get(index);
            StockEntryDetEntity stockEntryDetail = new StockEntryDetEntity();
            stockEntryDetail.ProductCod = line.ProductCod;
            stockEntryDetail.Variant = line.Variant;
            stockEntryDetail.WarehouseCod = line.WarehouseCod;
            stockEntryDetail.LotNumber = "";
            stockEntryDetail.ProductUnitName = line.ProductUnitName;
            stockEntryDetail.ProductUnitFactor = line.ProductUnitFactor;
            stockEntryDetail.NumUnit = line.NumUnit;
            stockEntryDetail.Observation = "Carga masiva " + request.BulkLoadCod
                    + ", fila Excel " + line.SourceRowNumber;
            stockEntryDetails.add(stockEntryDetail);
            referenceByDetailIndex.put(index, line.ReferenceItemNumber);
        }
        return stockEntryDetails;
    }

    private StockEntryBulkResultDto buildBulkResult(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails,
            Map<Integer, Integer> referenceByDetailIndex
    ) {
        StockEntryBulkResultDto result = new StockEntryBulkResultDto();
        result.StockEntryCod = stockEntryHead.StockEntryCod;
        for (int index = 0; index < stockEntryDetails.size(); index++) {
            result.ItemNumberByReference.put(
                    referenceByDetailIndex.get(index),
                    stockEntryDetails.get(index).ItemNumber
            );
        }
        return result;
    }

    private void persistEntry(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails,
            String userCod
    ) {
        stockEntryHead.Status = StatusConst.ACTIVE;
        stockEntryHead.addSession(userCod);
        stockEntryHeadRepository.save(stockEntryHead);

        int itemNumber = 1;
        for (StockEntryDetEntity stockEntryDetail : stockEntryDetails) {
            stockEntryDetail.StockEntryCod = stockEntryHead.StockEntryCod;
            stockEntryDetail.ItemNumber = itemNumber++;
            stockEntryDetail.Status = StatusConst.ACTIVE;
            stockEntryDetail.addSession(userCod);
        }
        stockEntryDetRepository.saveAll(stockEntryDetails);
    }

    private void confirmEntry(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails,
            String userCod
    ) {
        validateConfirmableEntry(stockEntryHead, stockEntryDetails);
        List<KardexEntity> kardexMovements = new ArrayList<>();
        List<KardexZoneEntity> kardexZoneMovements = new ArrayList<>();

        applyOriginalConfirmation(
                stockEntryHead,
                stockEntryDetails,
                kardexMovements,
                kardexZoneMovements,
                userCod
        );
        stockEntryDetRepository.saveAll(stockEntryDetails);
        kardexCreateService.saveAll(kardexMovements, kardexZoneMovements);

        stockEntryHead.ProcessStatus = StatusConst.CONFIRMED;
        stockEntryHead.ConfirmUser = userCod;
        stockEntryHead.ConfirmDate = new Date();
        stockEntryHead.addSessionModify(userCod);
        stockEntryHeadRepository.save(stockEntryHead);
    }

    private void validateConfirmableEntry(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails
    ) {
        if (stockEntryHead == null) {
            throw new IllegalArgumentException("No existe la entrada de stock");
        }
        if (!StatusConst.PENDING.equals(stockEntryHead.ProcessStatus)) {
            throw new IllegalStateException(
                    "El documento ya no se encuentra pendiente"
            );
        }
        if (stockEntryDetails == null || stockEntryDetails.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe registrar al menos un producto"
            );
        }
        stockMovementValidationService.requirePhysicalProducts(
                stockEntryDetails.stream()
                        .map(stockEntryDetail -> stockEntryDetail.ProductCod)
                        .toList(),
                stockEntryHead.StoreCod
        );
        if (!StockMovementConstants.PROCESS_ORIGINAL.equals(
                stockEntryHead.ProcessType
        )) {
            throw new IllegalStateException(
                    "No se permiten cabeceras independientes de resolucion"
            );
        }
    }

    private void resolveBulkStore(BulkLoadParsedRequestDto request,
                                  StockPreparationContext context) {
        if (request.StoreList == null || request.StoreList.isEmpty()) {
            context.globalError(bulkError("LOCALES", 0, null, "StoreCod", "",
                    "STORE_REQUIRED", "Debe indicar exactamente un local"));
        } else if (request.StoreList.size() != 1) {
            context.globalError(bulkError("LOCALES", 0, null, "StoreCod",
                    request.StoreList.stream().map(item -> clean(item.StoreCod))
                            .reduce((left, right) -> left + ", " + right).orElse(""),
                    "STOCK_SINGLE_STORE",
                    "La carga de stock requiere exactamente un local"));
        } else {
            BulkLoadStoreRowDto storeRow = request.StoreList.getFirst();
            String storeCod = clean(storeRow.StoreCod).toUpperCase(Locale.ROOT);
            if (BulkLoadConstants.WILDCARD_ALL.equals(storeCod)) {
                context.globalError(bulkError("LOCALES", sourceRow(storeRow.RowNumber),
                        null, "StoreCod", storeCod, "STORE_WILDCARD_NOT_ALLOWED",
                        "La carga de stock no permite el comodin TODOS"));
            } else if (storeCod.isBlank()) {
                context.globalError(bulkError("LOCALES", sourceRow(storeRow.RowNumber),
                        null, "StoreCod", "", "STORE_REQUIRED",
                        "El codigo de local es obligatorio"));
            } else if (storeCod.length() > 4) {
                context.globalError(bulkError("LOCALES", sourceRow(storeRow.RowNumber),
                        storeCod, "StoreCod", storeCod, "STORE_LENGTH",
                        "El codigo de local admite hasta 4 caracteres"));
            } else {
                StoreEntity store = storeRepository.findById(storeCod).orElse(null);
                if (store == null || !StatusConst.ACTIVE.equals(store.Status)) {
                    context.globalError(bulkError("LOCALES", sourceRow(storeRow.RowNumber),
                            storeCod, "StoreCod", storeCod, "STORE_NOT_FOUND",
                            "El local no existe o esta inactivo"));
                } else {
                    context.store = store;
                    try {
                        context.warehouse = warehouseShared.findMainWarehouseByStore(storeCod);
                    } catch (RuntimeException exception) {
                        context.globalError(bulkError("LOCALES",
                                sourceRow(storeRow.RowNumber), storeCod,
                                "StoreCod", storeCod, "MAIN_WAREHOUSE_NOT_FOUND",
                                exception.getMessage()));
                    }
                }
            }
        }
        if (businessConfigRepository.countActiveByGroupIdAndConfigCod(
                8, BulkLoadConstants.STOCK_REASON
        ) == 0) {
            context.globalError(bulkError("PRODUCTO_STOCK", 0, null,
                    "ReasonCode", BulkLoadConstants.STOCK_REASON,
                    "STOCK_REASON_NOT_FOUND",
                    "No existe el motivo activo CARGA_MASIVA_STOCK en el grupo 8"));
        }
    }

    private void validateBulkStockRows(BulkLoadParsedRequestDto request,
                                       StockPreparationContext context) {
        if (request.RowList == null || request.RowList.isEmpty()) {
            context.globalError(bulkError("PRODUCTO_STOCK", 0, null,
                    "ProductCod", "", "ROW_REQUIRED",
                    "El archivo debe contener al menos un producto"));
            return;
        }
        Set<String> seenProductSet = new HashSet<>();
        for (BulkLoadSourceRowDto row : request.RowList) {
            int rowNumber = sourceRow(row.RowNumber);
            String productCod = clean(row.ProductCod);
            if (productCod.isBlank()) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "ProductCod", "", "PRODUCT_REQUIRED",
                        "El codigo de producto es obligatorio"));
            } else if (productCod.length() > 20) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "ProductCod", productCod, "PRODUCT_LENGTH",
                        "El codigo de producto admite hasta 20 caracteres"));
            } else if (!seenProductSet.add(productCod)) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "ProductCod", productCod, "PRODUCT_DUPLICATED",
                        "El producto esta repetido en el archivo"));
            }

            ProductEntity product = productCod.isBlank()
                    ? null : productRepository.findById(productCod).orElse(null);
            if (product == null || !StatusConst.ACTIVE.equals(product.Status)) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "ProductCod", productCod, "PRODUCT_NOT_FOUND",
                        "El producto no existe o esta inactivo"));
            }

            Integer quantity = parseBulkQuantity(row.Value);
            if (quantity == null) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "NumPhysicalStock", row.Value, "STOCK_FORMAT",
                        "La cantidad debe cumplir NUMERO(7): entero entre 1 y 9999999"));
            } else {
                context.quantityMap.put(row.RowNumber, quantity);
            }

            ProductVariantId variantId = new ProductVariantId();
            variantId.ProductCod = productCod;
            variantId.Variant = BulkLoadConstants.DEFAULT_VARIANT;
            ProductVariantEntity variant = productVariantRepository.findById(variantId)
                    .orElse(null);
            if (variant == null || !StatusConst.ACTIVE.equals(variant.Status)) {
                context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                        null, "ProductCod", productCod, "PRODUCT_VARIANT_NOT_FOUND",
                        "El producto no tiene la variante activa 0000"));
            }

            if (context.store != null) {
                String storeCod = context.store.StoreCod;
                ProductConfigEntity config = productConfigRepository.findById(
                        new ProductConfigID(productCod, storeCod)
                ).orElse(null);
                if (config == null || !StatusConst.ACTIVE.equals(config.Status)) {
                    context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                            storeCod, "ProductCod", productCod,
                            "PRODUCT_CONFIG_NOT_FOUND",
                            "El producto no tiene configuracion activa en el local"));
                } else {
                    context.configMap.put(row.RowNumber, config);
                }
                if (!productInfoRepository.existsById(
                        new ProductInfoId(productCod,
                                BulkLoadConstants.DEFAULT_VARIANT, storeCod)
                )) {
                    context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                            storeCod, "ProductCod", productCod,
                            "PRODUCT_STOCK_NOT_FOUND",
                            "El producto no tiene registro de stock en el local"));
                }
                if (context.warehouse != null && !productInfoWarehouseRepository.existsById(
                        new ProductInfoWarehouseId(productCod,
                                BulkLoadConstants.DEFAULT_VARIANT,
                                context.warehouse.WarehouseCod)
                )) {
                    context.rowError(row.RowNumber, bulkError("PRODUCTO_STOCK", rowNumber,
                            storeCod, "ProductCod", productCod,
                            "PRODUCT_WAREHOUSE_STOCK_NOT_FOUND",
                            "El producto no tiene registro de stock en el almacen principal"));
                }
            }
        }
    }

    private Integer parseBulkQuantity(String value) {
        try {
            BigDecimal quantity = new BigDecimal(clean(value).replace(",", "."))
                    .stripTrailingZeros();
            if (quantity.scale() > 0
                    || quantity.compareTo(BigDecimal.ONE) < 0
                    || quantity.compareTo(new BigDecimal("9999999")) > 0) {
                return null;
            }
            return quantity.intValueExact();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private BulkLoadErrorDto bulkError(String sheet, Integer row, String store,
                                       String field, String value,
                                       String code, String detail) {
        return new BulkLoadErrorDto(sheet, row, store, field, value, code, detail);
    }

    private int sourceRow(Integer rowNumber) {
        return rowNumber == null || rowNumber < 1 ? 1 : rowNumber;
    }

    private String trimToLength(String value, int length) {
        String cleanValue = clean(value);
        return cleanValue.length() <= length
                ? cleanValue : cleanValue.substring(0, length);
    }

    private static class StockPreparationContext {
        private StoreEntity store;
        private WarehouseEntity warehouse;
        private final List<BulkLoadErrorDto> errorList = new ArrayList<>();
        private final List<BulkLoadErrorDto> globalErrors = new ArrayList<>();
        private final Map<Integer, List<BulkLoadErrorDto>> rowErrorMap = new HashMap<>();
        private final Map<Integer, Object> quantityMap = new HashMap<>();
        private final Map<Integer, ProductConfigEntity> configMap = new HashMap<>();

        private void globalError(BulkLoadErrorDto error) {
            errorList.add(error);
            globalErrors.add(error);
        }

        private void rowError(Integer row, BulkLoadErrorDto error) {
            errorList.add(error);
            rowErrorMap.computeIfAbsent(keyRow(row), ignored -> new ArrayList<>()).add(error);
        }

        private List<BulkLoadErrorDto> errorsFor(Integer row) {
            List<BulkLoadErrorDto> errors = new ArrayList<>(globalErrors);
            errors.addAll(rowErrorMap.getOrDefault(keyRow(row), List.of()));
            return errors;
        }

        private static int keyRow(Integer row) {
            return row == null ? 0 : row;
        }
    }

    @Transactional(rollbackOn = Exception.class)
    public StockEntryRegisterDto resolve(StockResolutionRequestDto request) {
        if (request == null || request.Code == null || request.Code.isBlank()
                || request.DetailList == null || request.DetailList.isEmpty()) {
            throw new IllegalArgumentException("Debe seleccionar al menos una cantidad para resolver");
        }
        String userCod = getUserCod();
        StockEntryHeadEntity stockEntryHead =
                stockEntryHeadRepository.findForUpdate(request.Code);
        requireResolvable(stockEntryHead);
        List<KardexEntity> kardexMovements = new ArrayList<>();
        List<KardexZoneEntity> kardexZoneMovements = new ArrayList<>();

        for (StockResolutionLineDto line : request.DetailList) {
            StockEntryDetEntity stockEntryDetail =
                    stockEntryDetRepository.findForUpdate(
                            request.Code, line.ItemNumber
                    );
            if (stockEntryDetail == null
                    || stockEntryDetail.OriginStockEntryCod != null) {
                throw new IllegalArgumentException("No existe el item original " + line.ItemNumber);
            }
            int currentVersion = stockEntryDetail.ResolutionVersion == null
                    ? 0 : stockEntryDetail.ResolutionVersion;
            int expectedVersion = line.ResolutionVersion == null ? 0 : line.ResolutionVersion;
            if (currentVersion != expectedVersion) {
                throw new IllegalStateException(
                        "El item " + line.ItemNumber + " ya fue resuelto por otro proceso. Recargue la pagina"
                );
            }
            int quantity = stockMovementValidationService.positive(
                    line.NumUnit, "La cantidad a resolver"
            );
            if (quantity > stockEntryDetail.NumUnitPending) {
                throw new IllegalArgumentException("La cantidad excede el pendiente del item " + line.ItemNumber);
            }
            stockMovementValidationService.validateResolution(
                    line.ResolutionType, line.ResolutionReasonCode, line.Observation, line.NextReviewDate
            );

            int newVersion = currentVersion + 1;
            String event = StockMovementConstants.EVENT_RESOLUTION_PREFIX + newVersion;
            boolean release = StockMovementConstants.RESOLUTION_RELEASE.equals(line.ResolutionType);
            boolean maintain = StockMovementConstants.RESOLUTION_MAINTAIN.equals(line.ResolutionType);

            if (release) {
                stockEntryDetail.NumUnitPending -= quantity;
                stockEntryDetail.NumUnitResolvedIn += quantity;
                stockEntryDetail.ResolvedInReasonCode = line.ResolutionReasonCode;
                kardexZoneMovements.addAll(buildKardexZoneMovements(
                        stockEntryDetail, stockEntryHead,
                        quantity, -quantity, event, userCod
                ));
            } else if (!maintain) {
                stockEntryDetail.NumUnitPending -= quantity;
                stockEntryDetail.NumUnitResolvedOut += quantity;
                stockEntryDetail.ResolvedOutReasonCode = line.ResolutionReasonCode;
                stockEntryDetail.ResolvedOutType = line.ResolutionType;
                kardexMovements.add(buildKardexMovement(
                        stockEntryDetail, stockEntryHead,
                        quantity, false, userCod
                ));
                kardexZoneMovements.addAll(buildKardexZoneMovements(
                        stockEntryDetail, stockEntryHead,
                        0, -quantity, event, userCod
                ));
            }

            stockEntryDetail.ResolutionVersion = newVersion;
            stockEntryDetail.ResolutionType = line.ResolutionType;
            stockEntryDetail.ResolutionReasonCode =
                    maintain ? null : line.ResolutionReasonCode;
            stockEntryDetail.Observation = line.Observation;
            stockEntryDetail.NextReviewDate = line.NextReviewDate;
            stockEntryDetail.addSessionModify(userCod);
            stockEntryDetRepository.save(stockEntryDetail);
        }

        kardexCreateService.saveAll(kardexMovements, kardexZoneMovements);
        stockEntryHead.ResolutionUser = userCod;
        stockEntryHead.ResolutionDate = new Date();
        stockEntryHead.addSessionModify(userCod);
        stockEntryHeadRepository.save(stockEntryHead);
        return stockEntrySearchService.findById(stockEntryHead.StockEntryCod);
    }

    @Transactional(rollbackOn = Exception.class)
    public StockEntryRegisterDto changeStatus(StockMovementActionDto action, String status) {
        StockEntryHeadEntity stockEntryHead =
                stockEntryHeadRepository.findForUpdate(action.Code);
        requirePending(stockEntryHead);
        if (!StatusConst.REJECTED.equals(status) && !StatusConst.CANCELLED.equals(status)) {
            throw new IllegalArgumentException("Estado no soportado");
        }
        stockEntryHead.ProcessStatus = status;
        stockEntryHead.Observation = appendObservation(
                stockEntryHead.Observation, action.Observation
        );
        stockEntryHead.addSessionModify(getUserCod());
        stockEntryHeadRepository.save(stockEntryHead);
        return stockEntrySearchService.findById(action.Code);
    }

    private void applyOriginalConfirmation(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails,
            List<KardexEntity> kardexMovements,
            List<KardexZoneEntity> kardexZoneMovements,
            String userCod
    ) {
        boolean direct = StockMovementConstants.MODE_DIRECT.equals(
                stockEntryHead.MovementMode
        );
        for (StockEntryDetEntity stockEntryDetail : stockEntryDetails) {
            int quantity = stockMovementValidationService.positive(
                    stockEntryDetail.NumUnit, "La cantidad"
            );
            stockEntryDetail.NumUnitPending = direct ? 0 : quantity;
            stockEntryDetail.NumUnitResolvedIn = direct ? quantity : 0;
            stockEntryDetail.NumUnitResolvedOut = 0;
            kardexMovements.add(buildKardexMovement(
                    stockEntryDetail, stockEntryHead, quantity, true, userCod
            ));
            kardexZoneMovements.addAll(buildKardexZoneMovements(
                    stockEntryDetail,
                    stockEntryHead,
                    direct ? quantity : 0,
                    direct ? 0 : quantity,
                    StockMovementConstants.EVENT_CONFIRMATION,
                    userCod
            ));
            stockEntryDetail.addSessionModify(userCod);
        }
    }

    private void normalizeAndValidate(
            StockEntryHeadEntity stockEntryHead,
            List<StockEntryDetEntity> stockEntryDetails
    ) {
        if (stockEntryDetails == null || stockEntryDetails.isEmpty()) {
            throw new IllegalArgumentException(
                    "Debe registrar al menos un producto"
            );
        }
        if (!StockMovementConstants.MODE_DIRECT.equals(stockEntryHead.MovementMode)
                && !StockMovementConstants.MODE_UNAVAILABLE.equals(
                        stockEntryHead.MovementMode
                )) {
            throw new IllegalArgumentException("Seleccione el modo del movimiento");
        }
        stockMovementValidationService.requireReason(
                8, stockEntryHead.ReasonCode, "El motivo de entrada"
        );
        stockEntryHead.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        stockEntryHead.OriginStockEntryCod = null;
        for (StockEntryDetEntity stockEntryDetail : stockEntryDetails) {
            stockMovementValidationService.positive(
                    stockEntryDetail.NumUnit, "La cantidad"
            );
            if (stockEntryDetail.ProductCod == null
                    || stockEntryDetail.ProductCod.isBlank()
                    || stockEntryDetail.Variant == null
                    || stockEntryDetail.WarehouseCod == null) {
                throw new IllegalArgumentException("Producto, variante y almacen son obligatorios");
            }
            stockEntryDetail.ProductUnitFactor =
                    stockEntryDetail.ProductUnitFactor == null
                            || stockEntryDetail.ProductUnitFactor <= 0
                            ? 1 : stockEntryDetail.ProductUnitFactor;
            stockEntryDetail.ProductUnitName =
                    clean(stockEntryDetail.ProductUnitName).isEmpty()
                            ? "UNIDAD" : stockEntryDetail.ProductUnitName;
            stockEntryDetail.LotNumber = clean(stockEntryDetail.LotNumber);
            stockEntryDetail.NumUnitPending = 0;
            stockEntryDetail.NumUnitResolvedIn = 0;
            stockEntryDetail.NumUnitResolvedOut = 0;
            stockEntryDetail.ResolvedInReasonCode = null;
            stockEntryDetail.ResolvedOutReasonCode = null;
            stockEntryDetail.ResolvedOutType = null;
            stockEntryDetail.ResolutionVersion = 0;
            stockEntryDetail.OriginStockEntryCod = null;
            stockEntryDetail.OriginItemNumber = null;
            stockEntryDetail.ResolutionType = null;
            stockEntryDetail.ResolutionReasonCode = null;
            if (StockMovementConstants.MODE_UNAVAILABLE.equals(
                    stockEntryHead.MovementMode
            )) {
                stockMovementValidationService.requireReason(
                        10,
                        stockEntryDetail.UnavailableReasonCode,
                        "El motivo de no disponible"
                );
            } else {
                stockEntryDetail.UnavailableReasonCode = null;
            }
        }
        stockMovementValidationService.requirePhysicalProducts(
                stockEntryDetails.stream()
                        .map(stockEntryDetail -> stockEntryDetail.ProductCod)
                        .toList(),
                stockEntryHead.StoreCod
        );
    }

    private void requireResolvable(StockEntryHeadEntity head) {
        if (head == null) throw new IllegalArgumentException("No existe la entrada de stock");
        requireStore(head.StoreCod);
        if (!StockMovementConstants.PROCESS_ORIGINAL.equals(head.ProcessType)
                || !StockMovementConstants.MODE_UNAVAILABLE.equals(head.MovementMode)
                || !StatusConst.CONFIRMED.equals(head.ProcessStatus)) {
            throw new IllegalStateException(
                    "Solo se puede resolver una entrada confirmada con stock no disponible"
            );
        }
    }

    private KardexEntity buildKardexMovement(
            StockEntryDetEntity stockEntryDetail,
            StockEntryHeadEntity stockEntryHead,
            int quantity,
            boolean addStock,
            String userCod
    ) {
        return KardexEntity.build(
                stockEntryHead.StockEntryCod,
                stockEntryDetail.ItemNumber,
                StockMovementConstants.SOURCE_ENTRY,
                addStock
                        ? KardexZoneConstants.TYPE_OPERATION_ADD
                        : KardexZoneConstants.TYPE_OPERATION_SUBTRACT,
                stockEntryDetail.ProductCod,
                stockEntryDetail.Variant,
                stockEntryHead.StoreCod,
                stockEntryDetail.WarehouseCod,
                quantity,
                stockEntryDetail.LotNumber,
                stockEntryDetail.ExpirationDate,
                addStock ? 2 : 1,
                userCod
        );
    }

    private List<KardexZoneEntity> buildKardexZoneMovements(
            StockEntryDetEntity stockEntryDetail,
            StockEntryHeadEntity stockEntryHead,
            int physicalQuantity,
            int unavailableQuantity,
            String event,
            String userCod
    ) {
        return KardexZoneEntity.buildInventoryMovement(
                stockEntryHead.StockEntryCod,
                stockEntryDetail.ItemNumber,
                StockMovementConstants.SOURCE_ENTRY,
                event,
                stockEntryDetail.ProductCod,
                stockEntryDetail.Variant,
                stockEntryHead.StoreCod,
                stockEntryDetail.WarehouseCod,
                stockEntryDetail.LotNumber,
                stockEntryDetail.ExpirationDate,
                userCod,
                physicalQuantity,
                unavailableQuantity
        );
    }

    private void requireRequest(StockEntryRegisterDto request) {
        if (request == null || request.Head == null) throw new IllegalArgumentException("Documento requerido");
    }

    private void requirePending(StockEntryHeadEntity head) {
        if (head == null) throw new IllegalArgumentException("No existe la entrada de stock");
        requireStore(head.StoreCod);
        if (!StatusConst.PENDING.equals(head.ProcessStatus)) {
            throw new IllegalStateException("El documento ya no se encuentra pendiente");
        }
    }

    private void requireStore(String storeCod) {
        if (!getStoreCod().equals(storeCod)) throw new IllegalArgumentException("El documento pertenece a otra tienda");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String appendObservation(String current, String added) {
        if (added == null || added.isBlank()) return current;
        return (current == null || current.isBlank()) ? added.trim() : current + "\n" + added.trim();
    }
}
