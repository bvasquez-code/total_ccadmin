package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessConfigCreateService extends SessionService {

    @Autowired
    private BusinessConfigRepository businessConfigRepository;

    public BusinessConfigEntity save(BusinessConfigEntity businessConfig) {
        businessConfig.validate().session(this.getUserCod());
        return this.businessConfigRepository.save(businessConfig);
    }

    public List<BusinessConfigEntity> saveAll(List<BusinessConfigEntity> businessConfigList) {
        businessConfigList.forEach(this::save);
        return businessConfigList;
    }

    public BusinessConfigEntity enable(BusinessConfigEntity request) {
        BusinessConfigEntity businessConfig = findRequired(request);
        businessConfig.active(this.getUserCod());
        return this.businessConfigRepository.save(businessConfig);
    }

    public BusinessConfigEntity disable(BusinessConfigEntity request) {
        BusinessConfigEntity businessConfig = findRequired(request);
        businessConfig.inactive(this.getUserCod());
        return this.businessConfigRepository.save(businessConfig);
    }

    private BusinessConfigEntity findRequired(BusinessConfigEntity request) {
        BusinessConfigEntityID id = new BusinessConfigEntityID(request.GroupCod, request.ConfigCorr);
        return this.businessConfigRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Configuración de negocio no encontrada."));
    }
}
