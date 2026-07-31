package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.repository.CategoryRepository;
import com.ccadmin.app.product.service.CategoryCreateService;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CategoryCreateBulkLoadHandler implements BulkLoadTypeHandler {
    private static final String SHEET = "CATEGORIAS";

    private final CategoryRepository categoryRepository;
    private final CategoryCreateService categoryCreateService;

    public CategoryCreateBulkLoadHandler(
            CategoryRepository categoryRepository,
            CategoryCreateService categoryCreateService
    ) {
        this.categoryRepository = categoryRepository;
        this.categoryCreateService = categoryCreateService;
    }

    @Override
    public String type() {
        return BulkLoadConstants.TYPE_CATEGORY_CREATE;
    }

    @Override
    public BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        BulkLoadPreparedDto prepared = new BulkLoadPreparedDto();
        if (request.RowList == null || request.RowList.isEmpty()) {
            prepared.ErrorList.add(BulkLoadHandlerSupport.error(
                    SHEET, 0, "CategoryCod", "", "ROW_REQUIRED",
                    "Debe registrar al menos una categoria"
            ));
            return prepared;
        }
        Set<String> codes = new HashSet<>();
        for (BulkLoadSourceRowDto row : request.RowList) {
            int rowNumber = BulkLoadHandlerSupport.sourceRow(row.RowNumber);
            String code = value(row, "CategoryCod");
            String name = value(row, "CategoryName");
            String parentInput = value(row, "CategoryDadName");
            String isDigital = flag(value(row, "IsDigital"));
            String isCategoryDad = flag(value(row, "IsCategoryDad"));
            CategoryEntity parent = resolveCategory(parentInput).orElse(null);

            BulkLoadPreparedDetailDto detail =
                    new BulkLoadPreparedDetailDto();
            detail.SourceRowNumber = rowNumber;
            detail.BusinessKey = code;
            detail.Payload.putAll(BulkLoadHandlerSupport.payload(row));
            detail.Payload.put("CategoryCod", code);
            detail.Payload.put("CategoryName", name);
            detail.Payload.put("CategoryDadInput", parentInput);
            detail.Payload.put(
                    "CategoryDadCod",
                    parent == null ? "" : parent.CategoryCod
            );
            detail.Payload.put("IsDigital", isDigital);
            detail.Payload.put("IsCategoryDad", isCategoryDad);

            validate(prepared, detail, rowNumber, "CategoryCod", code, 10);
            validate(prepared, detail, rowNumber, "CategoryName", name, 128);
            if (!code.isEmpty()
                    && !codes.add(code.toUpperCase(Locale.ROOT))) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "CategoryCod", code,
                        "CATEGORY_DUPLICATED",
                        "El codigo de categoria esta repetido en el archivo"
                ));
            }
            if (!code.isEmpty() && categoryRepository.existsById(code)) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "CategoryCod", code,
                        "CATEGORY_ALREADY_EXISTS",
                        "El codigo de categoria ya existe"
                ));
            }
            if (!parentInput.isEmpty() && parent == null) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "CategoryDadName", parentInput,
                        "CATEGORY_PARENT_NOT_FOUND",
                        "No existe una categoria padre activa con ese codigo o nombre"
                ));
            }
            validateFlag(
                    prepared, detail, rowNumber, "IsDigital", isDigital
            );
            validateFlag(
                    prepared, detail, rowNumber,
                    "IsCategoryDad", isCategoryDad
            );
            prepared.DetailList.add(detail);
        }
        return prepared;
    }

    @Override
    public void execute(BulkLoadHeadEntity head,
                        List<BulkLoadDetEntity> detailList,
                        String userCod,
                        String resourceCode) {
        List<CategoryEntity> categoryList = new ArrayList<>();
        for (BulkLoadDetEntity detail : detailList) {
            CategoryEntity category = new CategoryEntity();
            category.CategoryCod = text(detail, "CategoryCod");
            category.CategoryName = text(detail, "CategoryName");
            category.CategoryDadCod = nullableText(
                    detail, "CategoryDadCod"
            );
            category.IsDigital = text(detail, "IsDigital");
            category.IsCategoryDad = text(detail, "IsCategoryDad");
            categoryList.add(category);
        }
        categoryCreateService.createBulk(categoryList, userCod);
        for (BulkLoadDetEntity detail : detailList) {
            detail.ResultData = Map.of(
                    "CategoryCod", text(detail, "CategoryCod"),
                    "Created", true
            );
        }
    }

    private Optional<CategoryEntity> resolveCategory(String input) {
        if (input.isEmpty()) return Optional.empty();
        Optional<CategoryEntity> byCode = categoryRepository.findById(input)
                .filter(item -> StatusConst.ACTIVE.equals(item.Status))
                .filter(item -> "S".equalsIgnoreCase(
                        BulkLoadHandlerSupport.text(item.IsCategoryDad)
                ));
        return byCode.isPresent()
                ? byCode : categoryRepository.findFirstActiveDadByName(input);
    }

    private String value(BulkLoadSourceRowDto row, String field) {
        return BulkLoadHandlerSupport.text(
                BulkLoadHandlerSupport.sourceValue(row, field)
        );
    }

    private String text(BulkLoadDetEntity detail, String field) {
        return BulkLoadHandlerSupport.text(detail.Payload.get(field));
    }

    private String nullableText(BulkLoadDetEntity detail, String field) {
        String value = text(detail, field);
        return value.isEmpty() ? null : value;
    }

    private String flag(String value) {
        return value.isEmpty() ? "N" : value.toUpperCase(Locale.ROOT);
    }

    private void validate(BulkLoadPreparedDto prepared,
                          BulkLoadPreparedDetailDto detail,
                          int rowNumber,
                          String field,
                          String value,
                          int length) {
        if (value.isEmpty()) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, value,
                    field.toUpperCase(Locale.ROOT) + "_REQUIRED",
                    field + " es obligatorio"
            ));
        } else if (value.length() > length) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, value,
                    field.toUpperCase(Locale.ROOT) + "_LENGTH",
                    field + " admite hasta " + length + " caracteres"
            ));
        }
    }

    private void validateFlag(BulkLoadPreparedDto prepared,
                              BulkLoadPreparedDetailDto detail,
                              int rowNumber,
                              String field,
                              String value) {
        if (!"S".equals(value) && !"N".equals(value)) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, value, "FLAG_FORMAT",
                    field + " solo admite S o N"
            ));
        }
    }

    private void addError(BulkLoadPreparedDto prepared,
                          BulkLoadPreparedDetailDto detail,
                          BulkLoadErrorDto error) {
        prepared.ErrorList.add(error);
        detail.ErrorList.add(error);
    }
}
