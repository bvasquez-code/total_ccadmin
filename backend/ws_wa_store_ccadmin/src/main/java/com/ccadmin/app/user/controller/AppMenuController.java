package com.ccadmin.app.user.controller;

import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.user.model.entity.AppMenuEntity;
import com.ccadmin.app.user.service.AppMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/appMenu")
public class AppMenuController {

    @Autowired
    private AppMenuService appMenuService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody AppMenuEntity appMenu)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.appMenuService.save(appMenu))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query,int Page)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.appMenuService.findAll(Query,Page))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String Id)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.appMenuService.findDataForm(Id)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("updateStatus")
    public ResponseEntity<ResponseWsDto> updateStatus(@RequestBody AppMenuEntity appMenu)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.appMenuService.updateStatus(appMenu))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String MenuCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.appMenuService.findById(MenuCod))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}