package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;
import com.ccadmin.app.shared.model.myconst.StatusConst;

public final class PucharseHeadEntityFactory {

    private PucharseHeadEntityFactory() {
    }

    public static PucharseHeadEntity fromRequest(
            PucharseRequestHeadEntity request,
            String pucharseCod,
            String pucharseReqCod
    ) {
        PucharseHeadEntity head = new PucharseHeadEntity();
        head.PucharseCod = pucharseCod;
        head.StoreCod = request.StoreCod;
        head.PucharseReqCod = pucharseReqCod;
        head.ExternalCod = request.ExternalCod;
        head.DealerCod = request.DealerCod;
        head.Commenter = request.Commenter;
        head.PurchaseStatus = StatusConst.PENDING;
        head.CurrencyCod = request.CurrencyCod;
        head.CurrencyCodSys = request.CurrencyCodSys;
        head.NumExchangevalue = request.NumExchangevalue;
        head.NumTotalPrice = request.NumTotalPrice;
        return head;
    }
}
