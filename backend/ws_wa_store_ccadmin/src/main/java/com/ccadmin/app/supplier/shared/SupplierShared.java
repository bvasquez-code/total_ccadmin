package com.ccadmin.app.supplier.shared;

import com.ccadmin.app.supplier.model.entity.SupplierEntity;
import com.ccadmin.app.supplier.service.SupplierService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SupplierShared {

    @Autowired
    private SupplierService supplierService;

    public SupplierEntity findById(String SupplierCod)
    {
        return this.supplierService.findById(SupplierCod);
    }

    public List<SupplierEntity> findAllById(List<String> SupplierCodList)
    {
        return this.supplierService.findAllById(SupplierCodList);
    }
}
