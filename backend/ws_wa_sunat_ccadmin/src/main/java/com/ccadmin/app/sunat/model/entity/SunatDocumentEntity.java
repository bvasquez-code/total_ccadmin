package com.ccadmin.app.sunat.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import com.ccadmin.app.sunat.model.constants.SunatDocumentTypeConst;
import com.ccadmin.app.sunat.model.constants.SunatElectronicStatusConst;
import com.ccadmin.app.sunat.utility.SunatDocumentNameUtil;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "sunat_document")
public class SunatDocumentEntity extends AuditTableEntity implements Serializable {

    @Id
    public String SunatDocumentCod;
    public String SunatConfigCod;
    public String SourceModule;
    public String SourceDocumentCod;
    public String SourceDocumentType;
    public String SunatDocumentType;
    public String Series;
    public int Correlative;
    public String FullDocumentNumber;
    public String IssuerRuc;
    public Date IssueDate;
    public String CurrencyCod;
    public BigDecimal NumTotalPrice;
    public BigDecimal NumTotalTax;
    public String Environment;
    public String ElectronicStatus;
    public String TicketSunat;
    public String SunatResponseCode;
    public String SunatResponseDescription;
    public String SunatObservations;
    public String TechnicalResponse;
    public String LastTechnicalError;
    public String LastFunctionalError;
    public int SendAttemptCount;
    public int TicketAttemptCount;
    public Date AcceptedDate;
    public Date RejectedDate;
    public String OriginalSunatDocumentCod;
    public String RelatedDocumentNumber;
    public String RelatedDocumentType;

    public SunatDocumentEntity validate() {
        if (SunatDocumentCod == null || SunatDocumentCod.isBlank())
            throw new IllegalArgumentException("SunatDocumentCod requerido");
        if (SunatConfigCod == null || SunatConfigCod.isBlank())
            throw new IllegalArgumentException("SunatConfigCod requerido");
        if (SourceModule == null || SourceModule.isBlank())
            throw new IllegalArgumentException("Modulo origen requerido");
        if (SourceDocumentCod == null || SourceDocumentCod.isBlank())
            throw new IllegalArgumentException("Documento origen requerido");
        if (SunatDocumentType == null || !SunatDocumentTypeConst.isValid(SunatDocumentType))
            throw new IllegalArgumentException("Tipo de documento SUNAT invalido");
        if (Series == null || Series.isBlank())
            throw new IllegalArgumentException("Serie requerida");
        if (Correlative <= 0)
            throw new IllegalArgumentException("Correlativo debe ser mayor a cero");
        if (IssuerRuc == null || !IssuerRuc.matches("^\\d{11}$"))
            throw new IllegalArgumentException("RUC emisor debe tener 11 digitos");
        if (CurrencyCod == null || CurrencyCod.isBlank())
            throw new IllegalArgumentException("Moneda requerida");
        if (NumTotalPrice == null || NumTotalPrice.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Total de documento invalido");
        if (NumTotalTax == null || NumTotalTax.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Total de impuesto invalido");
        if (FullDocumentNumber == null || FullDocumentNumber.isBlank())
            FullDocumentNumber = SunatDocumentNameUtil.fullDocumentNumber(Series, Correlative);
        if (ElectronicStatus == null || ElectronicStatus.isBlank())
            ElectronicStatus = SunatElectronicStatusConst.PENDIENTE;
        return this;
    }

    public void ensureNotAccepted() {
        if (SunatElectronicStatusConst.isAccepted(this.ElectronicStatus)) {
            throw new IllegalArgumentException("Documento SUNAT ya aceptado, no puede reprocesarse");
        }
    }

    @Override
    public SunatDocumentEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
