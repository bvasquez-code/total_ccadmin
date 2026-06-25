package com.ccadmin.app.sale.service;

import com.ccadmin.app.payment.model.entity.TrxPaymentEntity;
import com.ccadmin.app.payment.shared.TrxPaymentShared;
import com.ccadmin.app.sale.model.dto.SalePaymentDto;
import com.ccadmin.app.sale.model.entity.SalePaymentEntity;
import com.ccadmin.app.sale.repository.SalePaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SalePaymentSearchService {

    @Autowired
    private SalePaymentRepository salePaymentRepository;
    @Autowired
    private TrxPaymentShared trxPaymentShared;

    public List<SalePaymentDto> findBySaleCod(String SaleCod){

        List<SalePaymentDto> SalePaymentDtoList = new ArrayList<>();

        List<SalePaymentEntity> SalePaymentList = this.salePaymentRepository.findBySaleCod(SaleCod);
        List<TrxPaymentEntity> TrxPaymentList = trxPaymentShared.findAllById( SalePaymentList.stream().map(e -> e.TrxPaymentId ).toList() );

        for(var SalePayment : SalePaymentList){
            TrxPaymentEntity trxPayment = TrxPaymentList.stream()
                    .filter( e -> e.TrxPaymentId == SalePayment.TrxPaymentId)
                    .findFirst().get();

            SalePaymentDto salePaymentDto = new SalePaymentDto(SalePayment,trxPayment);
            SalePaymentDtoList.add(salePaymentDto);
        }
        return SalePaymentDtoList;
    }
}
