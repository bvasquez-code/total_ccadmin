package com.ccadmin.app.sale.controller;

import com.ccadmin.app.sale.model.dto.PresaleRegisterDto;
import com.ccadmin.app.sale.service.PresaleCreateService;
import com.ccadmin.app.sale.service.PresaleSearchService;
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
}
