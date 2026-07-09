package com.ccadmin.app.system.model.entity.id;

import java.io.Serializable;

public class StoreSequenceID implements Serializable {

    public String StoreCod;
    public Integer PeriodId;
    public String SequenceTableType;

    public StoreSequenceID() {
    }

    public StoreSequenceID(String storeCod, Integer periodId, String sequenceTableType) {
        StoreCod = storeCod;
        PeriodId = periodId;
        SequenceTableType = sequenceTableType;
    }
}
