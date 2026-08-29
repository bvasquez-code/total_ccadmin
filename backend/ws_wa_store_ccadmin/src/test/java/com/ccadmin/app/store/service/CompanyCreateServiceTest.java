package com.ccadmin.app.store.service;

import com.ccadmin.app.store.model.entity.CompanyEntity;
import com.ccadmin.app.store.repository.CompanyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyCreateServiceTest {

    @Mock private CompanyRepository companyRepository;
    @InjectMocks private CompanyCreateService companyCreateService;

    @Test
    void createsTheCompanyWhenNoRegisterExists() {
        CompanyEntity company = validCompany("COMP");
        when(companyRepository.findOnlyCompany()).thenReturn(Optional.empty());
        when(companyRepository.save(company)).thenReturn(company);

        CompanyEntity result = companyCreateService.saveOnlyCompany(company);

        assertSame(company, result);
        assertEquals("SISTEMA", result.CreationUser);
    }

    @Test
    void updatesTheOnlyRegisteredCompany() {
        CompanyEntity company = validCompany("COMP");
        when(companyRepository.findOnlyCompany()).thenReturn(Optional.of(validCompany("COMP")));
        when(companyRepository.save(company)).thenReturn(company);

        CompanyEntity result = companyCreateService.saveOnlyCompany(company);

        assertSame(company, result);
        verify(companyRepository).save(company);
    }

    @Test
    void rejectsASecondCompanyCode() {
        CompanyEntity company = validCompany("OTRA");
        when(companyRepository.findOnlyCompany()).thenReturn(Optional.of(validCompany("COMP")));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> companyCreateService.saveOnlyCompany(company)
        );

        assertEquals(
                "Ya existe una compania registrada; solo puede administrar ese registro",
                exception.getMessage()
        );
        verify(companyRepository, never()).save(company);
    }

    private CompanyEntity validCompany(String companyCod) {
        CompanyEntity company = new CompanyEntity();
        company.CompanyCod = companyCod;
        company.TaxId = "20123456789";
        company.LegalName = "Compania de Prueba S.A.C.";
        company.FiscalAddress = "Av. Principal 123";
        company.UbigeoCod = "140101";
        company.CountryCode = "PE";
        return company;
    }
}
