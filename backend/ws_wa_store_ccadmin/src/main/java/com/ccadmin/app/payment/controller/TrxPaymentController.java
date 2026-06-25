package com.ccadmin.app.payment.controller;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.service.TrxPaymentCreateService;
import com.ccadmin.app.payment.service.TrxPaymentSearchService;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/TrxPayment")
@Slf4j
public class TrxPaymentController {

    @Autowired
    private TrxPaymentCreateService trxPaymentCreateService;

    @Autowired
    private TrxPaymentSearchService trxPaymentSearchService;

    @PostMapping("save")
    public ResponseEntity<ResponseWsDto> save(@RequestBody TrxPaymentEntity trxPayment)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.trxPaymentCreateService.save(trxPayment))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("saveAll")
    public ResponseEntity<ResponseWsDto> saveAll(@RequestBody List<TrxPaymentEntity> trxPaymentList)
    {
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.trxPaymentCreateService.saveAll(trxPaymentList))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findById")
    public ResponseEntity<ResponseWsDto> findById(@RequestParam Long TrxPaymentId){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.trxPaymentSearchService.findById(TrxPaymentId))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findByTransactionId")
    public ResponseEntity<ResponseWsDto> findByTransactionId(@RequestParam String TransactionId){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.trxPaymentSearchService.findByTransactionId(TransactionId))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findAll")
    public ResponseEntity<ResponseWsDto> findAll(@RequestParam String Query, @RequestParam int Page){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    new ResponseWsDto(this.trxPaymentSearchService.findAll(Query, Page))
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataForm")
    public ResponseEntity<ResponseWsDto> findDataForm(){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.trxPaymentSearchService.findDataForm()
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping("findDataFormView")
    public ResponseEntity<ResponseWsDto> findDataFormView(@RequestParam Long TrxPaymentId){
        try{
            return new ResponseEntity<ResponseWsDto>(
                    this.trxPaymentSearchService.findDataFormView(TrxPaymentId)
                    , HttpStatus.OK
            );
        }
        catch (Exception ex)
        {
            log.error("Error : {}",ex.getMessage(), ex);
            return new ResponseEntity<ResponseWsDto>(new ResponseWsDto(ex),HttpStatus.BAD_REQUEST);
        }
    }
}
