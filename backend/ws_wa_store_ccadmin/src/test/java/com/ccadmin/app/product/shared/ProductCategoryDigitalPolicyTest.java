package com.ccadmin.app.product.shared;

import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCategoryDigitalPolicyTest {

    @Mock
    private CategoryRepository categoryRepository;
    @InjectMocks
    private ProductCategoryDigitalPolicy productCategoryDigitalPolicy;

    @Test
    void forcesDigitalProductWhenCategoryIsDigital() {
        CategoryEntity category = category("CAT-DIG", "S");
        ProductConfigEntity config = new ProductConfigEntity();
        config.IsDigital = "N";
        when(categoryRepository.findByCategoryCodNative(category.CategoryCod))
                .thenReturn(Optional.of(category));

        productCategoryDigitalPolicy.apply(category.CategoryCod, config);

        assertEquals("S", config.IsDigital);
    }

    @Test
    void preservesProductChoiceWhenCategoryIsRegular() {
        CategoryEntity category = category("CAT-REG", "N");
        ProductConfigEntity config = new ProductConfigEntity();
        config.IsDigital = "N";
        when(categoryRepository.findByCategoryCodNative(category.CategoryCod))
                .thenReturn(Optional.of(category));

        productCategoryDigitalPolicy.apply(category.CategoryCod, config);

        assertEquals("N", config.IsDigital);
    }

    @Test
    void rejectsUnknownCategory() {
        ProductConfigEntity config = new ProductConfigEntity();
        when(categoryRepository.findByCategoryCodNative("CAT-X"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productCategoryDigitalPolicy.apply("CAT-X", config)
        );

        assertEquals("No existe la categoria CAT-X", exception.getMessage());
    }

    private CategoryEntity category(String categoryCod, String isDigital) {
        CategoryEntity category = new CategoryEntity();
        category.CategoryCod = categoryCod;
        category.IsDigital = isDigital;
        return category;
    }
}
