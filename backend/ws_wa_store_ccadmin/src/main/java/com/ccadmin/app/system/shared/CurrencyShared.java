package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.service.CurrencyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyShared {

    @Autowired
    private CurrencyService currencyService;

    public CurrencyEntity findCurrencySystem()
    {
        return this.currencyService.findCurrencySystem();
    }

    public CurrencyEntity findById(String CurrencyCod)
    {
        return this.currencyService.findById(CurrencyCod);
    }
    public List<CurrencyEntity> findAllActive()
    {
        return this.currencyService.findAllActive();
    }
}
