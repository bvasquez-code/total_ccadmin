package com.ccadmin.app.product.model.dto;

public class KardexZoneMovementDto {

    public String ZoneStockMoved;
    public int NumStockDelta;

    public KardexZoneMovementDto() {
    }

    public KardexZoneMovementDto(String zoneStockMoved, int numStockDelta) {
        this.ZoneStockMoved = zoneStockMoved;
        this.NumStockDelta = numStockDelta;
    }
}
