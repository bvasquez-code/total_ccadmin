package com.ccadmin.app.product.model.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ProductImageAnalysisDto {
    public String BrandCod;
    public BigDecimal NumPrice;
    public String BrandInput;
    public String CategoryCod;
    public String ProductDesc;
    public List<String> ProductNameList;
    public String CategoryInput;
    public String Barcode;

    public ProductImageAnalysisDto() {
        this.ProductNameList = new ArrayList<>();
    }
}
