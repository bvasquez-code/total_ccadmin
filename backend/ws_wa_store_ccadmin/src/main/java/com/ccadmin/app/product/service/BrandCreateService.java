package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.repository.BrandRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centraliza las operaciones de escritura de marcas para flujos unitarios
 * y masivos.
 */
@Service
public class BrandCreateService {
    private final BrandRepository brandRepository;

    public BrandCreateService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @Transactional
    public List<BrandEntity> createBulk(List<BrandEntity> brandList,
                                        String userCod) {
        if (brandList == null || brandList.isEmpty()) {
            throw new IllegalArgumentException("El bloque de marcas no tiene detalles");
        }
        String auditUser = auditUser(userCod);
        for (BrandEntity brand : brandList) {
            if (brand == null || clean(brand.BrandCod).isEmpty()
                    || clean(brand.BrandName).isEmpty()) {
                throw new IllegalArgumentException(
                        "El codigo y nombre de marca son obligatorios"
                );
            }
            if (brandRepository.existsById(brand.BrandCod)) {
                throw new IllegalStateException(
                        "La marca " + brand.BrandCod + " ya existe"
                );
            }
            brand.addSessionCreate(auditUser);
        }
        return brandRepository.saveAll(brandList);
    }

    private String auditUser(String userCod) {
        String cleanUser = clean(userCod);
        return cleanUser.isEmpty() ? "SISTEMA" : cleanUser;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
