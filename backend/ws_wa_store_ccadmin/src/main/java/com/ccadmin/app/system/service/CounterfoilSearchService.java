package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.shared.shared.CatalogSearchShared;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.model.dto.DocumentTypeDto;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.repository.CounterfoilRepository;
import com.ccadmin.app.system.repository.CounterfoilStoreRepository;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CounterfoilSearchService extends SessionService {

    @Autowired
    private CounterfoilRepository counterfoilRepository;
    @Autowired
    private CounterfoilStoreRepository counterfoilStoreRepository;
    @Autowired
    private CatalogSearchShared catalogSearchShared;
    @Autowired
    private StoreShared storeShared;

    private SearchTService<CounterfoilEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.counterfoilRepository);
    }

    public ResponsePageSearchT<CounterfoilEntity> findAll(String query, int page,String StoreCod) {
        SearchDto search = new SearchDto(query, page,StoreCod);
        return this.searchTService.findAllStore(search, 10);
    }

    public CounterfoilEntity findById(String counterfoilCod) {
        return counterfoilRepository.findById(counterfoilCod).orElse(null);
    }

    public Boolean existsSeries(String Series){
        return counterfoilRepository.existBySeries(Series) > 0;
    }

    public List<CounterfoilEntity> findActives() {
        return counterfoilRepository.findActives();
    }

    public ResponseWsDto findDataForm(String counterfoilCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (StringUtil.isNotEmpty(counterfoilCod)) {
            CounterfoilEntity counterfoil = this.findById(counterfoilCod);
            rpt.AddResponseAdditional("counterfoil", counterfoil);

            rpt.AddResponseAdditional(
                    "storesAssigned",
                    counterfoilStoreRepository.findStoresByCounterfoil(counterfoilCod)
            );
        }
        List<DocumentTypeDto> documentType = catalogSearchShared.getSaleDocumentType();
        rpt.AddResponseAdditional("documentType",documentType);
        StoreEntity store = this.storeShared.findById(getStoreCod());
        rpt.AddResponseAdditional("store",store);

        return rpt;
    }
}
