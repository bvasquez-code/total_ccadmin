package com.ccadmin.app.system.service;

import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.entity.CurrencyEntity;
import com.ccadmin.app.system.repository.CurrencyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyCreateService extends SessionService {

    @Autowired
    private CurrencyRepository currencyRepository;

    public CurrencyEntity save(CurrencyEntity currency) {
        if (currency == null) {
            throw new IllegalArgumentException("Moneda requerida");
        }
        currency.validate();
        currency.addSession(this.getUserCod(), !this.currencyRepository.existsById(currency.CurrencyCod));
        return this.currencyRepository.save(currency);
    }

    public List<CurrencyEntity> saveAll(List<CurrencyEntity> currencyList) {
        if (currencyList == null) {
            throw new IllegalArgumentException("Lista de monedas requerida");
        }
        currencyList.forEach(currency -> {
            currency.validate();
            currency.addSession(this.getUserCod(), !this.currencyRepository.existsById(currency.CurrencyCod));
        });
        return this.currencyRepository.saveAll(currencyList);
    }

    public CurrencyEntity enable(CurrencyEntity request) {
        CurrencyEntity currency = this.currencyRepository.findById(request.CurrencyCod)
                .orElseThrow(() -> new IllegalArgumentException("Moneda no encontrada"));
        currency.active(this.getUserCod());
        return this.currencyRepository.save(currency);
    }

    public CurrencyEntity disable(CurrencyEntity request) {
        CurrencyEntity currency = this.currencyRepository.findById(request.CurrencyCod)
                .orElseThrow(() -> new IllegalArgumentException("Moneda no encontrada"));
        currency.inactive(this.getUserCod());
        return this.currencyRepository.save(currency);
    }
}
