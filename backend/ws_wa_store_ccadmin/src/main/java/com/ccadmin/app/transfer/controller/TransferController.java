package com.ccadmin.app.transfer.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.transfer.model.dto.*;
import com.ccadmin.app.transfer.service.TransferCreateService;
import com.ccadmin.app.transfer.service.TransferSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/transfers")
public class TransferController {

    @Autowired
    private TransferCreateService transferCreateService;
    @Autowired
    private TransferSearchService transferSearchService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody TransferRegisterBundleDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferCreateService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("{transferCod}")
    public ResponseEntity<ResponseWsDto> delete(@PathVariable String transferCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferCreateService.delete(transferCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String transferCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto().okResponse(this.transferSearchService.findByTransferCod(transferCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("register-bundle")
    public ResponseEntity<ResponseWsDto> registerBundle(@RequestBody TransferRegisterBundleDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferCreateService.save(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("dispatch")
    public ResponseEntity<ResponseWsDto> dispatch(@RequestBody TransferDispatchDto request) {
        try {
            return new ResponseEntity<>(
                    this.transferCreateService.dispatchTransfer(request),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("receive")
    public ResponseEntity<ResponseWsDto> receive(@RequestBody TransferReceiveDto request) {
        try {
            return new ResponseEntity<>(
                    this.transferCreateService.receiveTransfer(request),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("reject")
    public ResponseEntity<ResponseWsDto> reject(@RequestBody TransferReceiveDto request) {
        try {
            return new ResponseEntity<>(
                    this.transferCreateService.rejectTransfer(request),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("cancel")
    public ResponseEntity<ResponseWsDto> cancel(@RequestBody TransferReceiveDto request) {
        try {
            return new ResponseEntity<>(
                    this.transferCreateService.cancelTransfer(request),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("approved")
    public ResponseEntity<ResponseWsDto> approved(@RequestBody TransferReceiveDto request) {
        try {
            return new ResponseEntity<>(
                        this.transferCreateService.approvedTransfer(request),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String transferCod) {
        try {
            return new ResponseEntity<>(
                    this.transferSearchService.findDataForm(transferCod),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataPrint")
    public ResponseEntity<ResponseWsDto> findDataPrint(@RequestParam String transferCod) {
        try {
            return new ResponseEntity<>(
                    this.transferSearchService.findDataPrint(transferCod),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("createCode")
    public ResponseEntity<ResponseWsDto> createCode(@RequestParam String storeCod){
        try{
            return new ResponseEntity<>(
                    new ResponseWsDto().okResponse(this.transferCreateService.createCode(storeCod))
                    ,HttpStatus.OK
            );
        }catch (Exception ex){
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
    
    @PostMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestBody TransferSearchDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferSearchService.findAll(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveDet")
    public ResponseEntity<ResponseWsDto> saveDet(@RequestBody TransferDetRegisterMassiveDto request) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.transferCreateService.saveDet(request)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
