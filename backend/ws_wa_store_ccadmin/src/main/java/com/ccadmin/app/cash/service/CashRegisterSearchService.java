package com.ccadmin.app.cash.service;


import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.sale.model.entity.SaleHeadEntity;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.StoreEntity;
import com.ccadmin.app.store.shared.StoreShared;
import com.ccadmin.app.system.utility.NetInfoUtil;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.SocketException;
import java.util.List;

@Service
public class CashRegisterSearchService extends SessionService {

    @Autowired
    private CashRegisterRepository repository;
    @Autowired
    private StoreShared storeShared;

    private SearchTService<CashRegisterEntity> searchService;

    @Autowired
    private void init() {
        this.searchService = new SearchTService<>(this.repository);
    }

    public ResponsePageSearchT<CashRegisterEntity> findAll(String Query, int Page) {
        SearchDto search = new SearchDto(Query,Page,getStoreCod());
        return searchService.findAll(new SearchDto(Query,Page), 10);
    }

    public CashRegisterEntity findById(String registerCod) {
        return repository.findById(registerCod).orElse(null);
    }

    public List<CashRegisterEntity> findActives() {
        return repository.findActives();
    }

    public List<CashRegisterEntity> findActivesByStore(String storeCod) {
        return repository.findActivesByStore(storeCod);
    }

    public ResponseWsDto findDataForm(String registerCod) throws SocketException {
        ResponseWsDto rpt = new ResponseWsDto();
        if(StringUtil.isNotEmpty(registerCod)){
            CashRegisterEntity cashRegister = findById(registerCod);
            rpt.AddResponseAdditional("cashRegister",cashRegister);
        }
        StoreEntity store  = this.storeShared.findById(getStoreCod());
        rpt.AddResponseAdditional("store",store);
        rpt.AddResponseAdditional("mac",NetInfoUtil.getBestLocalMac());
        rpt.AddResponseAdditional("ip",NetInfoUtil.getBestLocalIpv4());
        return rpt;
    }
}
