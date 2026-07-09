package com.ccadmin.app.system.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.entity.StoreSequenceEntity;
import com.ccadmin.app.system.service.StoreSequenceCreateService;
import com.ccadmin.app.system.service.StoreSequenceSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/storeSequence")
public class StoreSequenceController {

    @Autowired
    private StoreSequenceSearchService storeSequenceSearchService;
    @Autowired
    private StoreSequenceCreateService storeSequenceCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String StoreCod,
                                                  @RequestParam Integer PeriodId,
                                                  @RequestParam String SequenceTableType) {
        try { return new ResponseEntity<>(new ResponseWsDto(storeSequenceSearchService.findById(StoreCod, PeriodId, SequenceTableType)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query,
                                                 @RequestParam int Page,
                                                 @RequestParam(required = false) String StoreCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(storeSequenceSearchService.findAll(Query, Page, StoreCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String StoreCod,
                                                      @RequestParam(required = false) Integer PeriodId,
                                                      @RequestParam(required = false) String SequenceTableType) {
        try { return new ResponseEntity<>(storeSequenceSearchService.findDataForm(StoreCod, PeriodId, SequenceTableType), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody StoreSequenceEntity storeSequence) {
        try { return new ResponseEntity<>(new ResponseWsDto(storeSequenceCreateService.save(storeSequence)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
