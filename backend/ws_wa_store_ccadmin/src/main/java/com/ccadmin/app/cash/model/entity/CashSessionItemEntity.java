package com.ccadmin.app.cash.model.entity;

import com.ccadmin.app.shared.model.entity.AuditTableEntity;
import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "cash_session_item")
public class CashSessionItemEntity extends AuditTableEntity implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long ItemID;

    public Long CashSessionID;

    public Character ItemType; // D/P/M

    // Denominaciones (D)
    public BigDecimal Denomination;
    public Integer Qty;

    // Pago (P)
    public String PaymentMethodCod;

    // Movimiento manual (M)
    public String MovementType; // IN/OU
    public String ReferenceCod;

    public BigDecimal Amount = BigDecimal.ZERO;
    public String CurrencyCod;

    public String Commenter;

    public CashSessionItemEntity validate() {
        if (CashSessionID == null)
            throw new IllegalArgumentException("CashSessionID requerido");
        if (ItemType == null || "DPM".indexOf(ItemType) < 0)
            throw new IllegalArgumentException("ItemType inválido (D/P/M)");

        if (ItemType == 'D') {
            if (Denomination == null || Qty == null)
                throw new IllegalArgumentException("D requiere Denomination y Qty");
        }
        if (ItemType == 'P') {
            if (PaymentMethodCod == null || PaymentMethodCod.isBlank())
                throw new IllegalArgumentException("P requiere PaymentMethodCod");
        }
        if (ItemType == 'M') {
            if (MovementType == null || !(MovementType.equals("IN") || MovementType.equals("OU")))
                throw new IllegalArgumentException("M requiere MovementType IN/OU");
        }
        if (CurrencyCod == null || CurrencyCod.isBlank())
            throw new IllegalArgumentException("CurrencyCod requerido");

        return this;
    }

    @Override
    public CashSessionItemEntity session(String userCod) {
        this.addSession(userCod);
        return this;
    }
}
