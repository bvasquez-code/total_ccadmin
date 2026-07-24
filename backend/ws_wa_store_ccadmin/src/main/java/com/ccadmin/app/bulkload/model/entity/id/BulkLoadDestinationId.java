package com.ccadmin.app.bulkload.model.entity.id;

import java.io.Serializable;
import java.util.Objects;

public class BulkLoadDestinationId implements Serializable {
    public String BulkLoadCod;
    public String StoreCod;

    public BulkLoadDestinationId() {
    }

    public BulkLoadDestinationId(String bulkLoadCod, String storeCod) {
        this.BulkLoadCod = bulkLoadCod;
        this.StoreCod = storeCod;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof BulkLoadDestinationId that)) return false;
        return Objects.equals(BulkLoadCod, that.BulkLoadCod)
                && Objects.equals(StoreCod, that.StoreCod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(BulkLoadCod, StoreCod);
    }
}
