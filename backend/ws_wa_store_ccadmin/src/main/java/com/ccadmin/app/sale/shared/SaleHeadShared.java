package com.ccadmin.app.sale.shared;

import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.sale.service.SaleHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SaleHeadShared {

    @Autowired
    private SaleHeadService saleHeadService;

    public List<SaleHeadEntity> findAllById(List<String> SaleCodList){
        return this.saleHeadService.findAllById(SaleCodList);
    }
}
