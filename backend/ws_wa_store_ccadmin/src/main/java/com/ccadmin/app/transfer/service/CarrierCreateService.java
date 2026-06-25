package com.ccadmin.app.transfer.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.transfer.model.entity.CarrierEntity;
import com.ccadmin.app.transfer.repository.CarrierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarrierCreateService extends SessionService {

    @Autowired
    private CarrierRepository repository;

    public CarrierEntity save(CarrierEntity entity) {
        if (entity == null) {
            throw new IllegalArgumentException("Transportista requerido");
        }
        entity.CarrierCod = entity.DriverDocNumber;
        entity.validate().session(this.getUserCod());
        return repository.save(entity);
    }
    
    public List<CarrierEntity> saveAll(List<CarrierEntity> list) {
        list.forEach(e -> {
            e.CarrierCod = e.DriverDocNumber;
            e.validate().session(this.getUserCod());
        });
        return repository.saveAll(list);
    }

    public CarrierEntity enable(String carrierCod) {
        CarrierEntity e = repository.findById(carrierCod)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));
        e.active(this.getUserCod());
        return repository.save(e);
    }

    public CarrierEntity disable(String carrierCod) {
        CarrierEntity e = repository.findById(carrierCod)
                .orElseThrow(() -> new IllegalArgumentException("Transportista no encontrado"));
        e.inactive(this.getUserCod());
        return repository.save(e);
    }

    public String delete(String carrierCod) {
        disable(carrierCod);
        return "Transportista eliminado correctamente";
    }
}
