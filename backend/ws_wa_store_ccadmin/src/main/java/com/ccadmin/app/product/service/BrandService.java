package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.dto.BrandRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.repository.BrandRepository;
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
public class BrandService extends SessionService {

    @Autowired
    private BrandRepository brandRepository;
    @Autowired
    private BrandCreateService brandCreateService;
    private SearchService searchService;

    public BrandEntity findById(String brandCod) {
        return this.brandRepository.findById(brandCod).get();
    }

    public ResponsePageSearch findAll(String Query, int Page) {
        this.searchService = new SearchService(this.brandRepository);
        SearchDto search = new SearchDto(Query, Page);
        ResponsePageSearch responsePage = this.searchService.findAll(search, 10);
        return responsePage;
    }

    public ResponseWsDto findDataForm(String BrandCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (BrandCod != null && BrandCod.length() > 0) {
            rpt.AddResponseAdditional("brand", this.brandRepository.findById(BrandCod).get());
        }

        return rpt;
    }

    public BrandEntity save(BrandEntity brand) {
        return this.brandCreateService.save(brand, getUserCod());
    }

    public String generateBrandCode() {
        return this.brandCreateService.generateBrandCode();
    }

    @Transactional
    public ResponseWsDto saveAll(BrandRegisterMassiveDto brandRegisterMassive) {
        ResponseWsDto rpt = new ResponseWsDto();
        BrandRegisterMassiveDto registerMassiveFail = new BrandRegisterMassiveDto();
        BrandRegisterMassiveDto registerMassiveExists = new BrandRegisterMassiveDto();
        BrandRegisterMassiveDto registerMassiveOk = new BrandRegisterMassiveDto();

        for (BrandEntity brand : brandRegisterMassive.brandList) {
            try {
                if (this.brandRepository.existsById(brand.BrandCod)) {
                    registerMassiveExists.brandList.add(brand);
                } else {
                    registerMassiveOk.brandList.add(brand);
                }
            } catch (Exception ex) {
                log.error("Error en saveAll :{} ==> {}", brand.toString(), ex.getMessage());
                registerMassiveFail.brandList.add(brand);
            }
        }

        if (!registerMassiveOk.brandList.isEmpty()) {
            this.brandCreateService.createBulk(
                    registerMassiveOk.brandList, getUserCod()
            );
        }

        rpt.AddResponseAdditional("registerMassiveFail", registerMassiveFail);
        rpt.AddResponseAdditional("registerMassiveExists", registerMassiveExists);
        return rpt;
    }

}
