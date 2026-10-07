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
import java.util.Locale;

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

        if(counterfoil == null){
            throw new RuntimeException("No existe talonario automatico para documento " + DocumentType + " y local " + StoreCod);
        }

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
        CounterfoilEntity counterfoilToReplace = findCounterfoilToReplace(request);
        if (counterfoilToReplace != null) {
            request.counterfoil = createReplacementCounterfoil(request.counterfoil);
        }
        request.counterfoil.validate().session(this.getUserCod());
        request.counterfoilStore.validate().session(this.getUserCod());

        boolean repeatedDocumentTypeAndSeries = this.counterfoilRepository.findByDocTypeSeries(
                request.counterfoil.DocumentType,
                request.counterfoil.Series
        ).isPresent();
        boolean existsCounterfoil = this.counterfoilRepository.existsById(request.counterfoil.CounterfoilCod);

        if (counterfoilToReplace != null && existsCounterfoil) {
            throw new IllegalArgumentException("La nueva serie ya tiene un talonario; no se puede reemplazar");
        }

        if (!existsCounterfoil && repeatedDocumentTypeAndSeries) {
            throw new RuntimeException(
                    "Ya existe un talonario para el tipo de documento "
                            + request.counterfoil.DocumentType
                            + " y la serie "
                            + request.counterfoil.Series
            );
        }

        CounterfoilRegisterDto saved = new CounterfoilRegisterDto(
                counterfoilRepository.save(request.counterfoil),
                counterfoilStoreRepository.save(request.counterfoilStore)
        );
        if (counterfoilToReplace != null) {
            deactivateCounterfoil(counterfoilToReplace);
        }
        return saved;
    }

    private CounterfoilEntity findCounterfoilToReplace(CounterfoilRegisterDto request) {
        if (request.PreviousCounterfoilCod == null || request.PreviousCounterfoilCod.isBlank()) {
            return null;
        }
        String previousCounterfoilCod = request.PreviousCounterfoilCod.trim().toUpperCase(Locale.ROOT);
        if (previousCounterfoilCod.equals(request.counterfoil.CounterfoilCod)) {
            return null;
        }
        CounterfoilEntity previousCounterfoil = counterfoilRepository.findByIdForUpdate(previousCounterfoilCod)
                .orElseThrow(() -> new IllegalArgumentException("El talonario anterior no existe"));
        if (!"A".equals(previousCounterfoil.Status)) {
            throw new IllegalArgumentException("El talonario anterior ya no está activo; vuelva a cargar la lista");
        }
        if (!previousCounterfoil.DocumentType.equals(request.counterfoil.DocumentType)
                || !request.counterfoil.CounterfoilCod.equals(
                        request.counterfoil.DocumentType + request.counterfoil.Series)
                || !request.counterfoilStore.CounterfoilCod.equals(request.counterfoil.CounterfoilCod)) {
            throw new IllegalArgumentException("El cambio de serie debe conservar el tipo de documento y sus códigos asociados");
        }
        List<CounterfoilStoreEntity> storesAssigned = counterfoilStoreRepository
                .findStoresByCounterfoil(previousCounterfoilCod);
        if (storesAssigned.size() != 1
                || !storesAssigned.getFirst().StoreCod.equals(request.counterfoilStore.StoreCod)) {
            throw new IllegalArgumentException("El talonario debe estar asignado únicamente a la tienda que se está configurando");
        }
        return previousCounterfoil;
    }

    private CounterfoilEntity createReplacementCounterfoil(CounterfoilEntity source) {
        CounterfoilEntity replacement = new CounterfoilEntity();
        replacement.CounterfoilCod = source.CounterfoilCod;
        replacement.DocumentType = source.DocumentType;
        replacement.Series = source.Series;
        replacement.Correlative = source.Correlative;
        replacement.IsAutomatic = source.IsAutomatic;
        replacement.GroupDocument = source.GroupDocument;
        replacement.Status = source.Status;
        return replacement;
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
        return deactivateCounterfoil(e);
    }

    private CounterfoilEntity deactivateCounterfoil(CounterfoilEntity counterfoil) {
        counterfoil.inactive(this.getUserCod());
        return counterfoilRepository.save(counterfoil);
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
