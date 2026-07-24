package com.ccadmin.app.bulkload.controller;

import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.bulkload.service.BulkLoadCommandService;
import com.ccadmin.app.bulkload.service.BulkLoadCreateService;
import com.ccadmin.app.bulkload.service.BulkLoadSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/bulkLoad")
public class BulkLoadController {
    private final BulkLoadCreateService createService;
    private final BulkLoadSearchService searchService;
    private final BulkLoadCommandService commandService;

    public BulkLoadController(BulkLoadCreateService createService,
                              BulkLoadSearchService searchService,
                              BulkLoadCommandService commandService) {
        this.createService = createService;
        this.searchService = searchService;
        this.commandService = commandService;
    }

    @PostMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestBody BulkLoadSearchDto request) {
        return execute(() -> searchService.findAll(request));
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String code) {
        return execute(() -> searchService.findById(code));
    }

    @PostMapping("findDetails")
    public ResponseEntity<ResponseWsDto> findDetails(
            @RequestBody BulkLoadDetailSearchDto request
    ) {
        return execute(() -> searchService.findDetails(request));
    }

    @PostMapping("saveParsed")
    public ResponseEntity<ResponseWsDto> saveParsed(
            @RequestBody BulkLoadParsedRequestDto request
    ) {
        return execute(() -> createService.saveParsed(request));
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody BulkLoadActionDto request) {
        return execute(() -> commandService.confirm(request.Code));
    }

    @PostMapping("cancel")
    public ResponseEntity<ResponseWsDto> cancel(@RequestBody BulkLoadActionDto request) {
        return execute(() -> commandService.cancel(request.Code));
    }

    private ResponseEntity<ResponseWsDto> execute(Action action) {
        try {
            return ResponseEntity.ok(new ResponseWsDto(action.run()));
        } catch (Exception exception) {
            return new ResponseEntity<>(new ResponseWsDto(exception), HttpStatus.BAD_REQUEST);
        }
    }

    private interface Action {
        Object run();
    }
}
