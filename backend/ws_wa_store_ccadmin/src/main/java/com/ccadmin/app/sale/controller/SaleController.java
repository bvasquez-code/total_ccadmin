package com.ccadmin.app.sale.controller;

import com.ccadmin.app.sale.exception.SaleException;
import com.ccadmin.app.sale.model.dto.SaleConfirmDto;
import com.ccadmin.app.sale.model.dto.SalePaymentRegisterDto;
import com.ccadmin.app.sale.service.SaleCreateService;
import com.ccadmin.app.sale.service.SalePaymentCreateService;
import com.ccadmin.app.sale.service.SaleSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/sale")
public class SaleController {

    public static Logger log = LogManager.getLogger(SaleController.class);
    @Autowired
    private SalePaymentCreateService salePaymentCreateService;
    @Autowired
    private SaleSearchService saleSearchService;
    @Autowired
    private SaleCreateService saleCreateService;

    @PostMapping("addPayment")
    public ResponseEntity<ResponseWsDto> addPayment(@RequestBody SalePaymentRegisterDto salePayment)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.salePaymentCreateService.save(salePayment))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :{}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(@RequestParam String SaleCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.saleSearchService.findDataForm(SaleCod)
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
                    new ResponseWsDto(this.saleSearchService.findAll(Query,Page,StoreCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findByDocumentCod")
    public ResponseEntity<ResponseWsDto> findByDocumentCod(@RequestParam String DocumentCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto().okResponse(this.saleSearchService.findByDocumentCod(DocumentCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam String SaleCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto().okResponse(this.saleSearchService.findById(SaleCod))
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataPrint")
    public ResponseEntity<ResponseWsDto> findDataPrint(@RequestParam String SaleCod)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.saleSearchService.findDataPrint(SaleCod)
                    ,HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveClientSale")
    public ResponseEntity<ResponseWsDto> saveClientSale(@RequestParam String SaleCod, @RequestParam String ClientCod) throws SaleException {
        try {
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto().okResponse(this.saleCreateService.saveClientSale(SaleCod, ClientCod))
                    , HttpStatus.OK
            );
        } catch (Exception ex) {
            log.error("Error :{}", ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("confirm")
    public ResponseEntity<ResponseWsDto> confirm(@RequestBody SaleConfirmDto request)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.saleCreateService.confirm(
                        request.SaleCod,
                        request.DocumentType,
                        request.CounterfoilCod
                    ))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error :{}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

}
