package com.ccadmin.app.store.shared;

import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseShared {

    @Autowired
    private WarehouseService warehouseService;

    public boolean IsMultipleWarehouse(String StoreCod)
    {
        return this.warehouseService.IsMultipleWarehouse(StoreCod);
    }

    public List<WarehouseEntity> findByStore(String StoreCod)
    {
        return this.warehouseService.findByStore(StoreCod);
    }

    public List<WarehouseEntity> findAll()
    {
        return this.warehouseService.findAll();
    }
}
