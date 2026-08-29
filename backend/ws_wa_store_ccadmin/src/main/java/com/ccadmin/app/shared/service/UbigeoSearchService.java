package com.ccadmin.app.shared.service;

import com.ccadmin.app.shared.model.dto.LocationOptionDto;
import com.ccadmin.app.shared.repository.LocationRepository;
import com.ccadmin.app.shared.repository.UbigeoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UbigeoSearchService {

    private final UbigeoRepository ubigeoRepository;
    private final LocationRepository locationRepository;

    public UbigeoSearchService(
            UbigeoRepository ubigeoRepository,
            LocationRepository locationRepository
    ) {
        this.ubigeoRepository = ubigeoRepository;
        this.locationRepository = locationRepository;
    }

    public List<LocationOptionDto> findCountries() {
        return locationRepository.findCountries().stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findDepartments() {
        return ubigeoRepository.findDepartments().stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findProvinces(String departmentCod) {
        if (departmentCod == null || !departmentCod.matches("^\\d{2}$")) {
            throw new IllegalArgumentException("El codigo de departamento debe tener 2 digitos");
        }
        return ubigeoRepository.findProvinces(departmentCod).stream()
                .map(LocationOptionDto::new)
                .toList();
    }

    public List<LocationOptionDto> findDistricts(String provinceCod) {
        if (provinceCod == null || !provinceCod.matches("^\\d{4}$")) {
            throw new IllegalArgumentException("El codigo de provincia debe tener 4 digitos");
        }
        return ubigeoRepository.findDistricts(provinceCod).stream()
                .map(LocationOptionDto::new)
                .toList();
    }
}
