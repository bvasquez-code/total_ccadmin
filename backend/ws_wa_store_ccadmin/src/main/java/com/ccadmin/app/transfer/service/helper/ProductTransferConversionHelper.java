package com.ccadmin.app.transfer.service.helper;

import com.ccadmin.app.product.model.entity.ProductConfigEntity;
import com.ccadmin.app.product.shared.ProductSearchShared;
import com.ccadmin.app.transfer.exception.TransferException;
import com.ccadmin.app.transfer.model.dto.ProductConversionRequestDto;
import com.ccadmin.app.transfer.model.dto.ProductConversionResultDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProductTransferConversionHelper {

    @Autowired
    private ProductSearchShared productSearchShared;

    public ProductConversionResultDto validateConvertProductBetweenStores(ProductConversionRequestDto request) {
        ProductConfigEntity productConfigOrigin = this.productSearchShared
                .findConfigByIdAndStore(request.ProductCod, request.StoredCodOrigin);
        ProductConfigEntity productConfigDestination = this.productSearchShared
                .findConfigByIdAndStore(request.ProductCod, request.StoredCodDestination);

        return validateQuantityConversion(
                request.quantityToConvert,
                productConfigOrigin.ProductUnitFactor,
                productConfigDestination.ProductUnitFactor
        );
    }

    public void validateInternalQuantityBetweenStoresOrThrow(
            String productCod,
            long internalQuantity,
            String originStoreCod,
            String destinationStoreCod,
            ProductConfigEntity productConfigOrigin,
            ProductConfigEntity productConfigDestination
    ) throws TransferException {
        ProductConversionResultDto result = this.validateInternalQuantityBetweenStores(
                internalQuantity,
                productConfigOrigin.ProductUnitFactor,
                productConfigDestination.ProductUnitFactor
        );

        if (!result.valid) {
            throw new TransferException(
                    "No se puede transferir el producto " + productCod +
                            " desde el local " + originStoreCod +
                            " hacia el local " + destinationStoreCod +
                            ". " + result.message
            );
        }
    }

    public ProductConversionResultDto validateInternalQuantityBetweenStores(
            long internalQuantity,
            long originFactor,
            long destinationFactor
    ) {
        if (internalQuantity <= 0) {
            return ProductConversionResultDto.error(
                    "La cantidad a convertir debe ser mayor a cero."
            );
        }

        if (originFactor <= 0) {
            return ProductConversionResultDto.error(
                    "El factor del local origen debe ser mayor a cero."
            );
        }

        if (internalQuantity % originFactor != 0) {
            return ProductConversionResultDto.error(
                    "La cantidad en unidad minima es " + internalQuantity +
                            ", pero no puede dividirse exactamente entre el factor origen " + originFactor + "."
            );
        }

        return validateQuantityConversion(
                internalQuantity / originFactor,
                originFactor,
                destinationFactor
        );
    }

    public ProductConversionResultDto validateQuantityConversion(
            long quantityToConvert,
            long originFactor,
            long destinationFactor
    ) {
        if (quantityToConvert <= 0) {
            return ProductConversionResultDto.error(
                    "La cantidad a convertir debe ser mayor a cero."
            );
        }

        if (originFactor <= 0) {
            return ProductConversionResultDto.error(
                    "El factor del local origen debe ser mayor a cero."
            );
        }

        if (destinationFactor <= 0) {
            return ProductConversionResultDto.error(
                    "El factor del local destino debe ser mayor a cero."
            );
        }

        final long atomicQuantity;

        try {
            atomicQuantity = Math.multiplyExact(quantityToConvert, originFactor);
        } catch (ArithmeticException ex) {
            return ProductConversionResultDto.error(
                    "La cantidad convertida supera el limite permitido."
            );
        }

        long remainder = atomicQuantity % destinationFactor;

        if (remainder != 0) {
            long integerPart = atomicQuantity / destinationFactor;

            return ProductConversionResultDto.error(
                    "No es posible realizar la conversion. " +
                            "La cantidad en unidad minima es " + atomicQuantity +
                            ", pero no puede dividirse exactamente entre el factor destino " + destinationFactor +
                            ". Resultado parcial: " + integerPart + " con residuo " + remainder + "."
            );
        }

        long convertedQuantity = atomicQuantity / destinationFactor;

        return ProductConversionResultDto.ok(
                atomicQuantity,
                convertedQuantity,
                "Conversion valida."
        );
    }
}
