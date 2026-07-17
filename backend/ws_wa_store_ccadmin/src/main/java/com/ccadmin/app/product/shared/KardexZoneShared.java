package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.dto.KardexZoneOperationDto;
import com.ccadmin.app.product.model.entity.KardexZoneEntity;
import com.ccadmin.app.product.service.KardexZoneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KardexZoneShared {

    @Autowired
    private KardexZoneService kardexZoneService;

    public List<KardexZoneEntity> apply(KardexZoneOperationDto operation, String userCod) {
        return this.kardexZoneService.apply(operation, userCod);
    }
}
