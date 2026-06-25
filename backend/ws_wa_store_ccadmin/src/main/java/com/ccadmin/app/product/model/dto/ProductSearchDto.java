package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.shared.model.dto.SearchDto;

public class ProductSearchDto extends SearchDto {

    public String BrandCod;
    public String CategoryCod;
    public int StockMin;
    public String SortedBy;
    public String DirectionSortedBy;

}
