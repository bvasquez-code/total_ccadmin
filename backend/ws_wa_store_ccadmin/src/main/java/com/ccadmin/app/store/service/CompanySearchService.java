package com.ccadmin.app.store.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanySearchService {

    @Autowired
    private CompanyRepository companyRepository;

    private SearchService searchService;

    @Autowired
    private void initSearchService() {
        this.searchService = new SearchService(this.companyRepository);
    }


    public CompanyEntity findById(String CompanyCod) {
        return this.companyRepository.findById(CompanyCod).orElse(null);
    }

    public List<CompanyEntity> findActives() {
        return this.companyRepository.findActives();
    }

    public CompanyEntity findMyCompany(){
        return this.companyRepository.findMyCompany();
    }
}
