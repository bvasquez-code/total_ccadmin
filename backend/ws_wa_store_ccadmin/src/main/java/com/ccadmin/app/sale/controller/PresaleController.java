package com.ccadmin.app.sale.controller;

import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.service.PresaleCreateService;
import com.ccadmin.app.sale.service.PresaleSearchService;
import com.ccadmin.app.sale.service.ExpiredSaleCancellationService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/presale")
public class PresaleController {

    public static Logger log = LogManager.getLogger(PresaleController.class);
    @Autowired
    private PresaleCreateService presaleCreateService;
    @Autowired
    private PresaleSearchService presaleSearchService;
    @Autowired
    private ExpiredSaleCancellationService expiredSaleCancellationService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody PresaleRegisterDto presaleRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.presaleCreateService.save(presaleRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody PresaleRegisterDto presaleRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.presaleCreateService.confirm(presaleRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String PresaleCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.presaleSearchService.findDataForm(PresaleCod)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query,int Page,String StoreCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.presaleSearchService.findAll(Query,Page,StoreCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("createCode")
    public ResponseEntity<ResponseWsDto> createCode(){
        try{
            return new ResponseEntity<>(
                    new ResponseWsDto().okResponse(this.presaleCreateService.createCode())
                    ,HttpStatus.OK
            );
        }catch (Exception ex){
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("cancellationDetail")
    public ResponseEntity<ResponseWsDto> cancellationDetail(@RequestParam String PresaleCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.expiredSaleCancellationService.findCancellationDetail(PresaleCod)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("cancel")
    public ResponseEntity<ResponseWsDto> cancel(@RequestParam String PresaleCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.expiredSaleCancellationService.cancelPresale(PresaleCod, false)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("forceCancel")
    public ResponseEntity<ResponseWsDto> forceCancel(@RequestParam String PresaleCod) {
        try {
            return new ResponseEntity<>(
                    new ResponseWsDto(this.expiredSaleCancellationService.cancelPresale(PresaleCod, true)),
                    HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }
}
