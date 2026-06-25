package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import com.ccadmin.app.shared.repository.BusinessConfigGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessConfigGroupCreateService extends SessionService {

    @Autowired
    private BusinessConfigGroupRepository businessConfigGroupRepository;

    public BusinessConfigGroupEntity save(BusinessConfigGroupEntity businessConfigGroup) {
        businessConfigGroup.validate().session(this.getUserCod());
        validateUniqueGroupId(businessConfigGroup);
        return this.businessConfigGroupRepository.save(businessConfigGroup);
    }

    public List<BusinessConfigGroupEntity> saveAll(List<BusinessConfigGroupEntity> businessConfigGroupList) {
        businessConfigGroupList.forEach(this::save);
        return businessConfigGroupList;
    }

    public BusinessConfigGroupEntity enable(BusinessConfigGroupEntity request) {
        BusinessConfigGroupEntity businessConfigGroup = this.businessConfigGroupRepository.findById(request.GroupCod)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de configuración no encontrado."));
        businessConfigGroup.active(this.getUserCod());
        return this.businessConfigGroupRepository.save(businessConfigGroup);
    }

    public BusinessConfigGroupEntity disable(BusinessConfigGroupEntity request) {
        BusinessConfigGroupEntity businessConfigGroup = this.businessConfigGroupRepository.findById(request.GroupCod)
                .orElseThrow(() -> new IllegalArgumentException("Grupo de configuración no encontrado."));
        businessConfigGroup.inactive(this.getUserCod());
        return this.businessConfigGroupRepository.save(businessConfigGroup);
    }

    private void validateUniqueGroupId(BusinessConfigGroupEntity businessConfigGroup) {
        this.businessConfigGroupRepository.findByGroupId(businessConfigGroup.GroupId)
                .filter(existing -> !existing.GroupCod.equals(businessConfigGroup.GroupCod))
                .ifPresent(existing -> {
                    throw new RuntimeException("GroupId ya existe : " + businessConfigGroup.GroupId);
                });
    }
}
