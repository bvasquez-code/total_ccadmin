package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.SaleDetailSplitLineDto;
import com.ccadmin.app.sale.model.entity.PresaleDetWarehouseEntity;
import com.ccadmin.app.sale.model.entity.SaleDetEntity;
import com.ccadmin.app.sale.model.entity.SaleDetWarehouseEntity;

public final class SaleDetWarehouseEntityFactory {

    private SaleDetWarehouseEntityFactory() {
    }

    public static SaleDetWarehouseEntity fromPresale(
            PresaleDetWarehouseEntity presaleWarehouse,
            String saleCod
    ) {
        SaleDetWarehouseEntity warehouse = new SaleDetWarehouseEntity();
        warehouse.SaleCod = saleCod;
        warehouse.ItemNumber = presaleWarehouse.ItemNumber;
        warehouse.ProductCod = presaleWarehouse.ProductCod;
        warehouse.Variant = presaleWarehouse.Variant;
        warehouse.WarehouseCod = presaleWarehouse.WarehouseCod;
        warehouse.NumUnit = presaleWarehouse.NumUnit;
        warehouse.ProductUnitName = presaleWarehouse.ProductUnitName;
        warehouse.ProductUnitFactor = presaleWarehouse.ProductUnitFactor;
        warehouse.LotNumber = presaleWarehouse.LotNumber;
        warehouse.ExpirationDate = presaleWarehouse.ExpirationDate;
        return warehouse;
    }

    public static SaleDetWarehouseEntity copyForItem(
            SaleDetWarehouseEntity source,
            int itemNumber
    ) {
        SaleDetWarehouseEntity warehouse = copyBase(source, itemNumber);
        warehouse.NumUnit = source.NumUnit;
        warehouse.LotNumber = source.LotNumber;
        warehouse.ExpirationDate = source.ExpirationDate;
        return warehouse;
    }

    public static SaleDetWarehouseEntity fromPickedDetail(
            SaleDetWarehouseEntity source,
            SaleDetEntity pickedDetail,
            SaleDetailSplitLineDto splitLine
    ) {
        SaleDetWarehouseEntity warehouse = copyBase(source, splitLine.ItemNumber);
        warehouse.SaleCod = pickedDetail.SaleCod;
        warehouse.ProductCod = pickedDetail.ProductCod;
        warehouse.Variant = pickedDetail.Variant;
        warehouse.NumUnit = splitLine.NumUnit;
        warehouse.ProductUnitName = pickedDetail.ProductUnitName;
        warehouse.ProductUnitFactor = pickedDetail.ProductUnitFactor;
        warehouse.LotNumber = splitLine.LotNumber;
        warehouse.ExpirationDate = splitLine.ExpirationDate;
        return warehouse;
    }

    private static SaleDetWarehouseEntity copyBase(
            SaleDetWarehouseEntity source,
            int itemNumber
    ) {
        SaleDetWarehouseEntity warehouse = new SaleDetWarehouseEntity();
        warehouse.SaleCod = source.SaleCod;
        warehouse.ItemNumber = itemNumber;
        warehouse.ProductCod = source.ProductCod;
        warehouse.Variant = source.Variant;
        warehouse.WarehouseCod = source.WarehouseCod;
        warehouse.ProductUnitName = source.ProductUnitName;
        warehouse.ProductUnitFactor = source.ProductUnitFactor;
        warehouse.CreationUser = source.CreationUser;
        warehouse.CreationDate = source.CreationDate;
        warehouse.ModifyUser = source.ModifyUser;
        warehouse.ModifyDate = source.ModifyDate;
        warehouse.Status = source.Status;
        return warehouse;
    }
}
