package com.ccadmin.app.product.model.dto;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KardexZoneOperationDto {

    public String OperationCod;
    public int ItemNumber;
    public String SourceTable;
    public String MovementEvent;
    public String ProductCod;
    public String Variant;
    public String StoreCod;
    public String WarehouseCod;
    public String LotNumber;
    public Date ExpirationDate;
    public List<KardexZoneMovementDto> MovementList = new ArrayList<>();

    public KardexZoneOperationDto() {
    }
}
