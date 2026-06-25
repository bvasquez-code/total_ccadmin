package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import com.ccadmin.app.shared.model.entity.id.BusinessConfigEntityID;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import com.ccadmin.app.system.model.dto.DocumentTypeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessConfigSearchService {

    @Autowired
    private BusinessConfigRepository businessConfigRepository;

    @Autowired
    private BusinessConfigGroupSearchService businessConfigGroupSearchService;

    private SearchTService<BusinessConfigEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.businessConfigRepository);
    }

    public BusinessConfigEntity findById(BusinessConfigEntityID id) {
        return this.businessConfigRepository.findById(id).orElse(null);
    }

    public BusinessConfigEntity findById(String groupCod, Integer configCorr) {
        return this.findById(new BusinessConfigEntityID(groupCod, configCorr));
    }

    public List<BusinessConfigEntity> findAllById(List<BusinessConfigEntityID> businessConfigIDList) {
        return this.businessConfigRepository.findAllById(businessConfigIDList);
    }

    public BusinessConfigEntity findByConfigCod(String groupCod, String configCod) {
        return this.businessConfigRepository.findByConfigCod(groupCod, configCod);
    }

    public List<BusinessConfigEntity> findByGroupCod(String groupCod) {
        return this.businessConfigRepository.findByGroupCod(groupCod);
    }

    public List<BusinessConfigEntity> findActivesByGroupCod(String groupCod) {
        return this.businessConfigRepository.findActivesByGroupCod(groupCod);
    }

    public List<BusinessConfigEntity> findActives() {
        return this.businessConfigRepository.findActives();
    }

    public ResponsePageSearchT<BusinessConfigEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public ResponsePageSearchT<BusinessConfigEntity> findAll(String query, int page, String groupCod) {
        return this.searchTService.findAllStore(new SearchDto(query, page, groupCod), 10);
    }

    public ResponseWsDto findDataForm(String groupCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (groupCod != null && !groupCod.isEmpty()) {
            BusinessConfigGroupEntity businessConfigGroup = this.businessConfigGroupSearchService.findById(groupCod);
            rpt.AddResponseAdditional("businessConfigGroup", businessConfigGroup);

            List<BusinessConfigEntity> businessConfigList = this.findByGroupCod(groupCod);
            rpt.AddResponseAdditional("businessConfigList", businessConfigList);

            int ConfigCorrNext = businessConfigList.stream()
                .mapToInt(e->e.ConfigCorr)
                .max()
                .orElse(0) + 1;

            rpt.AddResponseAdditional("ConfigCorrNext", ConfigCorrNext);
        }

        return rpt;
    }

    public List<DocumentTypeDto> getSaleDocumentType() {
        return this.findActivesByGroupCod("SalesDocumentType")
                .stream()
                .map(e -> new DocumentTypeDto(e.ConfigCod, e.ConfigVal))
                .toList();
    }
}
