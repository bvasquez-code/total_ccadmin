package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.model.dto.ProductSearchDto;
import com.ccadmin.app.product.model.dto.ProductSearchRegisterDto;
import com.ccadmin.app.product.service.ProductFindCreateService;
import com.ccadmin.app.product.service.ProductFindSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/productSearch")
public class ProductSearchController {
    @Autowired
    private ProductFindSearchService productFindSearchService;
    @Autowired
    private ProductFindCreateService productFindCreateService;

    @PostMapping("query")
    public ResponseEntity<ResponseWsDto> query(@RequestBody ProductSearchDto productSearch)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productFindSearchService.query(productSearch))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("generateSearch")
    public ResponseEntity<ResponseWsDto> generateSearch(@RequestBody ProductSearchRegisterDto productRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.productFindCreateService.generateSearch(productRegister.ProductCod))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
