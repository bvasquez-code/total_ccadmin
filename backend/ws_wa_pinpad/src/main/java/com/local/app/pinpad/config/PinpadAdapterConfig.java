package com.local.app.pinpad.config;

import com.local.app.pinpad.adapter.PinpadAdapter;
import com.local.app.pinpad.adapter.PinpadCulqiAdapter;
import com.local.app.pinpad.adapter.PinpadNiubizAdapter;
import com.local.app.pinpad.adapter.PinpadSimulatorAdapter;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.niubiz.NiubizTerminalClient;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration(proxyBeanMethods = false)
public class PinpadAdapterConfig {

    @Bean
    public PinpadAdapter pinpadAdapter(PinpadAgentProperties pinpadAgentProperties,
                                       ObjectProvider<CulqiTerminalClient> culqiTerminalClients,
                                       ObjectProvider<NiubizTerminalClient> niubizTerminalClients) {
        if (pinpadAgentProperties.getTimeoutSeconds() <= 0
                || pinpadAgentProperties.getRegisterWaitTimeoutSeconds() <= 0) {
            throw new IllegalStateException("Los tiempos de espera del pinpad deben ser mayores a cero");
        }
        return switch (pinpadAgentProperties.getProvider()) {
            case DEMO -> new PinpadSimulatorAdapter(pinpadAgentProperties);
            case CULQI -> {
                validateTerminalConfiguration(pinpadAgentProperties, "culqi",
                        pinpadAgentProperties.getCulqiSupportedPaymentMethods());
                CulqiTerminalClient culqiTerminalClient = culqiTerminalClients.getIfAvailable();
                if (culqiTerminalClient == null) {
                    throw new IllegalStateException("Modo culqi sin conector: implementar CulqiTerminalClient "
                            + "con el SDK/protocolo oficial del terminal entregado por Culqi. "
                            + "Ver README.md; usar pinpad.provider=demo para simulacion.");
                }
                yield new PinpadCulqiAdapter(pinpadAgentProperties, culqiTerminalClient);
            }
            case NIUBIZ -> {
                validateTerminalConfiguration(pinpadAgentProperties, "niubiz",
                        pinpadAgentProperties.getNiubizSupportedPaymentMethods());
                NiubizTerminalClient niubizTerminalClient = niubizTerminalClients.getIfAvailable();
                if (niubizTerminalClient == null) {
                    throw new IllegalStateException("Modo niubiz sin conector: implementar NiubizTerminalClient "
                            + "con el SDK/protocolo presencial oficial del terminal entregado por Niubiz. "
                            + "Ver README.md; usar pinpad.provider=demo para simulacion.");
                }
                yield new PinpadNiubizAdapter(pinpadAgentProperties, niubizTerminalClient);
            }
        };
    }

    private void validateTerminalConfiguration(PinpadAgentProperties properties, String provider,
                                               List<PinpadPaymentMethod> supportedPaymentMethods) {
        if (isDemoIdentifier(properties.getTerminalId()) || isDemoIdentifier(properties.getMerchantId())) {
            throw new IllegalStateException("Modo " + provider + " requiere pinpad.terminal-id y pinpad.merchant-id reales");
        }
        if (properties.getAgentToken() == null || properties.getAgentToken().isBlank()
                || "change-me-local-token".equals(properties.getAgentToken())) {
            throw new IllegalStateException("Modo " + provider + " requiere un pinpad.agent-token propio");
        }
        if (supportedPaymentMethods == null || supportedPaymentMethods.isEmpty()
                || supportedPaymentMethods.contains(null)) {
            throw new IllegalStateException("Configurar pinpad." + provider + "-supported-payment-methods");
        }
    }

    private boolean isDemoIdentifier(String value) {
        return value == null || value.isBlank() || value.toUpperCase(java.util.Locale.ROOT).contains("DEMO");
    }
}
