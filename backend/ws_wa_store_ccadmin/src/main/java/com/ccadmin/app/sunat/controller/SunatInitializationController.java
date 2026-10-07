package com.ccadmin.app.sunat.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.sunat.service.SunatInitializationCreateService;
import com.ccadmin.app.sunat.service.SunatInitializationSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/sunatInitialization")
public class SunatInitializationController {
    private final SunatInitializationSearchService sunatInitializationSearchService;
    private final SunatInitializationCreateService sunatInitializationCreateService;

    public SunatInitializationController(SunatInitializationSearchService sunatInitializationSearchService,
                                        SunatInitializationCreateService sunatInitializationCreateService) {
        this.sunatInitializationSearchService = sunatInitializationSearchService;
        this.sunatInitializationCreateService = sunatInitializationCreateService;
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm() {
        try { return ResponseEntity.ok(new ResponseWsDto(sunatInitializationSearchService.findDataForm())); }
        catch (Exception exception) { return ResponseEntity.badRequest().body(new ResponseWsDto(exception)); }
    }

    @PostMapping(value = "configure", consumes = "multipart/form-data")
    public ResponseEntity<ResponseWsDto> configure(@RequestPart("configuration") String configuration,
            @RequestPart(value = "certificate", required = false) MultipartFile certificate) {
        try { return ResponseEntity.ok(new ResponseWsDto(sunatInitializationCreateService.configure(configuration, certificate))); }
        catch (Exception exception) { return ResponseEntity.badRequest().body(new ResponseWsDto(exception)); }
    }
}
