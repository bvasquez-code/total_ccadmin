package com.ccadmin.app.system.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.io.Serializable;

@Entity
@Table( name = "payment_method")
public class PaymentMethodEntity extends AuditTableEntity implements Serializable {

    @Id
    public String PaymentMethodCod;
    public String Name;
    public String Description;
    public String PaymentMethodType;
    public String IsInternalSaleEnabled = "S";
    public String IsWebSaleEnabled = "S";
    public String IsPaymentProofRequired = "N";
    public String FileCod;
    public String Route;

    public PaymentMethodEntity validate() {
        if (PaymentMethodCod == null || PaymentMethodCod.isBlank()) {
            throw new IllegalArgumentException("PaymentMethodCod requerido");
        }
        IsInternalSaleEnabled = normalizeIndicator(IsInternalSaleEnabled, "S");
        IsWebSaleEnabled = normalizeIndicator(IsWebSaleEnabled, "S");
        IsPaymentProofRequired = normalizeIndicator(IsPaymentProofRequired, "N");
        if ("N".equals(IsInternalSaleEnabled) && "N".equals(IsWebSaleEnabled)) {
            throw new IllegalArgumentException(
                    "El metodo de pago debe estar disponible en venta interna o tienda virtual"
            );
        }
        if ("S".equals(IsPaymentProofRequired) && !"S".equals(IsWebSaleEnabled)) {
            throw new IllegalArgumentException(
                    "El comprobante de pago solo puede solicitarse para un medio habilitado en tienda virtual"
            );
        }
        return this;
    }

    private String normalizeIndicator(String value, String defaultValue) {
        String indicator = value == null || value.isBlank()
                ? defaultValue
                : value.trim().toUpperCase();
        if (!"S".equals(indicator) && !"N".equals(indicator)) {
            throw new IllegalArgumentException("Los indicadores del metodo de pago solo admiten S o N");
        }
        return indicator;
    }

    @Override
    public PaymentMethodEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }

}
