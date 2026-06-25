package com.ccadmin.app.system.shared;

import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.system.service.CounterfoilCreateService;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CounterfoilShared {

    @Autowired
    private CounterfoilCreateService counterfoilCreateService;

    public SaleDocumentEntity generateDocumentSale(String StoreCod, String DocumentType,String SaleCod)
    {
        return this.counterfoilCreateService.generateDocumentSale(StoreCod,DocumentType,SaleCod);
    }

    public CreditNoteDocumentEntity generateDocumentCreditNote(String StoreCod, String DocumentType, String CreditNoteCod, String GroupDocument)
    {
        return this.counterfoilCreateService.generateDocumentCreditNote(StoreCod,DocumentType,CreditNoteCod,GroupDocument);
    }

    public TransferDocumentEntity generateDocumentTransfer(String StoreCod, String DocumentType, String TransferCod)
    {
        return this.counterfoilCreateService.generateDocumentTransfer(StoreCod,DocumentType,TransferCod);
    }


}
