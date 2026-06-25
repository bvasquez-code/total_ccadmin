package com.ccadmin.app.cash.shared;


import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.service.CashRegisterCreateService;
import com.ccadmin.app.cash.service.CashRegisterSearchService;
import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
public class CashRegisterShared {

    @Autowired
    private CashRegisterSearchService searchService;

    @Autowired
    private CashRegisterCreateService createService;

    // Lectura
    public CashRegisterEntity getById(String registerCod) {
        return searchService.findById(norm(registerCod));
    }
    public ResponsePageSearchT<CashRegisterEntity> search(String query, int page) {
        return searchService.findAll(safe(query), page);
    }
    public List<CashRegisterEntity> getActives() { return searchService.findActives(); }
    public List<CashRegisterEntity> getActivesByStore(String storeCod) {
        return searchService.findActivesByStore(norm(storeCod));
    }

    // Escritura
    public CashRegisterEntity createOrUpdate(CashRegisterEntity e) {
        normalize(e);
        return createService.save(e);
    }

    public java.util.List<CashRegisterEntity> createOrUpdateAll(java.util.List<CashRegisterEntity> list) {
        list.forEach(this::normalize);
        return createService.saveAll(list);
    }

    public CashRegisterEntity enable(String registerCod) {
        return createService.enable(norm(registerCod));
    }

    public CashRegisterEntity disable(String registerCod) {
        return createService.disable(norm(registerCod));
    }

    // helpers
    private static String norm(String v){
        return v==null?null:v.trim().toUpperCase();
    }

    private static String safe(String q){
        return (q==null||q.isBlank())? "": q.trim();
    }

    private void normalize(CashRegisterEntity e){
        if(e==null) return;
        e.RegisterCod = norm(e.RegisterCod);
        e.StoreCod = norm(e.StoreCod);
        if(e.Name!=null) e.Name = e.Name.trim();
    }
}
