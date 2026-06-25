package com.ccadmin.app.pucharse.controller;

import com.ccadmin.app.pucharse.model.dto.PucharseRegisterDto;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.service.PucharseService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/pucharse")
public class PucharseController {

    public static Logger log = LogManager.getLogger(PucharseController.class);
    @Autowired
    private PucharseService pucharseService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody PucharseRegisterDto pucharseRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseService.save(pucharseRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :"+ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody PucharseRegisterDto pucharseRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseService.confirm(pucharseRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :"+ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String PucharseCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.pucharseService.findDataForm(PucharseCod)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("endReception")
    public ResponseEntity<ResponseWsDto> endReception(@RequestBody PucharseHeadEntity pucharseHead){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseService.endReception(pucharseHead))
                    , HttpStatus.OK
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
                    new ResponseWsDto(this.pucharseService.findAll(Query,Page,StoreCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
