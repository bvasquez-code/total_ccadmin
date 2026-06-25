package com.ccadmin.app.security.controller;

import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.security.service.SecurityService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/security")
public class SecurityController {

    @Autowired
    private SecurityService securityService;

    @GetMapping("findUserSession")
    public ResponseEntity<ResponseWsDto> findUserSession()
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.securityService.findUserSession())
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

}
