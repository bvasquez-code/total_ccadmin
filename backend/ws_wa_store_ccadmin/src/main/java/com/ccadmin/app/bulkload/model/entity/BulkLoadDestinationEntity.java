package com.ccadmin.app.bulkload.model.entity;

import com.ccadmin.app.bulkload.model.entity.id.BulkLoadDestinationId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Date;

@Entity
@Table(name = "bulk_load_destination")
@IdClass(BulkLoadDestinationId.class)
public class BulkLoadDestinationEntity extends AuditTableEntity implements Serializable {
    @Id
    public String BulkLoadCod;
    @Id
    public String StoreCod;
    public String ProcessStatus;
    public Integer NumTotalDetails;
    public Integer NumProcessedDetails;
    public Integer NumSuccessDetails;
    public Integer NumErrorDetails;
    public Date StartDate;
    public Date EndDate;
    public String StatusMessage;
}
