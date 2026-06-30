package com.ccadmin.app.store.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.dto.StoreInfoDto;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import com.ccadmin.app.store.repository.StoreRepository;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class StoreService extends SessionService {

    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private CompanyRepository companyRepository;

    private SearchTService<StoreEntity> searchService;

    public StoreEntity findById(String StoreCod)
    {
        return this.storeRepository.findById(StoreCod).get();
    }

    public List<StoreEntity> findAll()
    {
        return this.storeRepository.findAll();
    }

    public String findUbigeo(String UbigeoCod){
        return this.storeRepository.findUbigeo(UbigeoCod);
    }

    public StoreInfoDto findStoreInfo(String StoreCod){
        StoreInfoDto storeInfo = new StoreInfoDto();
        storeInfo.Company = this.companyRepository.findMyCompany();
        storeInfo.CompanyUbigeo = this.storeRepository.findUbigeo(storeInfo.Company.UbigeoCod);
        storeInfo.Store = this.storeRepository.findById(StoreCod).get();
        storeInfo.StoreUbigeo = this.storeRepository.findUbigeo(storeInfo.Store.UbigeoCod);
        return storeInfo;
    }


    public void initializeStoreAutomation(StoreEntity store){
        log.info("INI_AUTOCOMPLETADO_TIENDA -->>  {}", store.StoreCod);
        this.storeRepository.initializeStoreAutomation(store.StoreCod, store.Name, store.Description);
        log.info("FIN_AUTOCOMPLETADO_TIENDA -->>  {}", store.StoreCod);
    }

    @Transactional
    public StoreEntity save(StoreEntity store)
    {

        boolean exists = this.storeRepository.existsById(store.StoreCod);

        store.SunatAddressTypeCode = normalizeSunatAddressTypeCode(store.SunatAddressTypeCode);
        store.addSession(getUserCod());        
        StoreEntity savedStore = this.storeRepository.save(store);

        if(!exists){
             this.initializeStoreAutomation(savedStore);
        }
       
        return savedStore;
    }

    public ResponsePageSearchT<StoreEntity> findAll(String Query,int Page)
    {
        SearchDto search = new SearchDto(Query,Page);
        this.searchService = new SearchTService<StoreEntity>(this.storeRepository);
        return this.searchService.findAll(search,10);
    }

    private String normalizeSunatAddressTypeCode(String value) {
        if (value == null || value.isBlank()) {
            return "0000";
        }
        String code = value.trim();
        if (!code.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("SunatAddressTypeCode debe tener 4 digitos");
        }
        return code;
    }
}
