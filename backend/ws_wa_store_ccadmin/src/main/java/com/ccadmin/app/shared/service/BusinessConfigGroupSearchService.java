package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import com.ccadmin.app.shared.repository.BusinessConfigGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessConfigGroupSearchService {

    @Autowired
    private BusinessConfigGroupRepository businessConfigGroupRepository;

    private SearchTService<BusinessConfigGroupEntity> searchTService;

    @Autowired
    private void initSearchService() {
        this.searchTService = new SearchTService<>(this.businessConfigGroupRepository);
    }

    public BusinessConfigGroupEntity findById(String groupCod) {
        return this.businessConfigGroupRepository.findById(groupCod).orElse(null);
    }

    public BusinessConfigGroupEntity findByGroupId(Integer groupId) {
        return this.businessConfigGroupRepository.findByGroupId(groupId).orElse(null);
    }

    public ResponsePageSearchT<BusinessConfigGroupEntity> findAll(String query, int page) {
        return this.searchTService.findAll(new SearchDto(query, page), 10);
    }

    public List<BusinessConfigGroupEntity> findActives() {
        return this.businessConfigGroupRepository.findActives();
    }

    public ResponseWsDto findDataForm(String groupCod) {
        ResponseWsDto rpt = new ResponseWsDto();

        if (groupCod != null && !groupCod.isEmpty()) {
            rpt.AddResponseAdditional("businessConfigGroup", this.findById(groupCod));
        }

        return rpt;
    }
}
