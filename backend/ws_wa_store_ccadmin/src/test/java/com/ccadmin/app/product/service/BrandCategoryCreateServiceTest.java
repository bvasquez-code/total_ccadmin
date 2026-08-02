package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.repository.BrandRepository;
import com.ccadmin.app.product.repository.CategoryRepository;
import com.ccadmin.app.system.shared.TableSequenceShared;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BrandCategoryCreateServiceTest {

    @Mock
    private BrandRepository brandRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TableSequenceShared tableSequenceShared;

    @Test
    void generateBrandCodeUsesSharedAvailableCodeCore() {
        BrandCreateService service = new BrandCreateService(
                brandRepository, tableSequenceShared
        );
        when(tableSequenceShared.getNextAvailableCode(eq("brand"), any()))
                .thenReturn("BR002");

        assertEquals("BR002", service.generateBrandCode());
        verify(tableSequenceShared).getNextAvailableCode(eq("brand"), any());
    }

    @Test
    void saveBrandGeneratesCodeWhenItIsEmpty() {
        BrandCreateService service = new BrandCreateService(
                brandRepository, tableSequenceShared
        );
        BrandEntity brand = new BrandEntity();
        brand.BrandName = "Nueva marca";
        when(tableSequenceShared.getNextAvailableCode(eq("brand"), any()))
                .thenReturn("BR001");
        when(brandRepository.existsById("BR001")).thenReturn(false);
        when(brandRepository.save(any(BrandEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        BrandEntity saved = service.save(brand, "USER01");

        assertEquals("BR001", saved.BrandCod);
        verify(brandRepository).save(brand);
    }

    @Test
    void generateCategoryCodeUsesSharedAvailableCodeCore() {
        CategoryCreateService service = new CategoryCreateService(
                categoryRepository, tableSequenceShared
        );
        when(tableSequenceShared.getNextAvailableCode(eq("category"), any()))
                .thenReturn("CA002");

        assertEquals("CA002", service.generateCategoryCode());
        verify(tableSequenceShared).getNextAvailableCode(eq("category"), any());
    }

    @Test
    void saveCategoryGeneratesCodeWhenItIsEmpty() {
        CategoryCreateService service = new CategoryCreateService(
                categoryRepository, tableSequenceShared
        );
        CategoryEntity category = new CategoryEntity();
        category.CategoryName = "Nueva categoria";
        when(tableSequenceShared.getNextAvailableCode(eq("category"), any()))
                .thenReturn("CA001");
        when(categoryRepository.existsById("CA001")).thenReturn(false);
        when(categoryRepository.save(any(CategoryEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryEntity saved = service.save(category, "USER01");

        assertEquals("CA001", saved.CategoryCod);
        verify(categoryRepository).save(category);
    }
}
