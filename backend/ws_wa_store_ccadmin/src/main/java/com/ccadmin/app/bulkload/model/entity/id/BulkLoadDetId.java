package com.ccadmin.app.bulkload.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class BulkLoadDetId implements Serializable {
    public String BulkLoadCod;
    public Integer ItemNumber;

    public BulkLoadDetId() {
    }

    public BulkLoadDetId(String bulkLoadCod, Integer itemNumber) {
        this.BulkLoadCod = bulkLoadCod;
        this.ItemNumber = itemNumber;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BulkLoadDetId that)) return false;
        return Objects.equals(BulkLoadCod, that.BulkLoadCod)
                && Objects.equals(ItemNumber, that.ItemNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(BulkLoadCod, ItemNumber);
    }
}
