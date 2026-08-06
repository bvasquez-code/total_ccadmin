package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.repository.CategoryRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductCategoryDigitalPolicy {

    private final CategoryRepository categoryRepository;

    public ProductCategoryDigitalPolicy(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public void apply(String categoryCod, ProductConfigEntity productConfig) {
        if (categoryCod == null || categoryCod.isBlank()) {
            throw new IllegalArgumentException("Debe seleccionar una categoria");
        }
        if (productConfig == null) {
            throw new IllegalArgumentException("Debe ingresar la configuracion del producto");
        }

        CategoryEntity category = this.categoryRepository.findByCategoryCodNative(categoryCod)
                .orElseThrow(() -> new IllegalArgumentException("No existe la categoria " + categoryCod));
        if ("S".equalsIgnoreCase(category.IsDigital)) {
            productConfig.IsDigital = "S";
        }
    }
}
