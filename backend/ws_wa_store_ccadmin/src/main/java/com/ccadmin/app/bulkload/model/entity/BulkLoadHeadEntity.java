package com.ccadmin.app.bulkload.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Map;

@Entity
@Table(name = "bulk_load_head")
public class BulkLoadHeadEntity extends AuditTableEntity implements Serializable {
    @Id
    public String BulkLoadCod;
    public String BulkLoadType;
    public Integer SchemaVersion;
    public String ProcessStatus;
    public String SourceFileCod;
    public String ErrorFileCod;
    public String OriginalFileName;
    public String FileHash;
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> Parameters;
    public Integer NumSourceRows;
    public Integer NumDestinations;
    public Integer NumTotalDetails;
    public Integer NumProcessedDetails;
    public Integer NumSuccessDetails;
    public Integer NumErrorDetails;
    public Integer NumWarningDetails;
    public BigDecimal ProgressPercent;
    public Date ValidationDate;
    public Date QueueDate;
    public Date StartDate;
    public Date EndDate;
    public Date LastHeartbeatDate;
    public String StatusMessage;
    public Integer AttemptCount;
}
