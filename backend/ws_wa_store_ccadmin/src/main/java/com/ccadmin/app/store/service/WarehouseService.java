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

    /**
     * Punto unico para resolver el almacen principal de una tienda.
     * Hoy una tienda debe tener exactamente un almacen activo. Cuando existan
     * reglas de prioridad, solamente se modificara este metodo.
     */
    public WarehouseEntity findMainWarehouseByStore(String StoreCod)
    {
        List<WarehouseEntity> warehouseList = this.warehouseRepository.findByStore(StoreCod);
        if (warehouseList.isEmpty()) {
            throw new IllegalArgumentException("La tienda " + StoreCod + " no tiene un almacen activo");
        }
        if (warehouseList.size() > 1) {
            throw new IllegalStateException(
                    "La tienda " + StoreCod
                            + " tiene mas de un almacen activo; aun no existe una regla para elegir el principal"
            );
        }
        return warehouseList.getFirst();
    }

    public List<WarehouseEntity> findAll()
    {
        return this.warehouseRepository.findAll();
    }
}
