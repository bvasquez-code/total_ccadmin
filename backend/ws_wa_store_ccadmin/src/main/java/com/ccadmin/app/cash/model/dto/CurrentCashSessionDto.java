package com.ccadmin.app.cash.model.dto;

import com.ccadmin.app.cash.model.entity.CashRegisterEntity;
import com.ccadmin.app.cash.model.entity.CashSessionEntity;

public class CurrentCashSessionDto {

    public CashRegisterEntity CashRegister;
    public CashSessionEntity CashSession;
    public boolean IsOpen;

    public CurrentCashSessionDto(CashRegisterEntity cashRegister, CashSessionEntity cashSession) {
        this.CashRegister = cashRegister;
        this.CashSession = cashSession;
        this.IsOpen = cashSession != null;
    }
}
