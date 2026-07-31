package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.*;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.product.model.dto.ProductRegisterDto;
import com.ccadmin.app.product.model.dto.ProductRegisterMassiveDto;
import com.ccadmin.app.product.model.entity.*;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.product.service.ProductCreateService;
import com.ccadmin.app.shared.model.myconst.StatusConst;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
public class ProductCreateBulkLoadHandler implements BulkLoadTypeHandler {
    private static final String SHEET = "PRODUCTOS";

    private final ProductRepository productRepository;
    private final ProductBarcodeRepository productBarcodeRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCreateService productCreateService;

    public ProductCreateBulkLoadHandler(
            ProductRepository productRepository,
            ProductBarcodeRepository productBarcodeRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            ProductCreateService productCreateService
    ) {
        this.productRepository = productRepository;
        this.productBarcodeRepository = productBarcodeRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productCreateService = productCreateService;
    }

    @Override
    public String type() {
        return BulkLoadConstants.TYPE_PRODUCT_CREATE;
    }

    @Override
    public BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        BulkLoadPreparedDto prepared = new BulkLoadPreparedDto();
        if (request.RowList == null || request.RowList.isEmpty()) {
            prepared.ErrorList.add(BulkLoadHandlerSupport.error(
                    SHEET, 0, "ProductCod", "", "ROW_REQUIRED",
                    "Debe registrar al menos un producto"
            ));
            return prepared;
        }

