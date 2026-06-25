package com.ccadmin.app.store.service;

import com.ccadmin.app.store.model.entity.WarehouseEntity;
import com.ccadmin.app.store.repository.WarehouseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {

    @Autowired
    private WarehouseRepository warehouseRepository;

    public WarehouseEntity findById(String WarehouseCod)
    {
        return this.warehouseRepository.findById(WarehouseCod).get();
    }

    public boolean IsMultipleWarehouse(String StoreCod)
    {
        int NumWarehouse = this.warehouseRepository.countNumberWarehouse(StoreCod);

        return ( NumWarehouse > 1 );
    }

    public List<WarehouseEntity> findByStore(String StoreCod)
    {
        return this.warehouseRepository.findByStore(StoreCod);
    }

    public List<WarehouseEntity> findAll()
    {
        return this.warehouseRepository.findAll();
    }
}
