package com.ccadmin.app.sale.controller;

import com.ccadmin.app.sale.model.dto.CreditNoteRegisterDto;
import com.ccadmin.app.sale.model.dto.CreditNoteReturnPaymentRegisterDto;
import com.ccadmin.app.sale.service.CreditNoteCreateService;
import com.ccadmin.app.sale.service.CreditNoteSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("api/v1/CreditNote")
public class CreditNoteController {

    @Autowired
    private CreditNoteSearchService creditNoteSearchService;
    @Autowired
    private CreditNoteCreateService creditNoteCreateService;

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, int Page, String StoreCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.creditNoteSearchService.findAll(Query,Page,StoreCod))
                    , HttpStatus.OK
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
                    new ResponseWsDto().okResponse(this.creditNoteCreateService.createCode())
                    ,HttpStatus.OK
            );
        }catch (Exception ex){
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody CreditNoteRegisterDto creditNoteRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.creditNoteCreateService.save(creditNoteRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody CreditNoteRegisterDto creditNoteRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.creditNoteCreateService.confirm(creditNoteRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("addReturnPayment")
    public ResponseEntity<ResponseWsDto> addReturnPayment(@RequestBody CreditNoteReturnPaymentRegisterDto payment)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.creditNoteCreateService.addReturnPayment(payment))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveReturnStock")
    public ResponseEntity<ResponseWsDto> saveReturnStock(@RequestBody CreditNoteRegisterDto creditNoteRegister)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.creditNoteCreateService.saveReturnStock(creditNoteRegister))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String CreditNoteCod){
        try{
            return new ResponseEntity<>(
                    new ResponseWsDto(this.creditNoteSearchService.findById(CreditNoteCod))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findByDocumentCod")
    public ResponseEntity<ResponseWsDto> findByDocumentCod(@RequestParam String DocumentCod){
        try{
            return new ResponseEntity<>(
                    new ResponseWsDto(this.creditNoteSearchService.findByDocumentCod(DocumentCod))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataPrint")
    public ResponseEntity<ResponseWsDto> findDataPrint(@RequestParam String CreditNoteCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.creditNoteSearchService.findDataPrint(CreditNoteCod)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
