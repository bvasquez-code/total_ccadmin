package com.ccadmin.app.system.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.entity.TableSequenceEntity;
import com.ccadmin.app.system.service.TableSequenceCreateService;
import com.ccadmin.app.system.service.TableSequenceSearchService;
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
@RequestMapping("api/v1/tableSequence")
public class TableSequenceController {

    @Autowired
    private TableSequenceSearchService tableSequenceSearchService;
    @Autowired
    private TableSequenceCreateService tableSequenceCreateService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String SequenceTableType) {
        try { return new ResponseEntity<>(new ResponseWsDto(tableSequenceSearchService.findById(SequenceTableType)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try { return new ResponseEntity<>(new ResponseWsDto(tableSequenceSearchService.findAll(Query, Page)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String SequenceTableType) {
        try { return new ResponseEntity<>(tableSequenceSearchService.findDataForm(SequenceTableType), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody TableSequenceEntity tableSequence) {
        try { return new ResponseEntity<>(new ResponseWsDto(tableSequenceCreateService.save(tableSequence)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
