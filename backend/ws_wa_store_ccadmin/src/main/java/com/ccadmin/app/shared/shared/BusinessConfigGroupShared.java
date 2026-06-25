package com.ccadmin.app.shared.shared;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.entity.BusinessConfigGroupEntity;
import com.ccadmin.app.shared.service.BusinessConfigGroupCreateService;
import com.ccadmin.app.shared.service.BusinessConfigGroupSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BusinessConfigGroupShared {

    @Autowired
    private BusinessConfigGroupSearchService searchService;

    @Autowired
    private BusinessConfigGroupCreateService createService;

    public BusinessConfigGroupEntity save(BusinessConfigGroupEntity businessConfigGroup) {
        return this.createService.save(businessConfigGroup);
    }

    public List<BusinessConfigGroupEntity> saveAll(List<BusinessConfigGroupEntity> businessConfigGroupList) {
        return this.createService.saveAll(businessConfigGroupList);
    }

    public BusinessConfigGroupEntity findById(String groupCod) {
        return this.searchService.findById(groupCod);
    }

    public BusinessConfigGroupEntity findByGroupId(Integer groupId) {
        return this.searchService.findByGroupId(groupId);
    }

    public ResponsePageSearchT<BusinessConfigGroupEntity> findAll(String query, int page) {
        return this.searchService.findAll(query, page);
    }

    public List<BusinessConfigGroupEntity> findActives() {
        return this.searchService.findActives();
    }
}
