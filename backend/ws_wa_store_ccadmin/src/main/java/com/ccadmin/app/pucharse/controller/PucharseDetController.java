package com.ccadmin.app.pucharse.controller;

import com.ccadmin.app.pucharse.model.dto.PucharseDetConfirmDto;
import com.ccadmin.app.pucharse.model.dto.PucharseDetLotConfirmDto;
import com.ccadmin.app.pucharse.service.PucharseDetService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/PucharseDet")
public class PucharseDetController {
    public static Logger log = LogManager.getLogger(PucharseController.class);
    @Autowired
    private PucharseDetService pucharseDetService;
    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody PucharseDetConfirmDto pucharseDetConfirm)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseDetService.confirm(pucharseDetConfirm))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :"+ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("confirmWithLots")
    public ResponseEntity<ResponseWsDto> confirmWithLots(@RequestBody PucharseDetLotConfirmDto pucharseDetLotConfirm)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.pucharseDetService.confirmWithLots(pucharseDetLotConfirm))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :"+ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
