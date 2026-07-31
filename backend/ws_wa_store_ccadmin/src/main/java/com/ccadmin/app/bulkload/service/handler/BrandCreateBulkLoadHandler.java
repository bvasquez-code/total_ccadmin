package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.repository.BrandRepository;
import com.ccadmin.app.product.service.BrandCreateService;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BrandCreateBulkLoadHandler implements BulkLoadTypeHandler {
    private static final String SHEET = "MARCAS";

    private final BrandRepository brandRepository;
    private final BrandCreateService brandCreateService;

    public BrandCreateBulkLoadHandler(
            BrandRepository brandRepository,
            BrandCreateService brandCreateService
    ) {
        this.brandRepository = brandRepository;
        this.brandCreateService = brandCreateService;
    }

    @Override
    public String type() {
        return BulkLoadConstants.TYPE_BRAND_CREATE;
    }

    @Override
    public BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        BulkLoadPreparedDto prepared = new BulkLoadPreparedDto();
        if (request.RowList == null || request.RowList.isEmpty()) {
            prepared.ErrorList.add(BulkLoadHandlerSupport.error(
                    SHEET, 0, "BrandCod", "", "ROW_REQUIRED",
                    "Debe registrar al menos una marca"
            ));
            return prepared;
        }
        Set<String> codes = new HashSet<>();
        for (BulkLoadSourceRowDto row : request.RowList) {
            int rowNumber = BulkLoadHandlerSupport.sourceRow(row.RowNumber);
            String code = value(row, "BrandCod");
            String name = value(row, "BrandName");
            BulkLoadPreparedDetailDto detail =
                    new BulkLoadPreparedDetailDto();
            detail.SourceRowNumber = rowNumber;
            detail.BusinessKey = code;
            detail.Payload.putAll(BulkLoadHandlerSupport.payload(row));
            detail.Payload.put("BrandCod", code);
            detail.Payload.put("BrandName", name);

            validate(prepared, detail, rowNumber, "BrandCod", code, 10);
            validate(prepared, detail, rowNumber, "BrandName", name, 128);
            if (!code.isEmpty()
                    && !codes.add(code.toUpperCase(Locale.ROOT))) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "BrandCod", code,
                        "BRAND_DUPLICATED",
                        "El codigo de marca esta repetido en el archivo"
                ));
            }
            if (!code.isEmpty() && brandRepository.existsById(code)) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "BrandCod", code,
                        "BRAND_ALREADY_EXISTS",
                        "El codigo de marca ya existe"
                ));
            }
            prepared.DetailList.add(detail);
        }
        return prepared;
    }

    @Override
    public void execute(BulkLoadHeadEntity head,
                        List<BulkLoadDetEntity> detailList,
                        String userCod,
                        String resourceCode) {
        List<BrandEntity> brandList = new ArrayList<>();
        for (BulkLoadDetEntity detail : detailList) {
            BrandEntity brand = new BrandEntity();
            brand.BrandCod = text(detail, "BrandCod");
            brand.BrandName = text(detail, "BrandName");
            brandList.add(brand);
        }
        brandCreateService.createBulk(brandList, userCod);
        for (BulkLoadDetEntity detail : detailList) {
            detail.ResultData = Map.of(
                    "BrandCod", text(detail, "BrandCod"),
                    "Created", true
            );
        }
    }

    private String value(BulkLoadSourceRowDto row, String field) {
        return BulkLoadHandlerSupport.text(
                BulkLoadHandlerSupport.sourceValue(row, field)
        );
    }

    private String text(BulkLoadDetEntity detail, String field) {
        return BulkLoadHandlerSupport.text(detail.Payload.get(field));
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

    private void addError(BulkLoadPreparedDto prepared,
                          BulkLoadPreparedDetailDto detail,
                          BulkLoadErrorDto error) {
        prepared.ErrorList.add(error);
        detail.ErrorList.add(error);
    }
}
