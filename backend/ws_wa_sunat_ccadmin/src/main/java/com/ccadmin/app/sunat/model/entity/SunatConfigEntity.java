package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.sunat.model.constants.SunatEnvironmentConst;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table(name = "sunat_config")
public class SunatConfigEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SunatConfigCod;
    public String IssuerRuc;
    public String SolUser;
    public String SolPassword;
    public String CertificatePath;
    public String CertificatePassword;
    public String CertificateType;
    public String Environment;
    public String StorageBasePath;
    public String InvoiceEndpoint;
    public String SummaryEndpoint;
    public String TicketEndpoint;
    public int MaxSendAttempts;
    public int MaxTicketAttempts;
    public String SchedulerEnabled;
    public String AutomaticRetryEnabled;
    public String ActiveConfig;

    public SunatConfigEntity validate() {
        if (SunatConfigCod == null || SunatConfigCod.isBlank())
            throw new IllegalArgumentException("SunatConfigCod requerido");
        if (IssuerRuc == null || !IssuerRuc.matches("^\\d{11}$"))
            throw new IllegalArgumentException("RUC emisor debe tener 11 digitos");
        if (SolUser == null || SolUser.isBlank())
            throw new IllegalArgumentException("Usuario SOL requerido");
        if (SolPassword == null || SolPassword.isBlank())
            throw new IllegalArgumentException("Clave SOL requerida");
        if (CertificatePath == null || CertificatePath.isBlank())
            throw new IllegalArgumentException("Ruta de certificado requerida");
        if (CertificatePassword == null || CertificatePassword.isBlank())
            throw new IllegalArgumentException("Clave de certificado requerida");
        if (Environment == null || !SunatEnvironmentConst.isValid(Environment))
            throw new IllegalArgumentException("Ambiente SUNAT invalido");
        if (StorageBasePath == null || StorageBasePath.isBlank())
            throw new IllegalArgumentException("Ruta base de almacenamiento requerida");
        if (InvoiceEndpoint == null || InvoiceEndpoint.isBlank())
            throw new IllegalArgumentException("Endpoint de comprobantes requerido");
        if (SummaryEndpoint == null || SummaryEndpoint.isBlank())
            throw new IllegalArgumentException("Endpoint de resumen/baja requerido");
        if (TicketEndpoint == null || TicketEndpoint.isBlank())
            throw new IllegalArgumentException("Endpoint de consulta ticket requerido");
        if (MaxSendAttempts <= 0)
            throw new IllegalArgumentException("MaxSendAttempts debe ser mayor a cero");
        if (MaxTicketAttempts <= 0)
            throw new IllegalArgumentException("MaxTicketAttempts debe ser mayor a cero");
        normalizeFlags();
        return this;
    }

    public void normalizeFlags() {
        if (CertificateType == null || CertificateType.isBlank()) CertificateType = "P12";
        if (SchedulerEnabled == null || SchedulerEnabled.isBlank()) SchedulerEnabled = "N";
        if (AutomaticRetryEnabled == null || AutomaticRetryEnabled.isBlank()) AutomaticRetryEnabled = "N";
        if (ActiveConfig == null || ActiveConfig.isBlank()) ActiveConfig = "N";
    }

    public void activate(String userCod) {
        this.ActiveConfig = "S";
        this.active(userCod);
    }

    public void deactivate(String userCod) {
        this.ActiveConfig = "N";
        this.inactive(userCod);
    }

    @Override
    public SunatConfigEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
