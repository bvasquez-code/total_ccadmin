package com.ccadmin.app.payment.service;

import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.repository.BusinessConfigRepository;
import org.springframework.stereotype.Service;

@Service
public class MercadoPagoConfigSearchService {
    private final BusinessConfigRepository businessConfigRepository;

    public MercadoPagoConfigSearchService(BusinessConfigRepository businessConfigRepository) {
        this.businessConfigRepository = businessConfigRepository;
    }

    public BusinessConfigEntity findCredentials() {
        BusinessConfigEntity config = businessConfigRepository.findByConfigCod(
                "MercadoPagoEcommerce", "MercadoPagoCheckout");
        if (config == null || !"A".equals(config.Status)
                || blank(config.ConfigVal) || blank(config.Str3Config)) {
            throw new IllegalArgumentException("El pago con tarjeta aun no esta disponible. Selecciona otro medio o contacta a la tienda.");
        }
        if (blank(config.Str2Config) || config.Num1Config == null
                || config.Num1Config < 1 || config.Num1Config > 48
                || !("S".equals(config.Sta2Config) || "N".equals(config.Sta2Config))) {
            throw new IllegalArgumentException("Revisa la moneda, las cuotas y el modo de prueba de Mercado Pago");
        }
        return config;
    }

    public BusinessConfigEntity findEnabled() {
        BusinessConfigEntity config = findCredentials();
        if (!"S".equals(config.Sta1Config)) {
            throw new IllegalArgumentException("El pago con Mercado Pago aun no esta habilitado");
        }
        return config;
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }
}
