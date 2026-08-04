package com.ccadmin.app.sunat.identity.provider;

import com.ccadmin.app.sunat.identity.model.constants.IdentityDocumentType;
import com.ccadmin.app.sunat.identity.model.dto.CompanyIdentityResponseDto;
import com.ccadmin.app.sunat.identity.model.dto.PersonIdentityResponseDto;

public interface IdentityQueryProvider {

    CompanyIdentityResponseDto findCompanyByRuc(String ruc);

    PersonIdentityResponseDto findPersonByDocument(
            IdentityDocumentType documentType,
            String documentNumber
    );
}
