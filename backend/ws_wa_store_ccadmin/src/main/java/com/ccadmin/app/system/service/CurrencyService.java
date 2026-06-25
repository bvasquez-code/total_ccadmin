package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyService {

    @Autowired
    private CurrencyRepository currencyRepository;

    public CurrencyEntity findById(String CurrencyCod)
    {
        return this.currencyRepository.findById(CurrencyCod).get();
    }

    public CurrencyEntity findCurrencySystem()
    {
        return this.currencyRepository.findCurrencySystem();
    }
    public List<CurrencyEntity> findAllActive()
    {
        return this.currencyRepository.findAllActive();
    }

}
