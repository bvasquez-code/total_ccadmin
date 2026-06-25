package com.ccadmin.app.system.service;

import com.ccadmin.app.sale.model.entity.CreditNoteDocumentEntity;
import com.ccadmin.app.sale.model.entity.SaleDocumentEntity;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.system.model.dto.CounterfoilRegisterDto;
import com.ccadmin.app.system.model.dto.CounterfoilRegisterListDto;
import com.ccadmin.app.system.model.entity.CounterfoilEntity;
import com.ccadmin.app.system.model.entity.CounterfoilStoreEntity;
import com.ccadmin.app.system.repository.CounterfoilRepository;
import com.ccadmin.app.system.repository.CounterfoilStoreRepository;
import com.ccadmin.app.transfer.model.entity.TransferDocumentEntity;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CounterfoilCreateService extends SessionService {

    @Autowired
    private CounterfoilRepository counterfoilRepository;
    @Autowired
    private CounterfoilStoreRepository counterfoilStoreRepository;

    public SaleDocumentEntity generateDocumentSale(String StoreCod, String DocumentType,String SaleCod)
    {
        SaleDocumentEntity saleDocument = new SaleDocumentEntity();
        CounterfoilEntity counterfoil = this.counterfoilRepository.findByStoreDefault(DocumentType,StoreCod);

        counterfoil.Correlative = counterfoil.Correlative + 1;
        this.counterfoilRepository.save(counterfoil);

        int Correlative = 1000000 + counterfoil.Correlative;

        saleDocument.DocumentCod = counterfoil.Series+"-"+String.valueOf(Correlative).substring(1, 7);
        saleDocument.CounterfoilCod = counterfoil.CounterfoilCod;
        saleDocument.SaleCod = SaleCod;
        saleDocument.addSession(getUserCod());
        return saleDocument;
    }

    public CreditNoteDocumentEntity generateDocumentCreditNote(String StoreCod, String DocumentType, String CreditNoteCod, String GroupDocument)
    {
        CreditNoteDocumentEntity creditNoteDocument = new CreditNoteDocumentEntity();
        CounterfoilEntity counterfoil = this.counterfoilRepository.findByStoreDefault(DocumentType,StoreCod,GroupDocument);

        counterfoil.Correlative = counterfoil.Correlative + 1;
        this.counterfoilRepository.save(counterfoil);

        int Correlative = 1000000 + counterfoil.Correlative;

        creditNoteDocument.DocumentCod = counterfoil.Series+"-"+String.valueOf(Correlative).substring(1, 7);
        creditNoteDocument.CounterfoilCod = counterfoil.CounterfoilCod;
        creditNoteDocument.CreditNoteCod = CreditNoteCod;
        creditNoteDocument.addSession(getUserCod());
        return creditNoteDocument;
    }

    public TransferDocumentEntity generateDocumentTransfer(String StoreCod, String DocumentType, String TransferCod)
    {
        TransferDocumentEntity transferDocument = new TransferDocumentEntity();
        CounterfoilEntity counterfoil = this.counterfoilRepository.findByStoreDefault(DocumentType,StoreCod);

        if(counterfoil == null){
            throw new RuntimeException("No existe talonario automático para documento "+DocumentType+" y local "+StoreCod);
        }

        counterfoil.Correlative = counterfoil.Correlative + 1;
        this.counterfoilRepository.save(counterfoil);

        int Correlative = 1000000 + counterfoil.Correlative;

        transferDocument.DocumentCod = counterfoil.Series+"-"+String.valueOf(Correlative).substring(1, 7);
        transferDocument.CounterfoilCod = counterfoil.CounterfoilCod;
        transferDocument.TransferCod = TransferCod;
        transferDocument.addSession(getUserCod());
        return transferDocument;
    }

    @Transactional
    public CounterfoilRegisterDto save(CounterfoilRegisterDto request) {
        request.counterfoil.validate().session(this.getUserCod());
        request.counterfoilStore.validate().session(this.getUserCod());

        boolean repeatedSeries = this.counterfoilRepository.existBySeries(request.counterfoil.Series)>0;
        boolean existeCounterfoil = this.counterfoilRepository.existsById(request.counterfoil.CounterfoilCod);

        if(!existeCounterfoil && repeatedSeries){
            throw new RuntimeException("Serie ya existe : "+request.counterfoil.Series);
        }

        return new CounterfoilRegisterDto(
                counterfoilRepository.save(request.counterfoil),
                counterfoilStoreRepository.save(request.counterfoilStore)
        );
    }

    @Transactional
    public CounterfoilRegisterListDto saveAll(CounterfoilRegisterListDto request) {
        List<CounterfoilRegisterDto> registerList = new ArrayList<>();
        for (var item : request.registerList){
            registerList.add(this.save(item));
        }
        return new CounterfoilRegisterListDto(registerList);
    }

    public CounterfoilEntity enable(CounterfoilEntity request) {
        CounterfoilEntity e = counterfoilRepository.findById(request.CounterfoilCod)
                .orElseThrow(() -> new IllegalArgumentException("Counterfoil no encontrado"));
        e.active(this.getUserCod());
        return counterfoilRepository.save(e);
    }
    public CounterfoilEntity disable(CounterfoilEntity request) {
        CounterfoilEntity e = counterfoilRepository.findById(request.CounterfoilCod)
                .orElseThrow(() -> new IllegalArgumentException("Counterfoil no encontrado"));
        e.inactive(this.getUserCod());
        return counterfoilRepository.save(e);
    }

    // ------- Counterfoil-Store (attach/detach = CRUD relación) -------
    public CounterfoilStoreEntity attachStore(String counterfoilCod, String storeCod) {
        CounterfoilStoreEntity e = new CounterfoilStoreEntity();
        e.CounterfoilCod = counterfoilCod == null ? null : counterfoilCod.trim().toUpperCase();
        e.StoreCod = storeCod == null ? null : storeCod.trim().toUpperCase();
        e.validate().session(this.getUserCod());
        return counterfoilStoreRepository.save(e);
    }
    public int detachStore(String counterfoilCod, String storeCod) {
        return counterfoilStoreRepository.softDelete(
                counterfoilCod == null ? null : counterfoilCod.trim().toUpperCase(),
                storeCod == null ? null : storeCod.trim().toUpperCase()
        );
    }
}
