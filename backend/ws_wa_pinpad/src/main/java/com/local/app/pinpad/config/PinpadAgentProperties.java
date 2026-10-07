package com.local.app.pinpad.config;

import com.local.app.pinpad.enums.PinpadProvider;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "pinpad")
public class PinpadAgentProperties {

    private String agentToken = "change-me-local-token";
    private String agentId;
    private String backendAuthorizationUrl = "http://127.0.0.1:8090/api/v1/TrxPayment/authorizePinpad";
    private String storagePath = "C:/pinpad-agent";
    private long timeoutSeconds = 120;
    private boolean simulatorEnabled = true;
    private PinpadProvider provider;
    private List<PinpadPaymentMethod> culqiSupportedPaymentMethods = new ArrayList<>(List.of(PinpadPaymentMethod.CARD));
    private List<PinpadPaymentMethod> niubizSupportedPaymentMethods = new ArrayList<>(List.of(PinpadPaymentMethod.CARD));
    private boolean waitFinalResultOnRegister = true;
    private long registerWaitTimeoutSeconds = 5;
    private long simulatorMinDelayMillis = 1000;
    private long simulatorMaxDelayMillis = 3000;
    private String terminalId = "TERM-DEMO-001";
    private String merchantId = "MERCHANT-DEMO-001";
    private String allowedCurrency = "PEN";
    private List<String> allowedOrigins = new ArrayList<>();

    public String getAgentToken() { return agentToken; }
    public String getBackendAuthorizationUrl() { return backendAuthorizationUrl; }
    public void setBackendAuthorizationUrl(String backendAuthorizationUrl) { this.backendAuthorizationUrl = backendAuthorizationUrl; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public void setAgentToken(String agentToken) { this.agentToken = agentToken; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    // Compatibilidad con configuraciones anteriores que solo tenian simulator-enabled.
    public PinpadProvider getProvider() { return provider != null ? provider : (simulatorEnabled ? PinpadProvider.DEMO : PinpadProvider.CULQI); }
    public void setProvider(PinpadProvider provider) { this.provider = provider; }
    public List<PinpadPaymentMethod> getCulqiSupportedPaymentMethods() { return culqiSupportedPaymentMethods; }
    public void setCulqiSupportedPaymentMethods(List<PinpadPaymentMethod> methods) { this.culqiSupportedPaymentMethods = methods; }
    public List<PinpadPaymentMethod> getNiubizSupportedPaymentMethods() { return niubizSupportedPaymentMethods; }
    public void setNiubizSupportedPaymentMethods(List<PinpadPaymentMethod> methods) { this.niubizSupportedPaymentMethods = methods; }
    public boolean isSimulatorEnabled() { return getProvider() == PinpadProvider.DEMO; }
    public void setSimulatorEnabled(boolean simulatorEnabled) { this.simulatorEnabled = simulatorEnabled; }
    public boolean isWaitFinalResultOnRegister() { return waitFinalResultOnRegister; }
    public void setWaitFinalResultOnRegister(boolean waitFinalResultOnRegister) { this.waitFinalResultOnRegister = waitFinalResultOnRegister; }
    public long getRegisterWaitTimeoutSeconds() { return registerWaitTimeoutSeconds; }
    public void setRegisterWaitTimeoutSeconds(long registerWaitTimeoutSeconds) { this.registerWaitTimeoutSeconds = registerWaitTimeoutSeconds; }
    public long getSimulatorMinDelayMillis() { return simulatorMinDelayMillis; }
    public void setSimulatorMinDelayMillis(long simulatorMinDelayMillis) { this.simulatorMinDelayMillis = simulatorMinDelayMillis; }
    public long getSimulatorMaxDelayMillis() { return simulatorMaxDelayMillis; }
    public void setSimulatorMaxDelayMillis(long simulatorMaxDelayMillis) { this.simulatorMaxDelayMillis = simulatorMaxDelayMillis; }
    public String getTerminalId() { return terminalId; }
    public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    public String getMerchantId() { return merchantId; }
    public void setMerchantId(String merchantId) { this.merchantId = merchantId; }
    public String getAllowedCurrency() { return allowedCurrency; }
    public void setAllowedCurrency(String allowedCurrency) { this.allowedCurrency = allowedCurrency; }
    public List<String> getAllowedOrigins() { return allowedOrigins; }
    public void setAllowedOrigins(List<String> allowedOrigins) { this.allowedOrigins = allowedOrigins; }
}
