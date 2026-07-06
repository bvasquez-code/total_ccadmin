package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.TaxEntity;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxCreateService extends SessionService {

    @Autowired
    private TaxRepository taxRepository;

    public TaxEntity save(TaxEntity tax) {
        if (tax == null) {
            throw new IllegalArgumentException("Tributo requerido");
        }
        tax.validate();
        tax.addSession(this.getUserCod(), !this.taxRepository.existsById(tax.TaxCod));
        return this.taxRepository.save(tax);
    }

    public TaxEntity enable(TaxEntity request) {
        TaxEntity tax = this.taxRepository.findById(request.TaxCod)
                .orElseThrow(() -> new IllegalArgumentException("Tributo no encontrado"));
        tax.active(this.getUserCod());
        return this.taxRepository.save(tax);
    }

    public TaxEntity disable(TaxEntity request) {
        TaxEntity tax = this.taxRepository.findById(request.TaxCod)
                .orElseThrow(() -> new IllegalArgumentException("Tributo no encontrado"));
        tax.inactive(this.getUserCod());
        return this.taxRepository.save(tax);
    }
}