        Set<String> productCodes = new HashSet<>();
        Set<String> barcodes = new HashSet<>();
        for (BulkLoadSourceRowDto row : request.RowList) {
            BulkLoadPreparedDetailDto detail =
                    new BulkLoadPreparedDetailDto();
            int rowNumber = BulkLoadHandlerSupport.sourceRow(row.RowNumber);
            detail.SourceRowNumber = rowNumber;
            detail.StoreCod = null;
            detail.Payload.putAll(BulkLoadHandlerSupport.payload(row));

            String productCod = value(row, "ProductCod");
            String productName = value(row, "ProductName");
            String productDesc = value(row, "ProductDesc");
            String barcode = value(row, "BarCode");
            String brandInput = value(row, "BrandCod");
            String categoryInput = value(row, "CategoryCod");
            BrandEntity brand = resolveBrand(brandInput).orElse(null);
            CategoryEntity category =
                    resolveProductCategory(categoryInput).orElse(null);

            detail.BusinessKey = productCod;
            detail.Payload.put("ProductCod", productCod);
            detail.Payload.put("ProductName", productName);
            detail.Payload.put("ProductDesc", productDesc);
            detail.Payload.put("BarCode", barcode);
            detail.Payload.put("BrandInput", brandInput);
            detail.Payload.put(
                    "BrandCod", brand == null ? "" : brand.BrandCod
            );
            detail.Payload.put("CategoryInput", categoryInput);
            detail.Payload.put(
                    "CategoryCod", category == null ? "" : category.CategoryCod
            );

            validateRequiredAndLength(
                    prepared, detail, rowNumber, "ProductCod",
                    productCod, 20, "El codigo de producto"
            );
            validateRequiredAndLength(
                    prepared, detail, rowNumber, "ProductName",
                    productName, 128, "El nombre de producto"
            );
            if (productDesc.length() > 256) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "ProductDesc", productDesc,
                        "PRODUCT_DESCRIPTION_LENGTH",
                        "La descripcion admite hasta 256 caracteres"
                ));
            }
            if (barcode.length() > 20) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "BarCode", barcode,
                        "BARCODE_LENGTH",
                        "El codigo de barras admite hasta 20 caracteres"
                ));
            }
            if (!productCod.isEmpty()
                    && !productCodes.add(productCod.toUpperCase(Locale.ROOT))) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "ProductCod", productCod,
                        "PRODUCT_DUPLICATED",
                        "El codigo de producto esta repetido en el archivo"
                ));
            }
            if (!productCod.isEmpty()
                    && productRepository.existsById(productCod)) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "ProductCod", productCod,
                        "PRODUCT_ALREADY_EXISTS",
                        "El codigo de producto ya existe"
                ));
            }
            if (brandInput.isEmpty() || brand == null) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "BrandCod", brandInput,
                        "BRAND_NOT_FOUND",
                        "No existe una marca activa con ese codigo o nombre"
                ));
            }
            if (categoryInput.isEmpty() || category == null) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "CategoryCod", categoryInput,
                        "CATEGORY_NOT_FOUND",
                        "No existe una categoria de producto activa con ese codigo o nombre"
                ));
            } else if (BulkLoadHandlerSupport.text(category.CategoryDadCod).isEmpty()
                    || !categoryRepository.existsById(category.CategoryDadCod)) {
                addError(prepared, detail, BulkLoadHandlerSupport.error(
                        SHEET, rowNumber, "CategoryCod", categoryInput,
                        "CATEGORY_PARENT_REQUIRED",
                        "La categoria seleccionada no tiene una categoria padre valida"
                ));
            }
            if (!barcode.isEmpty()) {
                String normalizedBarcode = barcode.toUpperCase(Locale.ROOT);
                if (!barcodes.add(normalizedBarcode)) {
                    addError(prepared, detail, BulkLoadHandlerSupport.error(
                            SHEET, rowNumber, "BarCode", barcode,
                            "BARCODE_DUPLICATED",
                            "El codigo de barras esta repetido en el archivo"
                    ));
                } else if (productBarcodeRepository.existsById(barcode)) {
                    addError(prepared, detail, BulkLoadHandlerSupport.error(
                            SHEET, rowNumber, "BarCode", barcode,
                            "BARCODE_ALREADY_EXISTS",
                            "El codigo de barras ya se encuentra registrado"
                    ));
                }
            }

            putDecimal(prepared, detail, row, rowNumber, "NumPrice");
            putInteger(prepared, detail, row, rowNumber, "NumMaxStock");
            putInteger(prepared, detail, row, rowNumber, "NumMinStock");
            prepared.DetailList.add(detail);
        }
        return prepared;
    }

    @Override
    public void execute(BulkLoadHeadEntity head,
                        List<BulkLoadDetEntity> detailList,
                        String userCod,
                        String resourceCode) {
        ProductRegisterMassiveDto request = new ProductRegisterMassiveDto();
        for (BulkLoadDetEntity detail : detailList) {
            ProductRegisterDto register = new ProductRegisterDto();
            ProductEntity product = new ProductEntity();
            product.ProductCod = text(detail, "ProductCod");
            product.ProductName = text(detail, "ProductName");
            product.ProductDesc = text(detail, "ProductDesc");
            product.BrandCod = text(detail, "BrandCod");
            product.CategoryCod = text(detail, "CategoryCod");
            register.product = product;

            ProductConfigEntity config = new ProductConfigEntity();
            config.ProductCod = product.ProductCod;
            config.NumPrice = BulkLoadHandlerSupport.decimal(
                    detail.Payload.get("NumPrice")
            );
            config.NumMaxStock = BulkLoadHandlerSupport.integer(
                    detail.Payload.get("NumMaxStock")
            );
            config.NumMinStock = BulkLoadHandlerSupport.integer(
                    detail.Payload.get("NumMinStock")
            );
            config.ProductUnitName = "NIU";
            config.ProductUnitFactor = 1;
            register.config = config;

            ProductBarcodeEntity barcode = new ProductBarcodeEntity();
            barcode.ProductCod = product.ProductCod;
            barcode.BarCode = text(detail, "BarCode");
            register.productBarcode = barcode;
            request.productList.add(register);
        }

        productCreateService.createBulk(request, userCod);
        for (BulkLoadDetEntity detail : detailList) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("ProductCod", text(detail, "ProductCod"));
            result.put("Created", true);
            detail.ResultData = result;
        }
    }

    private Optional<BrandEntity> resolveBrand(String input) {
        if (input.isEmpty()) return Optional.empty();
        Optional<BrandEntity> byCode = brandRepository.findById(input)
                .filter(item -> StatusConst.ACTIVE.equals(item.Status));
        return byCode.isPresent()
                ? byCode : brandRepository.findFirstActiveByName(input);
    }

    private Optional<CategoryEntity> resolveProductCategory(String input) {
        if (input.isEmpty()) return Optional.empty();
        Optional<CategoryEntity> byCode = categoryRepository.findById(input)
                .filter(item -> StatusConst.ACTIVE.equals(item.Status));
        Optional<CategoryEntity> resolved = byCode.isPresent()
                ? byCode : categoryRepository.findFirstActiveNoDadByName(input);
        return resolved.filter(item ->
                !"S".equalsIgnoreCase(
                        BulkLoadHandlerSupport.text(item.IsCategoryDad)
                )
        );
    }

    private String value(BulkLoadSourceRowDto row, String field) {
        return BulkLoadHandlerSupport.text(
                BulkLoadHandlerSupport.sourceValue(row, field)
        );
    }

    private String text(BulkLoadDetEntity detail, String field) {
        return BulkLoadHandlerSupport.text(detail.Payload.get(field));
    }

    private void validateRequiredAndLength(
            BulkLoadPreparedDto prepared,
            BulkLoadPreparedDetailDto detail,
            int rowNumber,
            String field,
            String value,
            int maxLength,
            String label
    ) {
        if (value.isEmpty()) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, value,
                    field.toUpperCase(Locale.ROOT) + "_REQUIRED",
                    label + " es obligatorio"
            ));
        } else if (value.length() > maxLength) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, value,
                    field.toUpperCase(Locale.ROOT) + "_LENGTH",
                    label + " admite hasta " + maxLength + " caracteres"
            ));
        }
    }

    private void putDecimal(BulkLoadPreparedDto prepared,
                            BulkLoadPreparedDetailDto detail,
                            BulkLoadSourceRowDto row,
                            int rowNumber,
                            String field) {
        Object rawValue = BulkLoadHandlerSupport.sourceValue(row, field);
        try {
            BigDecimal value = BulkLoadHandlerSupport.decimal(rawValue);
            if (value.signum() < 0 || value.scale() > 2
                    || value.precision() - value.scale() > 14) {
                throw new NumberFormatException();
            }
            detail.Payload.put(field, value);
        } catch (RuntimeException exception) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, rawValue, "NUMBER_FORMAT",
                    field + " debe cumplir NUMERO(16,2) y no ser negativo"
            ));
            detail.Payload.put(field, BigDecimal.ZERO);
        }
    }

    private void putInteger(BulkLoadPreparedDto prepared,
                            BulkLoadPreparedDetailDto detail,
                            BulkLoadSourceRowDto row,
                            int rowNumber,
                            String field) {
        Object rawValue = BulkLoadHandlerSupport.sourceValue(row, field);
        try {
            int value = BulkLoadHandlerSupport.integer(rawValue);
            if (value < 0) throw new NumberFormatException();
            detail.Payload.put(field, value);
        } catch (RuntimeException exception) {
            addError(prepared, detail, BulkLoadHandlerSupport.error(
                    SHEET, rowNumber, field, rawValue, "INTEGER_FORMAT",
                    field + " debe ser un numero entero no negativo"
            ));
            detail.Payload.put(field, 0);
        }
    }

    private void addError(BulkLoadPreparedDto prepared,
                          BulkLoadPreparedDetailDto detail,
                          BulkLoadErrorDto error) {
        prepared.ErrorList.add(error);
        detail.ErrorList.add(error);
    }
}
