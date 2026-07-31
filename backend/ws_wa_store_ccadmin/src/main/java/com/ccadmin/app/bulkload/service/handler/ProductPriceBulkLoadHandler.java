package com.ccadmin.app.bulkload.service.handler;

import com.ccadmin.app.bulkload.model.constants.BulkLoadConstants;
import com.ccadmin.app.bulkload.model.dto.BulkLoadParsedRequestDto;
import com.ccadmin.app.bulkload.model.dto.BulkLoadPreparedDto;
import com.ccadmin.app.bulkload.model.entity.BulkLoadDetEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceLineDto;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceResultDto;
import com.ccadmin.app.product.model.dto.ProductConfigBulkPriceUpdateDto;
import com.ccadmin.app.product.service.ProductConfigCreateService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductPriceBulkLoadHandler implements BulkLoadTypeHandler {
    private final ProductConfigCreateService productConfigCreateService;

    public ProductPriceBulkLoadHandler(
            ProductConfigCreateService productConfigCreateService
    ) {
        this.productConfigCreateService = productConfigCreateService;
    }

    @Override
    public String type() {
        return BulkLoadConstants.TYPE_PRODUCT_PRICE;
    }

    @Override
    public BulkLoadPreparedDto prepare(BulkLoadParsedRequestDto request) {
        return productConfigCreateService.prepareBulkPriceLoad(request);
    }

    @Override
    public void execute(BulkLoadHeadEntity head,
                        List<BulkLoadDetEntity> detailList,
                        String userCod,
                        String resourceCode) {
        ProductConfigBulkPriceUpdateDto request =
                new ProductConfigBulkPriceUpdateDto();
        for (BulkLoadDetEntity detail : detailList) {
            ProductConfigBulkPriceLineDto line =
                    new ProductConfigBulkPriceLineDto();
            line.ReferenceItemNumber = detail.ItemNumber;
            line.ProductCod = BulkLoadHandlerSupport.text(
                    detail.Payload.get("ProductCod")
            );
            line.StoreCod = BulkLoadHandlerSupport.text(
                    detail.Payload.get("StoreCod")
            );
            line.NumPrice = BulkLoadHandlerSupport.decimal(
                    detail.Payload.get("NumPrice")
            ).setScale(2);
            request.DetailList.add(line);
        }

        List<ProductConfigBulkPriceResultDto> resultList =
                productConfigCreateService.saveBulkPrices(request, userCod);
        Map<Integer, ProductConfigBulkPriceResultDto> resultMap =
                new HashMap<>();
        resultList.forEach(result ->
                resultMap.put(result.ReferenceItemNumber, result)
        );
        for (BulkLoadDetEntity detail : detailList) {
            ProductConfigBulkPriceResultDto businessResult =
                    resultMap.get(detail.ItemNumber);
            if (businessResult == null) {
                throw new IllegalStateException(
                        "No se obtuvo resultado para el item "
                                + detail.ItemNumber
                );
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("OldPrice", businessResult.OldPrice);
            result.put("NewPrice", businessResult.NewPrice);
            result.put("Changed", businessResult.Changed);
            detail.ResultData = result;
        }
    }
}
