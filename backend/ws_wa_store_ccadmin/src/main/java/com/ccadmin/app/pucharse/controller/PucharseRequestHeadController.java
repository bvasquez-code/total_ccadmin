package com.ccadmin.app.pucharse.controller;

import com.ccadmin.app.pucharse.model.dto.PucharseRequestRegisterDto;
import com.ccadmin.app.pucharse.service.PucharseRequestHeadService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/pucharserequest")
public class PucharseRequestHeadController {

    public static Logger log = LogManager.getLogger(PucharseRequestHeadController.class);
    @Autowired
    private PucharseRequestHeadService pucharseRequestHeadService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody PucharseRequestRegisterDto pucharseRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseRequestHeadService.save(pucharseRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :"+ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query,int Page,String StoreCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseRequestHeadService.findAll(Query,Page,StoreCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String PucharseReqCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.pucharseRequestHeadService.findDataForm(PucharseReqCod)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
