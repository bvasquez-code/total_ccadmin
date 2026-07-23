package com.ccadmin.app.inventory.controller;

import com.ccadmin.app.inventory.model.dto.*;
import com.ccadmin.app.inventory.service.StockEntryService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/stockEntry")
public class StockEntryController {
    private final StockEntryService service;

    public StockEntryController(StockEntryService service) {
        this.service = service;
    }

    @PostMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestBody StockMovementSearchDto request) {
        return execute(() -> service.findAll(request));
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String code) {
        return execute(() -> service.findById(code));
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(defaultValue = "") String code) {
        try {
            return ResponseEntity.ok(service.findDataForm(code));
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody StockEntryRegisterDto request) {
        return execute(() -> service.save(request));
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody StockMovementActionDto action) {
        return execute(() -> service.confirm(action.Code));
    }

    @PostMapping("resolve")
    public ResponseEntity<ResponseWsDto> resolve(@RequestBody StockResolutionRequestDto request) {
        return execute(() -> service.resolve(request));
    }

    @PostMapping("reject")
    public ResponseEntity<ResponseWsDto> reject(@RequestBody StockMovementActionDto action) {
        return execute(() -> service.changeStatus(action, StatusConst.REJECTED));
    }

    @PostMapping("cancel")
    public ResponseEntity<ResponseWsDto> cancel(@RequestBody StockMovementActionDto action) {
        return execute(() -> service.changeStatus(action, StatusConst.CANCELLED));
    }

    private ResponseEntity<ResponseWsDto> execute(Action action) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(action.run()));
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    private interface Action {
        Object run();
    }
}
