package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.repository.SaleHeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleHeadService {

    @Autowired
    private SaleHeadRepository saleHeadRepository;
    public List<SaleHeadEntity> findAllById(List<String> SaleCodList){
        return this.saleHeadRepository.findAllById(SaleCodList);
    }
}
