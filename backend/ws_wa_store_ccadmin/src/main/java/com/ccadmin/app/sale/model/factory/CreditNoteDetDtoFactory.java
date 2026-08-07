package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.product.model.entity.ProductEntity;
import com.ccadmin.app.sale.model.dto.CreditNoteDetDto;
import com.ccadmin.app.sale.model.entity.CreditNoteDetEntity;

public final class CreditNoteDetDtoFactory {

    private CreditNoteDetDtoFactory() {
    }

    public static CreditNoteDetDto fromEntities(
            CreditNoteDetEntity detail,
            ProductEntity product
    ) {
        CreditNoteDetDto result = new CreditNoteDetDto();
        result.CreditNoteDet = detail;
        result.Product = product;
        return result;
    }
}
