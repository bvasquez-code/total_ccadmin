package com.ccadmin.app.inventory.service;

import com.ccadmin.app.inventory.model.constants.StockMovementConstants;
import com.ccadmin.app.inventory.model.dto.StockEntryRegisterDto;
import com.ccadmin.app.inventory.model.dto.StockMovementSearchDto;
import com.ccadmin.app.inventory.model.entity.StockEntryDetEntity;
import com.ccadmin.app.inventory.model.entity.StockEntryHeadEntity;
import com.ccadmin.app.inventory.model.factory.StockEntryRegisterDtoFactory;
import com.ccadmin.app.inventory.repository.StockEntryDetRepository;
import com.ccadmin.app.inventory.repository.StockEntryHeadRepository;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.repository.ProductRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.repository.WarehouseRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockEntrySearchService extends SessionService {
    private static final int PAGE_SIZE = 10;
    private final StockEntryHeadRepository stockEntryHeadRepository;
    private final StockEntryDetRepository stockEntryDetRepository;
    private final BusinessConfigRepository businessConfigRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;

    public StockEntrySearchService(
            StockEntryHeadRepository stockEntryHeadRepository,
            StockEntryDetRepository stockEntryDetRepository,
            BusinessConfigRepository businessConfigRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository
    ) {
        this.stockEntryHeadRepository = stockEntryHeadRepository;
        this.stockEntryDetRepository = stockEntryDetRepository;
        this.businessConfigRepository = businessConfigRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
    }

    public ResponsePageSearchT<StockEntryHeadEntity> findAll(
            StockMovementSearchDto request
    ) {
        String storeCod = getStoreCod();
        String query = clean(request.Query);
        String status = clean(request.ProcessStatus);
        String type = StockMovementConstants.PROCESS_ORIGINAL;
        int page = Math.max(1, request.Page);
        int count = stockEntryHeadRepository.countSearch(
                storeCod, query, status, type, request.DateStart, request.DateEnd
        );
        List<StockEntryHeadEntity> result = stockEntryHeadRepository.search(
                storeCod, query, status, type, request.DateStart, request.DateEnd,
                (page - 1) * PAGE_SIZE, PAGE_SIZE
        );
        result.forEach(stockEntryHead ->
                stockEntryHead.HasPendingResolution =
                        stockEntryDetRepository.countPendingByCode(
                                stockEntryHead.StockEntryCod
                        ) > 0
        );
        return new ResponsePageSearchT<>(result, page, PAGE_SIZE, count);
    }

    public StockEntryRegisterDto findById(String code) {
        StockEntryHeadEntity stockEntryHead = stockEntryHeadRepository.findById(code)
                .orElseThrow(() ->
                        new IllegalArgumentException("No existe la entrada de stock")
                );
        requireStore(stockEntryHead.StoreCod);
        List<StockEntryDetEntity> stockEntryDetails =
                stockEntryDetRepository.findByCode(code);
        populateProductNames(stockEntryDetails);
        return StockEntryRegisterDtoFactory.fromEntities(
                stockEntryHead, stockEntryDetails
        );
    }

    public ResponseWsDto findDataForm(String code) {
        ResponseWsDto response = new ResponseWsDto();
        if (code != null && !code.isBlank()) {
            response.AddResponseAdditional("movement", findById(code));
        }
        response.AddResponseAdditional(
                "reasonList", businessConfigRepository.findActivesByGroupId(8)
        );
        response.AddResponseAdditional(
                "unavailableReasonList",
                businessConfigRepository.findActivesByGroupId(10)
        );
        response.AddResponseAdditional(
                "releaseReasonList",
                businessConfigRepository.findActivesByGroupId(11)
        );
        response.AddResponseAdditional(
                "withdrawReasonList",
                businessConfigRepository.findActivesByGroupId(12)
        );
        response.AddResponseAdditional(
                "warehouseList", warehouseRepository.findByStore(getStoreCod())
        );
        return response.okResponse(null);
    }

    private void populateProductNames(List<StockEntryDetEntity> detailList) {
        Map<String, ProductEntity> productMap = productRepository.findAllById(
                detailList.stream()
                        .map(stockEntryDetail -> stockEntryDetail.ProductCod)
                        .distinct()
                        .toList()
        ).stream().collect(Collectors.toMap(
                product -> product.ProductCod,
                Function.identity()
        ));
        detailList.forEach(stockEntryDetail -> {
            ProductEntity product = productMap.get(stockEntryDetail.ProductCod);
            stockEntryDetail.ProductName = product == null
                    ? stockEntryDetail.ProductCod : product.ProductName;
        });
    }

    private void requireStore(String storeCod) {
        if (!getStoreCod().equals(storeCod)) {
            throw new IllegalArgumentException(
                    "El documento pertenece a otra tienda"
            );
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
