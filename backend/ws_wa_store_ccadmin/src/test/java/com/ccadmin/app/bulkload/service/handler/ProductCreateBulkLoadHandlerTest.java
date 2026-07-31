package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadSourceRowDto;
import com.ccadmin.app.product.model.entity.BrandEntity;
import com.ccadmin.app.product.model.entity.CategoryEntity;
import com.ccadmin.app.product.repository.*;
import com.ccadmin.app.product.service.ProductCreateService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ProductCreateBulkLoadHandlerTest {

    @Test
    void resolvesBrandAndCategoryByNameWhenCodeDoesNotExist() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductBarcodeRepository barcodeRepository =
                mock(ProductBarcodeRepository.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ProductCreateService productCreateService =
                mock(ProductCreateService.class);

        BrandEntity brand = new BrandEntity();
        brand.BrandCod = "MAR001";
        brand.BrandName = "ACME";
        CategoryEntity category = new CategoryEntity();
        category.CategoryCod = "CAT001";
        category.CategoryName = "TECNOLOGIA";
        category.CategoryDadCod = "PAD001";
        category.IsCategoryDad = "N";

        when(brandRepository.findById("ACME")).thenReturn(Optional.empty());
        when(brandRepository.findFirstActiveByName("ACME"))
                .thenReturn(Optional.of(brand));
        when(categoryRepository.findById("TECNOLOGIA"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findFirstActiveNoDadByName("TECNOLOGIA"))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsById("PAD001")).thenReturn(true);

        ProductCreateBulkLoadHandler handler =
                new ProductCreateBulkLoadHandler(
                        productRepository,
                        barcodeRepository,
                        brandRepository,
                        categoryRepository,
                        productCreateService
                );
        BulkLoadPreparedDto prepared = handler.prepare(request(
                "PRO001", "ACME", "TECNOLOGIA"
        ));

        assertTrue(prepared.ErrorList.isEmpty());
        assertEquals(
                "MAR001", prepared.DetailList.getFirst().Payload.get("BrandCod")
        );
        assertEquals(
                "CAT001",
                prepared.DetailList.getFirst().Payload.get("CategoryCod")
        );
    }

    @Test
    void prioritizesExistingCodeOverNameLookup() {
        ProductRepository productRepository = mock(ProductRepository.class);
        ProductBarcodeRepository barcodeRepository =
                mock(ProductBarcodeRepository.class);
        BrandRepository brandRepository = mock(BrandRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ProductCreateService productCreateService =
                mock(ProductCreateService.class);

        BrandEntity brand = new BrandEntity();
        brand.BrandCod = "MAR777";
        brand.BrandName = "OTRO NOMBRE";
        CategoryEntity category = new CategoryEntity();
        category.CategoryCod = "CAT777";
        category.CategoryDadCod = "PAD001";
        category.IsCategoryDad = "N";
        when(brandRepository.findById("MAR777"))
                .thenReturn(Optional.of(brand));
        when(categoryRepository.findById("CAT777"))
                .thenReturn(Optional.of(category));
        when(categoryRepository.existsById("PAD001")).thenReturn(true);

        ProductCreateBulkLoadHandler handler =
                new ProductCreateBulkLoadHandler(
                        productRepository,
                        barcodeRepository,
                        brandRepository,
                        categoryRepository,
                        productCreateService
                );
        BulkLoadPreparedDto prepared = handler.prepare(request(
                "PRO001", "MAR777", "CAT777"
        ));

        assertTrue(prepared.ErrorList.isEmpty());
        verify(brandRepository, never()).findFirstActiveByName(anyString());
        verify(categoryRepository, never())
                .findFirstActiveNoDadByName(anyString());
    }

    private BulkLoadParsedRequestDto request(String productCod,
                                             String brand,
                                             String category) {
        BulkLoadSourceRowDto row = new BulkLoadSourceRowDto();
        row.RowNumber = 4;
        row.Payload.put("ProductCod", productCod);
        row.Payload.put("ProductName", "Producto de prueba");
        row.Payload.put("ProductDesc", "");
        row.Payload.put("BrandCod", brand);
        row.Payload.put("CategoryCod", category);
        row.Payload.put("BarCode", "");
        row.Payload.put("NumPrice", 10);
        row.Payload.put("NumMaxStock", 20);
        row.Payload.put("NumMinStock", 1);
        BulkLoadParsedRequestDto request = new BulkLoadParsedRequestDto();
        request.RowList.add(row);
        return request;
    }
}
