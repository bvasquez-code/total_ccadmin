package com.ccadmin.app.inventory.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class StockExitDetId implements Serializable {
    public String StockExitCod;
    public Integer ItemNumber;

    public StockExitDetId() {
    }

    public StockExitDetId(String stockExitCod, Integer itemNumber) {
        StockExitCod = stockExitCod;
        ItemNumber = itemNumber;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof StockExitDetId that)) return false;
        return Objects.equals(StockExitCod, that.StockExitCod)
                && Objects.equals(ItemNumber, that.ItemNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(StockExitCod, ItemNumber);
    }
}
