package com.ccadmin.app.person.controller;

import com.ccadmin.app.person.service.PersonService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/person")
public class PersonController {

    @Autowired
    private PersonService personService;

    @GetMapping("findByDocumentNum")
    public ResponseEntity<ResponseWsDto> findByDocumentNum(@RequestParam String DocumentType, String DocumentNum)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.personService.findByDocumentNum(DocumentType,DocumentNum))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
