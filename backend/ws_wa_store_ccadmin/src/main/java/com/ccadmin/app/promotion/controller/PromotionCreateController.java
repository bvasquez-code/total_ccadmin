package com.ccadmin.app.promotion.controller;

import com.ccadmin.app.promotion.model.dto.PromotionProductRegisterDto;
import com.ccadmin.app.promotion.service.PromotionCreateService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/promotion/create")
public class PromotionCreateController {

    @Autowired
    private PromotionCreateService promotionCreateService;

    @PostMapping("saveForProduct")
    public ResponseEntity<ResponseWsDto> saveForProduct(@RequestBody PromotionProductRegisterDto promoProduct)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.promotionCreateService.save(promoProduct))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
