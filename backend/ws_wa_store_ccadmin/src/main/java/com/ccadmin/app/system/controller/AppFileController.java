package com.ccadmin.app.system.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.system.model.dto.AppFileDto;
import com.ccadmin.app.system.service.AppFileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("api/v1/appFile")
public class AppFileController {

    @Autowired
    private AppFileService appFileService;


    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody AppFileDto appFileDto)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.appFileService.save(appFileDto)
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

}
