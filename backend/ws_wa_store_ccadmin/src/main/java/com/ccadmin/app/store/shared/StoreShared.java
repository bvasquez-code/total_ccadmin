package com.ccadmin.app.store.shared;

import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.service.StoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StoreShared {

    @Autowired
    private StoreService storeService;

    public List<StoreEntity> findAll()
    {
        return this.storeService.findAll();
    }

    public StoreEntity findById(String StoreCod)
    {
        return this.storeService.findById(StoreCod);
    }

    public String findUbigeo(String UbigeoCod){
        return this.storeService.findUbigeo(UbigeoCod);
    }

    public StoreInfoDto findStoreInfo(String StoreCod){
        return this.storeService.findStoreInfo(StoreCod);
    }
}
