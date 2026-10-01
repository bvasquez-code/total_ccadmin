package com.ccadmin.app.product.model.dto;

import com.ccadmin.app.shared.model.dto.SearchDto;

public class ProductSearchDto extends SearchDto {

    /** internal o ausente: todos; external: solo productos publicos. */
    public String SearchOrigin;
    public String BrandCod;
    public String CategoryCod;
    public int StockMin;
    public String SortedBy;
    public String DirectionSortedBy;

}
