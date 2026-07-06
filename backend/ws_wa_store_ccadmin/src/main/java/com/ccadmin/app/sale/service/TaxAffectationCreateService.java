package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.TaxAffectationEntity;
import com.ccadmin.app.sale.repository.TaxAffectationRepository;
import com.ccadmin.app.sale.repository.TaxRepository;
import com.ccadmin.app.shared.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TaxAffectationCreateService extends SessionService {

    @Autowired
    private TaxAffectationRepository taxAffectationRepository;
    @Autowired
    private TaxRepository taxRepository;

    public TaxAffectationEntity save(TaxAffectationEntity taxAffectation) {
        if (taxAffectation == null) {
            throw new IllegalArgumentException("Afectacion tributaria requerida");
        }
        taxAffectation.validate();
        if (!this.taxRepository.existsById(taxAffectation.TaxCod)) {
            throw new IllegalArgumentException("Tributo asociado no existe");
        }
        taxAffectation.addSession(this.getUserCod(), !this.taxAffectationRepository.existsById(taxAffectation.TaxAffectationCod));
        return this.taxAffectationRepository.save(taxAffectation);
    }

    public TaxAffectationEntity enable(TaxAffectationEntity request) {
        TaxAffectationEntity taxAffectation = this.taxAffectationRepository.findById(request.TaxAffectationCod)
                .orElseThrow(() -> new IllegalArgumentException("Afectacion tributaria no encontrada"));
        taxAffectation.active(this.getUserCod());
        return this.taxAffectationRepository.save(taxAffectation);
    }

    public TaxAffectationEntity disable(TaxAffectationEntity request) {
        TaxAffectationEntity taxAffectation = this.taxAffectationRepository.findById(request.TaxAffectationCod)
                .orElseThrow(() -> new IllegalArgumentException("Afectacion tributaria no encontrada"));
        taxAffectation.inactive(this.getUserCod());
        return this.taxAffectationRepository.save(taxAffectation);
    }
}
