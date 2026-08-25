package com.ccadmin.app.inventory.controller;

import com.ccadmin.app.inventory.model.dto.*;
import com.ccadmin.app.inventory.service.StockEntryCreateService;
import com.ccadmin.app.inventory.service.StockEntrySearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/stockEntry")
public class StockEntryController {
    private final StockEntryCreateService stockEntryCreateService;
    private final StockEntrySearchService stockEntrySearchService;

    public StockEntryController(
            StockEntryCreateService stockEntryCreateService,
            StockEntrySearchService stockEntrySearchService
    ) {
        this.stockEntryCreateService = stockEntryCreateService;
        this.stockEntrySearchService = stockEntrySearchService;
    }

    @PostMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestBody StockMovementSearchDto request) {
        return execute(() -> stockEntrySearchService.findAll(request));
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String code) {
        return execute(() -> stockEntrySearchService.findById(code));
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(defaultValue = "") String code) {
        try {
            return ResponseEntity.ok(stockEntrySearchService.findDataForm(code));
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody StockEntryRegisterDto request) {
        return execute(() -> stockEntryCreateService.save(request));
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody StockMovementActionDto action) {
        return execute(() -> stockEntryCreateService.confirm(action.Code));
    }

    @PostMapping("quickCreateAndConfirm")
    public ResponseEntity<ResponseWsDto> quickCreateAndConfirm(
            @RequestBody StockEntryQuickCreateDto request
    ) {
        return execute(() -> stockEntryCreateService.createAndConfirmQuick(request));
    }

    @PostMapping("resolve")
    public ResponseEntity<ResponseWsDto> resolve(@RequestBody StockResolutionRequestDto request) {
        return execute(() -> stockEntryCreateService.resolve(request));
    }

    @PostMapping("reject")
    public ResponseEntity<ResponseWsDto> reject(@RequestBody StockMovementActionDto action) {
        return execute(() ->
                stockEntryCreateService.changeStatus(action, StatusConst.REJECTED)
        );
    }

    @PostMapping("cancel")
    public ResponseEntity<ResponseWsDto> cancel(@RequestBody StockMovementActionDto action) {
        return execute(() ->
                stockEntryCreateService.changeStatus(action, StatusConst.CANCELLED)
        );
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
