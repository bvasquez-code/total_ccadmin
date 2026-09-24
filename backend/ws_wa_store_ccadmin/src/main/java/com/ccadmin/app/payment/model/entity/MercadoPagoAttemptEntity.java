package com.ccadmin.app.payment.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "mercado_pago_attempt")
public class MercadoPagoAttemptEntity extends AuditTableEntity {
    @Id public String AttemptId;
    public String SaleCod;
    public String PaymentMethodCod;
    public String RequestHash;
    public String CredentialHash;
    public BigDecimal Amount;
    public String CurrencyCod;
    public String TestMode;
    public String PaymentId;
    public String PaymentState;
    public String ProviderStatus;
    public String ProviderStatusDetail;
}
