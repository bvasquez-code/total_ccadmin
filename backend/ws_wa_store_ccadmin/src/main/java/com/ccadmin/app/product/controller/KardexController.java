package com.ccadmin.app.product.controller;

import com.ccadmin.app.product.service.KardexService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/kardex")
public class KardexController {

    @Autowired
    private KardexService kardexService;

    @GetMapping("regularizeAllKardex")
    public ResponseEntity<ResponseWsDto> regularizeAllKardex() {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.kardexService.regularizeAllKardex()
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query,int Page,String StoreCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.kardexService.findAll(Query,Page,StoreCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
