package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.service.ProductInfoStockSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/productInfoStock")
public class ProductInfoStockController {

    private final ProductInfoStockSearchService productInfoStockSearchService;

    public ProductInfoStockController(ProductInfoStockSearchService productInfoStockSearchService) {
        this.productInfoStockSearchService = productInfoStockSearchService;
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(
            @RequestParam(name = "Query", defaultValue = "") String query,
            @RequestParam(name = "Page", defaultValue = "1") int page,
            @RequestParam(name = "StoreCod", defaultValue = "") String storeCod
    ) {
        try {
            SearchDto search = new SearchDto(query, page, storeCod);
            return new ResponseEntity<>(
                    new ResponseWsDto(this.productInfoStockSearchService.findAll(search)),
                    HttpStatus.OK
            );
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        }
    }
}
