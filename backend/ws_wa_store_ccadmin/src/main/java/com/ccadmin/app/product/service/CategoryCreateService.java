package com.ccadmin.app.product.service;

import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.repository.CategoryRepository;
import com.ccadmin.app.system.shared.TableSequenceShared;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Centraliza las operaciones de escritura de categorias para flujos
 * unitarios y masivos.
 */
@Service
public class CategoryCreateService {
    private static final String CATEGORY_SEQUENCE_TYPE = "category";

    private final CategoryRepository categoryRepository;
    private final TableSequenceShared tableSequenceShared;

    public CategoryCreateService(CategoryRepository categoryRepository,
                                 TableSequenceShared tableSequenceShared) {
        this.categoryRepository = categoryRepository;
        this.tableSequenceShared = tableSequenceShared;
    }

    public CategoryEntity save(CategoryEntity category, String userCod) {
        if (category == null) {
            throw new IllegalArgumentException("Debe ingresar una categoria");
        }
        if (clean(category.CategoryCod).isEmpty()) {
            category.CategoryCod = generateCategoryCode();
        }
        boolean isNewCategory = !categoryRepository.existsById(category.CategoryCod);
        category.addSession(auditUser(userCod), isNewCategory);
        return categoryRepository.save(category);
    }

    public String generateCategoryCode() {
        return tableSequenceShared.getNextAvailableCode(
                CATEGORY_SEQUENCE_TYPE, categoryRepository::existsById
        );
    }

    @Transactional
    public List<CategoryEntity> createBulk(List<CategoryEntity> categoryList,
                                           String userCod) {
        if (categoryList == null || categoryList.isEmpty()) {
            throw new IllegalArgumentException(
                    "El bloque de categorias no tiene detalles"
            );
        }
        String auditUser = auditUser(userCod);
        for (CategoryEntity category : categoryList) {
            if (category == null || clean(category.CategoryCod).isEmpty()
                    || clean(category.CategoryName).isEmpty()) {
                throw new IllegalArgumentException(
                        "El codigo y nombre de categoria son obligatorios"
                );
            }
            if (categoryRepository.existsById(category.CategoryCod)) {
                throw new IllegalStateException(
                        "La categoria " + category.CategoryCod + " ya existe"
                );
            }
            category.addSessionCreate(auditUser);
        }
        return categoryRepository.saveAll(categoryList);
    }

    private String auditUser(String userCod) {
        String cleanUser = clean(userCod);
        return cleanUser.isEmpty() ? "SISTEMA" : cleanUser;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
