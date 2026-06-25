package com.ccadmin.app.sale.model.idto;

import java.math.BigDecimal;

public interface IExpectedTotalsDto {
    BigDecimal getCash();
    BigDecimal getOther();
    BigDecimal getTotal();
}
