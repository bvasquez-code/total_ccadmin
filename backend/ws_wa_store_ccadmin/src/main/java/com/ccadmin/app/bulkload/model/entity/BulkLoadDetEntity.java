package com.ccadmin.app.bulkload.model.entity;

import com.ccadmin.app.bulkload.model.entity.id.BulkLoadDetId;
import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "bulk_load_det")
@IdClass(BulkLoadDetId.class)
public class BulkLoadDetEntity extends AuditTableEntity implements Serializable {
    @Id
    public String BulkLoadCod;
    @Id
    public Integer ItemNumber;
    public Integer SourceRowNumber;
    public String StoreCod;
    public String BusinessKey;
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> Payload;
    public String ProcessStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<Map<String, Object>> ErrorDetail;
    @JdbcTypeCode(SqlTypes.JSON)
    public List<Map<String, Object>> WarningDetail;
    @JdbcTypeCode(SqlTypes.JSON)
    public Map<String, Object> ResultData;
    public Integer AttemptCount;
    public Date StartDate;
    public Date EndDate;
}
