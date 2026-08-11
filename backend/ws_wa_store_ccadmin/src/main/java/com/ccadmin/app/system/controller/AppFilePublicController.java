package com.ccadmin.app.system.controller;

import com.ccadmin.app.system.model.dto.AppFileResourceDto;
import com.ccadmin.app.system.service.AppFileService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("api/v1/public/appFile")
public class AppFilePublicController {

    private final AppFileService appFileService;

    public AppFilePublicController(AppFileService appFileService) {
        this.appFileService = appFileService;
    }

    @GetMapping("{FileCod}")
    public ResponseEntity<?> findById(@PathVariable String FileCod) {
        try {
            AppFileResourceDto file = appFileService.findResource(FileCod);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.ContentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic())
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.FileName + "\"")
                    .body(file.Resource);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }
}
