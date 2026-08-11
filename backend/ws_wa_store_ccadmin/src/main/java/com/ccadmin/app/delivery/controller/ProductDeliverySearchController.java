package com.ccadmin.app.delivery.controller;

import com.ccadmin.app.delivery.service.ProductDeliverySearchService;
import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/delivery/productSearch")
public class ProductDeliverySearchController {

    private final ProductDeliverySearchService productDeliverySearchService;

    public ProductDeliverySearchController(ProductDeliverySearchService productDeliverySearchService) {
        this.productDeliverySearchService = productDeliverySearchService;
    }

    @PostMapping("query")
    public ResponseEntity<ResponseWsDto> query(@RequestBody ProductSearchDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(productDeliverySearchService.query(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAvailability")
    public ResponseEntity<ResponseWsDto> findAvailability(
            @RequestParam String ProductCod,
            @RequestParam String StoreCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(productDeliverySearchService.findAvailability(ProductCod, StoreCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDetail")
    public ResponseEntity<ResponseWsDto> findDetail(
            @RequestParam String ProductCod,
            @RequestParam String StoreCod
    ) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(productDeliverySearchService.findDetail(ProductCod, StoreCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
