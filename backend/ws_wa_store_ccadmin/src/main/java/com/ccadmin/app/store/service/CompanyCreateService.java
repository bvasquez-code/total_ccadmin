package com.ccadmin.app.store.service;


import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyCreateService extends SessionService {

    @Autowired
    private CompanyRepository companyRepository;

    /** Crea/actualiza una Company */
    public CompanyEntity save(CompanyEntity company) {
        company.validate().session(this.getUserCod());
        return this.companyRepository.save(company);
    }

    /** Crea/actualiza una lista de Company */
    public List<CompanyEntity> saveAll(List<CompanyEntity> companies) {
        companies.forEach(c -> c.validate().session(this.getUserCod()));
        return this.companyRepository.saveAll(companies);
    }

    /** Cambia estado a Activo */
    public CompanyEntity enable(String CompanyCod) {
        CompanyEntity c = this.companyRepository.findById(CompanyCod)
                .orElseThrow(() -> new IllegalArgumentException("Company no encontrada"));
        c.Status = "A";
        c.session(this.getUserCod());
        return this.companyRepository.save(c);
    }

    /** Cambia estado a Inactivo */
    public CompanyEntity disable(String CompanyCod) {
        CompanyEntity c = this.companyRepository.findById(CompanyCod)
                .orElseThrow(() -> new IllegalArgumentException("Company no encontrada"));
        c.Status = "I";
        c.session(this.getUserCod());
        return this.companyRepository.save(c);
    }
}
