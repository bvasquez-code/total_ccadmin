package com.ccadmin.app.sunat.identity.provider.dni;

import com.ccadmin.app.sunat.identity.model.dto.DniIdentityData;

import java.util.Optional;

public interface DniIdentityFallbackProvider {

    Optional<DniIdentityData> findByDni(String dni);
}
