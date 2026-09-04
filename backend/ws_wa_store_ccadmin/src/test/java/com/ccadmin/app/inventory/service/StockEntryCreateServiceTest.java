package com.ccadmin.app.inventory.service;

import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadSourceRowDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadStoreRowDto;
import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
import com.ccadmin.app.inventory.model.dto.StockEntryQuickCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryRegisterDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;
import com.ccadmin.app.inventory.repository.StockEntryDetRepository;
import com.ccadmin.app.inventory.repository.StockEntryHeadRepository;
import com.ccadmin.app.product.repository.ProductConfigRepository;
import com.ccadmin.app.product.repository.ProductInfoRepository;
import com.ccadmin.app.product.repository.ProductInfoWarehouseRepository;
import com.ccadmin.app.product.repository.ProductRepository;
import com.ccadmin.app.product.repository.ProductVariantRepository;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.model.entity.ProductVariantEntity;
import com.ccadmin.app.product.model.entity.id.ProductConfigID;
import com.ccadmin.app.product.service.KardexCreateService;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockEntryCreateServiceTest {

    @Mock
    private StockEntryHeadRepository stockEntryHeadRepository;
    @Mock
    private StockEntryDetRepository stockEntryDetRepository;
    @Mock
    private StockMovementValidationService stockMovementValidationService;
    @Mock
    private BusinessConfigRepository businessConfigRepository;
    @Mock
    private KardexCreateService kardexCreateService;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductConfigRepository productConfigRepository;
    @Mock
    private ProductVariantRepository productVariantRepository;
    @Mock
    private ProductInfoRepository productInfoRepository;
    @Mock
    private ProductInfoWarehouseRepository productInfoWarehouseRepository;
    @Mock
    private StoreRepository storeRepository;
    @Mock
    private WarehouseShared warehouseShared;
    @Mock
    private StockEntrySearchService stockEntrySearchService;
    @Mock
    private UserStoreShared userStoreShared;
    @InjectMocks
    private StockEntryCreateService stockEntryCreateService;

    @BeforeEach
    void injectSessionDependency() {
        ReflectionTestUtils.setField(
                stockEntryCreateService, "userStoreShared", userStoreShared
        );
    }

    @Test
    void bulkCreationUsesTheSameListConfirmationFlow() {
        when(stockMovementValidationService.positive(7, "La cantidad"))
                .thenReturn(7);

        StockEntryBulkCreateDto request = new StockEntryBulkCreateDto();
        request.StockEntryCod = "IET0010001000001";
        request.StoreCod = "T001";
        request.BulkLoadCod = "CM00000000000001";
        StockEntryBulkLineDto line = new StockEntryBulkLineDto();
        line.ReferenceItemNumber = 25;
        line.SourceRowNumber = 4;
        line.ProductCod = "TEC008";
        line.Variant = "0000";
        line.WarehouseCod = "T0010001";
        line.ProductUnitName = "NIU";
        line.ProductUnitFactor = 1;
        line.NumUnit = 7;
        request.DetailList.add(line);

        StockEntryBulkResultDto result =
                stockEntryCreateService.createAndConfirmBulk(request, "BVASQUEZ");

        assertEquals(request.StockEntryCod, result.StockEntryCod);
        assertEquals(1, result.ItemNumberByReference.get(25));
        verify(stockEntryHeadRepository, times(2))
                .save(org.mockito.ArgumentMatchers.argThat(stockEntryHead ->
                        StatusConst.CONFIRMED.equals(stockEntryHead.ProcessStatus)
                                && "BVASQUEZ".equals(stockEntryHead.ConfirmUser)
                ));
        verify(stockEntryDetRepository, times(2)).saveAll(anyList());
        verify(kardexCreateService).saveAll(anyList(), anyList());
    }

    @Test
    void regularConfirmationUsesTheSameConfirmationResult() {
        StockEntryHeadEntity stockEntryHead = new StockEntryHeadEntity();
        stockEntryHead.StockEntryCod = "IET0010001000002";
        stockEntryHead.StoreCod = "T001";
        stockEntryHead.ProcessType = StockMovementConstants.PROCESS_ORIGINAL;
        stockEntryHead.MovementMode = StockMovementConstants.MODE_DIRECT;
        stockEntryHead.ProcessStatus = StatusConst.PENDING;
        StockEntryDetEntity stockEntryDetail = new StockEntryDetEntity();
        stockEntryDetail.StockEntryCod = stockEntryHead.StockEntryCod;
        stockEntryDetail.ItemNumber = 1;
        stockEntryDetail.ProductCod = "TEC008";
        stockEntryDetail.Variant = "0000";
        stockEntryDetail.WarehouseCod = "T0010001";
        stockEntryDetail.NumUnit = 7;
        StockEntryRegisterDto expected = new StockEntryRegisterDto();
        expected.Head = stockEntryHead;
        expected.DetailList = List.of(stockEntryDetail);

        when(userStoreShared.getMainStore("SISTEMA")).thenReturn("T001");
        when(stockEntryHeadRepository.findForUpdate(stockEntryHead.StockEntryCod))
                .thenReturn(stockEntryHead);
        when(stockEntryDetRepository.findByCode(stockEntryHead.StockEntryCod))
                .thenReturn(List.of(stockEntryDetail));
        when(stockMovementValidationService.positive(7, "La cantidad"))
                .thenReturn(7);
        when(stockEntrySearchService.findById(stockEntryHead.StockEntryCod))
                .thenReturn(expected);

        StockEntryRegisterDto result =
                stockEntryCreateService.confirm(stockEntryHead.StockEntryCod);

        assertEquals(expected, result);
        assertEquals(StatusConst.CONFIRMED, stockEntryHead.ProcessStatus);
        assertEquals("SISTEMA", stockEntryHead.ConfirmUser);
        assertEquals(7, stockEntryDetail.NumUnitResolvedIn);
        verify(stockEntryDetRepository).saveAll(List.of(stockEntryDetail));
        verify(kardexCreateService).saveAll(anyList(), anyList());
    }

    @Test
    void quickCreationBuildsAndConfirmsADirectInitialStoreEntry() {
        StockEntryQuickCreateDto request = new StockEntryQuickCreateDto();
        request.ProductCod = "00000035";
        request.Quantity = 7;
        request.NumUnitPrice = new BigDecimal("12.50");
        request.LotNumber = "LOTE-QUICK";
        request.ExpirationDate = Date.valueOf("2027-10-30");

        ProductEntity product = new ProductEntity();
        product.ProductCod = request.ProductCod;
        product.ProductName = "Producto rapido";
        ProductConfigEntity productConfig = new ProductConfigEntity();
        productConfig.ProductCod = request.ProductCod;
        productConfig.StoreCod = "T001";
        productConfig.ProductUnitName = "NIU";
        productConfig.ProductUnitFactor = 1;
        ProductVariantEntity productVariant = new ProductVariantEntity(
                request.ProductCod
        );
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.WarehouseCod = "T0010001";
        StockEntryRegisterDto expected = new StockEntryRegisterDto();

        when(userStoreShared.getMainStore("SISTEMA")).thenReturn("T001");
        when(stockMovementValidationService.positive(7, "La cantidad"))
                .thenReturn(7);
        when(productRepository.findById(request.ProductCod))
                .thenReturn(Optional.of(product));
        when(productConfigRepository.findById(
                org.mockito.ArgumentMatchers.any(ProductConfigID.class)
        )).thenReturn(Optional.of(productConfig));
        when(productVariantRepository.findAllVariantProduct(request.ProductCod))
                .thenReturn(List.of(productVariant));
        when(warehouseShared.findMainWarehouseByStore("T001"))
                .thenReturn(warehouse);
        when(stockEntryHeadRepository.createCode("T001"))
                .thenReturn("IET0010001000003");
        when(stockEntrySearchService.findById("IET0010001000003"))
                .thenReturn(expected);

        StockEntryRegisterDto result =
                stockEntryCreateService.createAndConfirmQuick(request);

        assertEquals(expected, result);
        verify(stockEntryHeadRepository, times(2)).save(
                org.mockito.ArgumentMatchers.argThat(head ->
                        StockMovementConstants.MODE_DIRECT.equals(head.MovementMode)
                                && StockMovementConstants.INITIAL_STORE_LOAD_REASON
                                .equals(head.ReasonCode)
                                && StatusConst.CONFIRMED.equals(head.ProcessStatus)
                )
        );
        verify(stockEntryDetRepository, times(2)).saveAll(
                org.mockito.ArgumentMatchers.argThat(details -> {
                    StockEntryDetEntity detail = details.iterator().next();
                    return request.ProductCod.equals(detail.ProductCod)
                            && warehouse.WarehouseCod.equals(detail.WarehouseCod)
                            && detail.NumUnit == 7
                            && request.NumUnitPrice.equals(detail.NumUnitPrice)
                            && request.LotNumber.equals(detail.LotNumber)
                            && request.ExpirationDate.equals(detail.ExpirationDate);
                })
        );
        verify(kardexCreateService).saveAll(anyList(), anyList());
    }

    @Test
    void bulkPreparationKeepsOptionalCostLotAndExpirationDate() {
        BulkLoadParsedRequestDto request = new BulkLoadParsedRequestDto();
        BulkLoadSourceRowDto row = new BulkLoadSourceRowDto();
        row.RowNumber = 4;
        row.ProductCod = "TEC008";
        row.Value = "7";
        row.Payload.put("NumUnitPrice", "12.50");
        row.Payload.put("LotNumber", " LOTE-001 ");
        row.Payload.put("ExpirationDate", "30/10/2027");
        request.RowList.add(row);
        BulkLoadSourceRowDto rowWithoutOptionalValues =
                new BulkLoadSourceRowDto();
        rowWithoutOptionalValues.RowNumber = 5;
        rowWithoutOptionalValues.ProductCod = "TEC009";
        rowWithoutOptionalValues.Value = "3";
        request.RowList.add(rowWithoutOptionalValues);
        BulkLoadStoreRowDto storeRow = new BulkLoadStoreRowDto();
        storeRow.RowNumber = 4;
        storeRow.StoreCod = "T001";
        request.StoreList.add(storeRow);

        com.ccadmin.app.store.model.entity.StoreEntity store =
                new com.ccadmin.app.store.model.entity.StoreEntity();
        store.StoreCod = "T001";
        WarehouseEntity warehouse = new WarehouseEntity();
        warehouse.WarehouseCod = "T0010001";
        ProductEntity product = new ProductEntity();
        product.ProductCod = row.ProductCod;
        ProductVariantEntity variant = new ProductVariantEntity(row.ProductCod);
        ProductConfigEntity productConfig = new ProductConfigEntity();
        productConfig.ProductCod = row.ProductCod;
        productConfig.StoreCod = store.StoreCod;
        productConfig.ProductUnitName = "NIU";
        productConfig.ProductUnitFactor = 1;

        when(businessConfigRepository.countActiveByGroupIdAndConfigCod(
                8, "CARGA_MASIVA_STOCK"
        )).thenReturn(1);
        when(storeRepository.findById("T001")).thenReturn(Optional.of(store));
        when(warehouseShared.findMainWarehouseByStore("T001"))
                .thenReturn(warehouse);
        when(productRepository.findById(
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(Optional.of(product));
        when(productVariantRepository.findById(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(Optional.of(variant));
        when(productConfigRepository.findById(
                org.mockito.ArgumentMatchers.any(ProductConfigID.class)
        )).thenReturn(Optional.of(productConfig));
        when(productInfoRepository.existsById(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);
        when(productInfoWarehouseRepository.existsById(
                org.mockito.ArgumentMatchers.any()
        )).thenReturn(true);

        BulkLoadPreparedDto result =
                stockEntryCreateService.prepareBulkStockLoad(request);

        assertEquals(0, result.ErrorList.size());
        assertEquals(new BigDecimal("12.50"),
                result.DetailList.getFirst().Payload.get("NumUnitPrice"));
        assertEquals("LOTE-001",
                result.DetailList.getFirst().Payload.get("LotNumber"));
        assertEquals("2027-10-30",
                result.DetailList.getFirst().Payload.get("ExpirationDate"));
        assertEquals(new BigDecimal("0.00"),
                result.DetailList.get(1).Payload.get("NumUnitPrice"));
        assertNull(result.DetailList.get(1).Payload.get("LotNumber"));
        assertNull(result.DetailList.get(1).Payload.get("ExpirationDate"));
    }
}
