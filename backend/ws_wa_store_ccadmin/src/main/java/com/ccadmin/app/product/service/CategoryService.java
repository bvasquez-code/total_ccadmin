package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.CategoryRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.repository.CategoryRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CategoryService extends SessionService {
    @Autowired
    private CategoryRepository categoryRepository;
    private SearchService searchService;

    public ResponsePageSearch findAll(String Query, int Page) {
        this.searchService = new SearchService(this.categoryRepository);
        SearchDto search = new SearchDto(Query, Page);
        ResponsePageSearch responsePage = this.searchService.findAll(search, 10);
        return responsePage;
    }

    public ResponseWsDto findDataForm(String CategoryCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (CategoryCod != null && CategoryCod.length() > 0) {
            rpt.AddResponseAdditional("category", this.categoryRepository.findById(CategoryCod).get());
        }
        rpt.AddResponseAdditional("categoryDadList", this.categoryRepository.findAllActiveDad());

        return rpt;
    }

    public CategoryEntity save(CategoryEntity category) {

        category.addSession(getUserCod(), !this.categoryRepository.existsById(category.CategoryCod));

        return this.categoryRepository.save(category);
    }

    @Transactional
    public ResponseWsDto saveAll(CategoryRegisterMassiveDto categoryRegisterMassive) {
        ResponseWsDto rpt = new ResponseWsDto();
        CategoryRegisterMassiveDto registerMassiveFail = new CategoryRegisterMassiveDto();
        CategoryRegisterMassiveDto registerMassiveExists = new CategoryRegisterMassiveDto();
        CategoryRegisterMassiveDto registerMassiveOk = new CategoryRegisterMassiveDto();

        for (CategoryEntity category : categoryRegisterMassive.categoryList) {
            try {
                category.addSession(getUserCod(), !this.categoryRepository.existsById(category.CategoryCod));

                if (this.categoryRepository.existsById(category.CategoryCod)) {
                    registerMassiveExists.categoryList.add(category);
                } else {
                    registerMassiveOk.categoryList.add(category);
                }
            } catch (Exception ex) {
                log.error("Error en saveAll :{} ==> {}", category.toString(), ex.getMessage());
                registerMassiveFail.categoryList.add(category);
            }
        }

        this.categoryRepository.saveAll(registerMassiveOk.categoryList);

        rpt.AddResponseAdditional("registerMassiveFail", registerMassiveFail);
        rpt.AddResponseAdditional("registerMassiveExists", registerMassiveExists);
        return rpt;
    }

    public ResponseWsDto findDataFormMassive() {
        ResponseWsDto rpt = new ResponseWsDto();
        rpt.AddResponseAdditional("categoryDadList", this.categoryRepository.findAllActiveDad());
        return rpt;
    }
}
