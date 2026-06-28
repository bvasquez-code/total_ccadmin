package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.KardexEntity;
import com.ccadmin.app.product.service.KardexService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KardexShared {

    @Autowired
    private KardexService kardexService;

    public KardexEntity save(KardexEntity kardex)
    {
        return this.kardexService.save(kardex);
    }

    public KardexEntity findLastMovement(String ProductCod,String Variant,String WarehouseCod,String StoreCod)
    {
        return this.kardexService.findLastMovement(ProductCod,Variant,WarehouseCod,StoreCod);
    }

    public List<KardexEntity> saveAll(List<KardexEntity> kardexList)
    {
        return this.kardexService.saveAll(kardexList);
    }

    public List<KardexEntity> saveAllLedgerOnly(List<KardexEntity> kardexList)
    {
        return this.kardexService.saveAllLedgerOnly(kardexList);
    }
}
