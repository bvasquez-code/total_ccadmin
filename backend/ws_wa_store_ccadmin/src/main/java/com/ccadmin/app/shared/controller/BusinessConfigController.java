package com.ccadmin.app.shared.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigCreateService;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
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
@RequestMapping("api/v1/business/config")
public class BusinessConfigController {

    @Autowired
    private BusinessConfigSearchService searchService;

    @Autowired
    private BusinessConfigCreateService createService;

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String GroupCod, @RequestParam Integer ConfigCorr) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findById(GroupCod, ConfigCorr)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findByConfigCod")
    public ResponseEntity<ResponseWsDto> findByConfigCod(@RequestParam String GroupCod, @RequestParam String ConfigCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findByConfigCod(GroupCod, ConfigCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findByGroupCod")
    public ResponseEntity<ResponseWsDto> findByGroupCod(@RequestParam String GroupCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findByGroupCod(GroupCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActivesByGroupCod")
    public ResponseEntity<ResponseWsDto> findActivesByGroupCod(@RequestParam String GroupCod) {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findActivesByGroupCod(GroupCod)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page, @RequestParam(required = false) String GroupCod) {
        try {
            if (GroupCod != null && !GroupCod.isEmpty()) {
                return new ResponseEntity<>(new ResponseWsDto(searchService.findAll(Query, Page, GroupCod)), HttpStatus.OK);
            }
            return new ResponseEntity<>(new ResponseWsDto(searchService.findAll(Query, Page)), HttpStatus.OK);
        }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findActives")
    public ResponseEntity<ResponseWsDto> findActives() {
        try { return new ResponseEntity<>(new ResponseWsDto(searchService.findActives()), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam(required = false) String GroupCod, @RequestParam(required = false) Integer ConfigCorr) {
        try { return new ResponseEntity<>(searchService.findDataForm(GroupCod), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody BusinessConfigEntity businessConfig) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.save(businessConfig)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<BusinessConfigEntity> businessConfigList) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.saveAll(businessConfigList)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("enable")
    public ResponseEntity<ResponseWsDto> enable(@RequestBody BusinessConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.enable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }

    @PostMapping("disable")
    public ResponseEntity<ResponseWsDto> disable(@RequestBody BusinessConfigEntity request) {
        try { return new ResponseEntity<>(new ResponseWsDto(createService.disable(request)), HttpStatus.OK); }
        catch (Exception ex) { return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST); }
    }
}
