package com.ccadmin.app.transfer.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.transfer.model.dto.TransferRequestDetSaveDto;
import com.ccadmin.app.transfer.service.TransferRequestDetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/transfers-request-det")
public class TransferRequestDetController {

    @Autowired
    private TransferRequestDetService transferRequestDetService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody TransferRequestDetSaveDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferRequestDetService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("delete")
    public ResponseEntity<ResponseWsDto> delete(@RequestBody TransferRequestDetSaveDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferRequestDetService.delete(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
