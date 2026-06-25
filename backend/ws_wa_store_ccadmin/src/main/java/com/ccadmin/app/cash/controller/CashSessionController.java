package com.ccadmin.app.cash.controller;

import com.ccadmin.app.cash.model.dto.CloseRequestDto;
import com.ccadmin.app.cash.model.dto.OpenRequestDto;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;
import com.ccadmin.app.cash.model.entity.CashSessionItemEntity;
import com.ccadmin.app.cash.service.CashSessionAdminService;
import com.ccadmin.app.cash.service.CashSessionItemService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("api/v1/cash/session")
public class CashSessionController {

    @Autowired
    private CashSessionAdminService cashSessionAdminService;

    @Autowired
    private CashSessionItemService itemService;


    // ========= Open =========
    @PostMapping("open")
    public ResponseEntity<ResponseWsDto> open(@RequestBody OpenRequestDto req) {
        try {
            CashSessionEntity s = cashSessionAdminService.open(
                    req.RegisterCod, req.StoreCod, req.CurrencyCod, req.Commenter, req.OpeningFloatAmount
            );
            return new ResponseEntity<>(new ResponseWsDto(s), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    // ========= Items =========
    @PostMapping("item/add")
    public ResponseEntity<ResponseWsDto> addItem(@RequestBody CashSessionItemEntity item) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(itemService.addItem(item)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("item/addAll")
    public ResponseEntity<ResponseWsDto> addItems(@RequestBody List<CashSessionItemEntity> items) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(itemService.addItems(items)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("items")
    public ResponseEntity<ResponseWsDto> getItems(@RequestParam Long CashSessionID) {
        try {
            return new ResponseEntity<>(new ResponseWsDto(itemService.getItems(CashSessionID)), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    // ========= Close =========
    @PostMapping("close")
    public ResponseEntity<ResponseWsDto> close(@RequestBody CloseRequestDto req) {
        try {
            CashSessionEntity s = cashSessionAdminService.close(req.CashSessionID, req.Commenter);
            return new ResponseEntity<>(new ResponseWsDto(s), HttpStatus.OK);
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

}

