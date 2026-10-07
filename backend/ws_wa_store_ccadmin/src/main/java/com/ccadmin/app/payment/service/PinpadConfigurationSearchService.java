package com.ccadmin.app.payment.service;

import com.ccadmin.app.cash.repository.CashSessionRepository;
import com.ccadmin.app.payment.config.PinpadBrowserProperties;
import com.ccadmin.app.shared.model.entity.BusinessConfigEntity;
import com.ccadmin.app.shared.service.BusinessConfigSearchService;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.Objects;

@Service
public class PinpadConfigurationSearchService {
    public static final String CONFIG_GROUP = "PinpadServiceUrl";
    public static final String REGISTER_CONFIG = "PinpadRegisterPaymentUrl";
    public static final String STATUS_CONFIG = "PinpadPaymentStatusUrl";
    public static final String ACK_CONFIG = "PinpadPaymentAckUrl";
    public static final String LOGIN_CONFIG = "PinpadLoginUrl";
    private static final java.util.Map<String, String> DEFAULT_LOCAL_ENDPOINTS = java.util.Map.of(
            REGISTER_CONFIG, "http://127.0.0.1:8094/pinpad/payment/register",
            STATUS_CONFIG, "http://127.0.0.1:8094/pinpad/payment/status",
            ACK_CONFIG, "http://127.0.0.1:8094/pinpad/payment/ack",
            LOGIN_CONFIG, "http://127.0.0.1:8094/pinpad/login");

    public record Connection(String agentId, String loginUrl, String registerUrl, String statusUrl,
                             String ackUrl, int waitSeconds, int pollMillis) {}
    private final BusinessConfigSearchService businessConfigSearchService;
    private final CashSessionRepository cashSessionRepository;
    private final PinpadBrowserProperties pinpadBrowserProperties;

    public PinpadConfigurationSearchService(BusinessConfigSearchService businessConfigSearchService,
            CashSessionRepository cashSessionRepository, PinpadBrowserProperties pinpadBrowserProperties) {
        this.businessConfigSearchService = businessConfigSearchService;
        this.cashSessionRepository = cashSessionRepository;
        this.pinpadBrowserProperties = pinpadBrowserProperties;
    }

    public Connection connection(String userCod, Long cashSessionId, String storeCod) {
        String registerCod = null;
        if (storeCod == null || storeCod.isBlank()) {
            throw new IllegalArgumentException("Seleccione una tienda para cobrar con pinpad");
        }
        if (cashSessionId != null) {
            var session = cashSessionRepository.findByCashSessionId(cashSessionId)
                .orElseThrow(() -> new IllegalArgumentException("La sesion de caja no existe"));
            if (!Objects.equals(userCod, session.UserCod) || !Objects.equals(storeCod, session.StoreCod)
                    || !"A".equals(session.Status)) {
                throw new IllegalArgumentException("La caja no pertenece al usuario y tienda o esta inactiva");
            }
            registerCod = session.RegisterCod;
        }
        String selectedRegister = registerCod;
        var agents = pinpadBrowserProperties.getAgents().entrySet().stream()
                .filter(entry -> Objects.equals(storeCod, entry.getValue().getStoreCod())
                        && (selectedRegister == null || Objects.equals(selectedRegister, entry.getValue().getRegisterCod()))).toList();
        if (agents.size() != 1) {
            throw new IllegalStateException(cashSessionId == null
                    ? "Configure un unico agente pinpad para la tienda en pinpad-cajas.yml cuando no utiliza sesiones de caja"
                    : "Debe configurar un agente pinpad para esta tienda y caja");
        }
        var agent = agents.get(0);
        if (!agent.getKey().matches("[A-Za-z0-9_-]{1,64}")
                || agent.getValue().getRegisterCod() == null || agent.getValue().getRegisterCod().isBlank()) {
            throw new IllegalStateException("Identificador del agente pinpad invalido");
        }
        BusinessConfigEntity register = configuration(REGISTER_CONFIG);
        String status = configuration(STATUS_CONFIG).ConfigVal;
        String ack = configuration(ACK_CONFIG).ConfigVal;
        String login = configuration(LOGIN_CONFIG).ConfigVal;
        URI origin = localEndpoint(register.ConfigVal);
        for (String address : new String[]{status, ack, login}) {
            URI uri = localEndpoint(address);
            if (!origin.getScheme().equals(uri.getScheme()) || !origin.getHost().equalsIgnoreCase(uri.getHost())
                    || origin.getPort() != uri.getPort()) throw new IllegalStateException("Las URLs deben apuntar al mismo agente local");
        }
        int wait = register.Num1Config == null ? 130 : register.Num1Config;
        int poll = register.Num2Config == null ? 1000 : register.Num2Config;
        if (wait < 0 || wait > 180 || poll < 100 || poll > 5000) throw new IllegalStateException("Tiempos pinpad invalidos");
        return new Connection(agent.getKey(), login, register.ConfigVal, status, ack, wait, poll);
    }

    private BusinessConfigEntity configuration(String code) {
        var config = businessConfigSearchService.findByConfigCod(CONFIG_GROUP, code);
        if (config == null) {
            config = new BusinessConfigEntity();
            config.ConfigVal = DEFAULT_LOCAL_ENDPOINTS.get(code);
        }
        if (!"A".equals(config.Status)) {
            throw new IllegalStateException("URL pinpad no configurada o inactiva: " + code);
        }
        if (config.ConfigVal == null || config.ConfigVal.isBlank()) {
            var defaults = new BusinessConfigEntity();
            defaults.ConfigVal = DEFAULT_LOCAL_ENDPOINTS.get(code);
            defaults.Num1Config = config.Num1Config;
            defaults.Num2Config = config.Num2Config;
            return defaults;
        }
        return config;
    }

    private URI localEndpoint(String address) {
        try {
            URI uri = URI.create(address);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || !("127.0.0.1".equals(uri.getHost()) || "localhost".equalsIgnoreCase(uri.getHost()))
                    || uri.getPort() == 6669 || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null) {
                throw new IllegalArgumentException();
            }
            return uri;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("La URL del agente debe ser de loopback y usar un puerto permitido, por ejemplo 8094");
        }
    }
}
