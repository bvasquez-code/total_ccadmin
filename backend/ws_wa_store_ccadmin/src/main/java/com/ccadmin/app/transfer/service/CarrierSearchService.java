package com.ccadmin.app.transfer.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearchT;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchTService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.utility.StringUtil;
import com.ccadmin.app.transfer.model.entity.CarrierEntity;
import com.ccadmin.app.transfer.repository.CarrierRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarrierSearchService extends SessionService {

    @Autowired
    private CarrierRepository repository;

    private SearchTService<CarrierEntity> searchService;

    @Autowired
    private void init() {
        this.searchService = new SearchTService<>(this.repository);
    }

    public ResponsePageSearchT<CarrierEntity> findAll(String query, int page) {
        return searchService.findAll(new SearchDto(query, page), 10);
    }

    public CarrierEntity findById(String carrierCod) {
        return repository.findById(carrierCod).orElse(null);
    }

    public List<CarrierEntity> findActives() {
        return repository.findActives();
    }

}
