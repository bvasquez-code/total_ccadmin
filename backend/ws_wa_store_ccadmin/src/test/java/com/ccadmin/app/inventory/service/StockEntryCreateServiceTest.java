package com.ccadmin.app.inventory.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkCreateDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkLineDto;
import com.ccadmin.app.inventory.model.dto.StockEntryBulkResultDto;
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
import com.ccadmin.app.product.service.KardexCreateService;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.store.repository.StoreRepository;
import com.ccadmin.app.store.shared.WarehouseShared;
import com.ccadmin.app.user.shared.UserStoreShared;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
