package com.ccadmin.app.inventory.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class StockEntryDetId implements Serializable {
    public String StockEntryCod;
    public Integer ItemNumber;

    public StockEntryDetId() {
    }

    public StockEntryDetId(String stockEntryCod, Integer itemNumber) {
        StockEntryCod = stockEntryCod;
        ItemNumber = itemNumber;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) return true;
        if (!(value instanceof StockEntryDetId that)) return false;
        return Objects.equals(StockEntryCod, that.StockEntryCod)
                && Objects.equals(ItemNumber, that.ItemNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(StockEntryCod, ItemNumber);
    }
}
