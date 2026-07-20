package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.KardexZoneDto;
import com.ccadmin.app.product.model.dto.KardexZoneSearchDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.product.repository.KardexZoneRepository;
import com.ccadmin.app.product.shared.ProductShared;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KardexZoneSearchService {

    private static final int PAGE_SIZE = 10;

    @Autowired
    private KardexZoneRepository kardexZoneRepository;
    @Autowired
    private ProductShared productShared;

    public ResponsePageSearch findAll(KardexZoneSearchDto search) {
        this.normalize(search);

        int totalResult = this.kardexZoneRepository.countSearch(
                search.Query,
                search.StoreCod,
                search.ZoneStockMoved,
                search.TypeOperation
        );
        int totalPages = Math.max(1, (int) Math.ceil((double) totalResult / PAGE_SIZE));
        search.Page = Math.min(search.Page, totalPages);
        int init = (search.Page - 1) * PAGE_SIZE;
        List<KardexZoneEntity> movementList = totalResult == 0
                ? List.of()
                : this.kardexZoneRepository.search(
                        search.Query,
                        search.StoreCod,
                        search.ZoneStockMoved,
                        search.TypeOperation,
                        init,
                        PAGE_SIZE
                );

        List<String> productCodList = movementList.stream()
                .map(movement -> movement.ProductCod)
                .distinct()
                .toList();
        Map<String, ProductEntity> productByCode = productCodList.isEmpty()
                ? Map.of()
                : this.productShared.findAllById(productCodList).stream()
                        .collect(Collectors.toMap(product -> product.ProductCod, Function.identity()));

        List<KardexZoneDto> result = movementList.stream()
                .map(movement -> new KardexZoneDto(
                        movement,
                        productByCode.get(movement.ProductCod)
                ))
                .toList();

        ResponsePageSearch response = new ResponsePageSearch(result, search.Page, PAGE_SIZE, totalResult);
        response.StarResult = totalResult == 0 ? 0 : init + 1;
        response.EndResult = Math.min(init + result.size(), totalResult);
        return response;
    }

    private void normalize(KardexZoneSearchDto search) {
        if (search == null) {
            throw new IllegalArgumentException("Los filtros de kardex por zona son obligatorios");
        }
        search.Query = this.text(search.Query);
        search.StoreCod = this.text(search.StoreCod);
        search.ZoneStockMoved = this.text(search.ZoneStockMoved);
        search.TypeOperation = this.text(search.TypeOperation);
        search.Page = Math.max(search.Page, 1);

        if (search.StoreCod.isBlank()) {
            throw new IllegalArgumentException("StoreCod es obligatorio");
        }
    }

    private String text(String value) {
        return value == null ? "" : value.trim();
    }
}
