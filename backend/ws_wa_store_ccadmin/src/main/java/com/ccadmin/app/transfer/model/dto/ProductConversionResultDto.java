package com.ccadmin.app.transfer.model.dto;

public class ProductConversionResultDto {

    public boolean valid;
    public Long atomicQuantity;
    public Long convertedQuantity;
    public String message;

    public ProductConversionResultDto(){

    }

    public ProductConversionResultDto(boolean valid, Long atomicQuantity, Long convertedQuantity, String message) {
        this.valid = valid;
        this.atomicQuantity = atomicQuantity;
        this.convertedQuantity = convertedQuantity;
        this.message = message;
    }

    public static ProductConversionResultDto ok(
            long atomicQuantity,
            long convertedQuantity,
            String message
    ) {
        return new ProductConversionResultDto(
                true,
                atomicQuantity,
                convertedQuantity,
                message
        );
    }

    public static ProductConversionResultDto error(String message) {
        return new ProductConversionResultDto(
                false,
                null,
                null,
                message
        );
    }

}
