package com.ccadmin.app.shared.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import com.ccadmin.app.shared.service.BusinessConfigGroupCreateService;
import com.ccadmin.app.shared.service.BusinessConfigGroupSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/business/config/group")
public class BusinessConfigGroupController {

    @Autowired
    private BusinessConfigGroupSearchService searchService;

    @Autowired
    private BusinessConfigGroupCreateService createService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String GroupCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findById(GroupCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findByGroupId")
    public ResponseEntity<ResponseWsDto> findByGroupId(@RequestParam Integer GroupId) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findByGroupId(GroupId)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findAll(Query, Page)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findActives()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String GroupCod) {
        try { return new ResponseEntity<>(searchService.findDataForm(GroupCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody BusinessConfigGroupEntity businessConfigGroup) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.save(businessConfigGroup)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<BusinessConfigGroupEntity> businessConfigGroupList) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.saveAll(businessConfigGroupList)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestBody BusinessConfigGroupEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.enable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody BusinessConfigGroupEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
