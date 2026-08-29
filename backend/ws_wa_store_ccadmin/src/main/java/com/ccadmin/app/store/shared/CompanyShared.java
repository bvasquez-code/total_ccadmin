package com.ccadmin.app.store.shared;

import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.service.CompanyCreateService;
import com.ccadmin.app.store.service.CompanySearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyShared {
    @Autowired
    private CompanySearchService searchService;
    @Autowired
    private CompanyCreateService createService;

    public CompanyEntity findById(String companyCod) {
        return searchService.findById(normalizeCompanyCod(companyCod));
    }

    public CompanyEntity findMyCompany(){
        return this.searchService.findMyCompany();
    }

    public CompanyEntity findOnlyCompany() {
        return this.searchService.findOnlyCompany();
    }


    public List<CompanyEntity> findActives() {
        return searchService.findActives();
    }

    public CompanyEntity save(CompanyEntity company) {
        normalize(company);
        return createService.save(company);
    }

    public CompanyEntity saveOnlyCompany(CompanyEntity company) {
        normalize(company);
        return createService.saveOnlyCompany(company);
    }

    public List<CompanyEntity> saveAll(List<CompanyEntity> companies) {
        companies.forEach(this::normalize);
        return createService.saveAll(companies);
    }

    public CompanyEntity enable(String companyCod) {
        return createService.enable(normalizeCompanyCod(companyCod));
    }

    public CompanyEntity disable(String companyCod) {
        return createService.disable(normalizeCompanyCod(companyCod));
    }

    // ===== Helpers mínimos =====
    private static String normalizeCompanyCod(String v) {
        return (v == null) ? null : v.trim().toUpperCase();
    }
    private static String safeQuery(String q) {
        return (q == null || q.isBlank()) ? "" : q.trim();
    }
    private void normalize(CompanyEntity e) {
        if (e == null) return;
        e.CompanyCod = normalizeCompanyCod(e.CompanyCod);
        if (e.CountryCode != null) e.CountryCode = e.CountryCode.trim().toUpperCase();
        if (e.TaxId != null) e.TaxId = e.TaxId.trim();
        if (e.LegalName != null) e.LegalName = e.LegalName.trim();
        if (e.TradeName != null) e.TradeName = e.TradeName.trim();
        if (e.FiscalAddress != null) e.FiscalAddress = e.FiscalAddress.trim();
        if (e.Address != null) e.Address = e.Address.trim();
        if (e.UbigeoCod != null) e.UbigeoCod = e.UbigeoCod.trim();
        if (e.Department != null) e.Department = e.Department.trim();
        if (e.Province != null) e.Province = e.Province.trim();
        if (e.District != null) e.District = e.District.trim();
        if (e.Phone != null) e.Phone = e.Phone.trim();
        if (e.Email != null) e.Email = e.Email.trim();
        if (e.Website != null) e.Website = e.Website.trim();
        if (e.LogoPath != null) e.LogoPath = e.LogoPath.trim();
    }
}
