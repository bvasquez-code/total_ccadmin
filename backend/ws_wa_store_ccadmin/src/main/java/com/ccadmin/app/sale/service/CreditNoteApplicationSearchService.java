package com.ccadmin.app.sale.service;

import com.ccadmin.app.sale.model.entity.CreditNoteApplicationEntity;
import com.ccadmin.app.sale.model.entity.CreditNoteHeadEntity;
import com.ccadmin.app.sale.repository.CreditNoteApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class CreditNoteApplicationSearchService {

    @Autowired
    private CreditNoteApplicationRepository creditNoteApplicationRepository;

    public List<CreditNoteApplicationEntity> findActiveByCreditNoteCod(String creditNoteCod) {
        return this.creditNoteApplicationRepository.findActiveByCreditNoteCod(creditNoteCod);
    }

    public List<CreditNoteApplicationEntity> findActiveBySaleCod(String saleCod) {
        return this.creditNoteApplicationRepository.findActiveBySaleCod(saleCod);
    }

    public BigDecimal findTotalApplied(String creditNoteCod) {
        return amount(this.creditNoteApplicationRepository.findTotalApplied(creditNoteCod));
    }

    public BigDecimal findAvailableBalance(CreditNoteHeadEntity creditNoteHead) {
        if (creditNoteHead == null || creditNoteHead.CreditNoteCod == null) {
            return amount(BigDecimal.ZERO);
        }
        BigDecimal availableBalance = amount(creditNoteHead.NumTotalPrice)
                .subtract(findTotalApplied(creditNoteHead.CreditNoteCod));
        return availableBalance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }
}
