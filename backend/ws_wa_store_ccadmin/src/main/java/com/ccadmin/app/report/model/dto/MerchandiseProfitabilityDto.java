package com.ccadmin.app.report.model.dto;

import com.ccadmin.app.report.model.idto.IMerchandiseProfitabilityDto;
import java.math.BigDecimal;

/** Estimación comercial de los movimientos trazados, separada del saldo de inventario. */
public class MerchandiseProfitabilityDto {
    public String CurrencyCod;
    public BigDecimal NetSales;
    public BigDecimal CostOfGoodsSold;
    public BigDecimal StockLosses;
    public BigDecimal SoldQuantity;
    public BigDecimal LostQuantity;
    public BigDecimal EstimatedResult;
    public long MissingCostRows;

    public MerchandiseProfitabilityDto(IMerchandiseProfitabilityDto row) {
        CurrencyCod = row.getCurrencyCod();
        NetSales = row.getNetSales();
        MissingCostRows = row.getMissingCostRows();
        CostOfGoodsSold = MissingCostRows == 0 ? row.getCostOfGoodsSold() : null;
        StockLosses = MissingCostRows == 0 ? row.getStockLosses() : null;
        SoldQuantity = row.getSoldQuantity();
        LostQuantity = row.getLostQuantity();
        EstimatedResult = MissingCostRows == 0 ? NetSales.subtract(CostOfGoodsSold).subtract(StockLosses) : null;
    }
}
