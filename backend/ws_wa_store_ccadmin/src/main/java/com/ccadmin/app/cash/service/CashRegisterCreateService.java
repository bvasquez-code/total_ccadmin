package com.ccadmin.app.cash.service;


import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.repository.CashRegisterRepository;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.shared.TableSequenceShared;
import com.ccadmin.app.system.utility.StringUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CashRegisterCreateService extends SessionService {

    @Autowired
    private CashRegisterRepository repository;

    @Autowired
    private TableSequenceShared tableSequenceShared;

    public CashRegisterEntity save(CashRegisterEntity entity) {

        if(StringUtil.isEmpty(entity.RegisterCod)){
            entity.RegisterCod = tableSequenceShared.getNextCode("cash_register");
        }
        entity.validate().session(this.getUserCod());
        return repository.save(entity);
    }

    public List<CashRegisterEntity> saveAll(List<CashRegisterEntity> list) {
        list.forEach(e -> e.validate().session(this.getUserCod()));
        return repository.saveAll(list);
    }

    public CashRegisterEntity enable(String registerCod) {
        CashRegisterEntity e = repository.findById(registerCod)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada"));
        e.active(this.getUserCod());
        return repository.save(e);
    }

    public CashRegisterEntity disable(String registerCod) {
        CashRegisterEntity e = repository.findById(registerCod)
                .orElseThrow(() -> new IllegalArgumentException("Caja no encontrada"));
        e.inactive(this.getUserCod());
        return repository.save(e);
    }
}
