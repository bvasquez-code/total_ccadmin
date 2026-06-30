package com.ccadmin.app.store.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;


@Entity
@Table(name = "company")
public class CompanyEntity extends AuditTableEntity implements Serializable {

    @Id
    public String CompanyCod;
    public String TaxId;
    public String LegalName;
    public String TradeName;
    public String FiscalAddress;
    public String Address;
    public String UbigeoCod;
    public String Department;
    public String Province;
    public String District;
    public String CountryCode = "PE";
    public String Phone;
    public String Email;
    public String Website;
    public String LogoPath;

    public CompanyEntity validate() {
        if (CompanyCod == null || CompanyCod.isBlank())
            throw new IllegalArgumentException("CompanyCod requerido");
        if (!CompanyCod.matches("^[A-Z0-9]{4}$"))
            throw new IllegalArgumentException("CompanyCod debe ser [A-Z0-9]{4}");

        if (TaxId == null || !TaxId.matches("^\\d{11}$"))
            throw new IllegalArgumentException("TaxId (RUC) debe tener 11 dígitos");

        if (LegalName == null || LegalName.isBlank())
            throw new IllegalArgumentException("LegalName requerido");

        if (FiscalAddress == null || FiscalAddress.isBlank())
            throw new IllegalArgumentException("FiscalAddress requerido");

        if (UbigeoCod == null || !UbigeoCod.matches("^\\d{6}$"))
            throw new IllegalArgumentException("UbigeoCod debe tener 6 dígitos");

        if (CountryCode != null && !CountryCode.matches("^[A-Z]{2}$"))
            throw new IllegalArgumentException("CountryCode debe ser ISO-3166-1 alfa-2");

        if (Department != null && Department.length() > 100)
            throw new IllegalArgumentException("Department supera 100 caracteres");

        if (Province != null && Province.length() > 100)
            throw new IllegalArgumentException("Province supera 100 caracteres");

        if (District != null && District.length() > 100)
            throw new IllegalArgumentException("District supera 100 caracteres");

        if (Email != null && !Email.isBlank()
                && !Email.matches("^[\\w.!#$%&’*+/=?`{|}~-]+@[\\w-]+(\\.[\\w-]+)+$"))
            throw new IllegalArgumentException("Email inválido");

        if (Website != null && Website.length() > 150)
            throw new IllegalArgumentException("Website supera 150 caracteres");

        if (LogoPath != null && LogoPath.length() > 500)
            throw new IllegalArgumentException("LogoPath supera 500 caracteres");

        return this;
    }

    @Override
    public CompanyEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
