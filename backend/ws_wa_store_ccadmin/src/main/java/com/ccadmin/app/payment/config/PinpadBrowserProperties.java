package com.ccadmin.app.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "pinpad.browser")
public class PinpadBrowserProperties {
    private Map<String, Agent> agents = new LinkedHashMap<>();
    public PinpadBrowserProperties() {
        Agent initial = new Agent(); initial.setStoreCod("T001"); initial.setRegisterCod("CAJA0001");
        agents.put("CAJA01", initial);
    }
    public Map<String, Agent> getAgents() { return agents; }
    public void setAgents(Map<String, Agent> agents) { this.agents = agents; }

    public static class Agent {
        private String registerCod;
        private String storeCod;
        public String getRegisterCod() { return registerCod; }
        public void setRegisterCod(String registerCod) { this.registerCod = registerCod; }
        public String getStoreCod() { return storeCod; }
        public void setStoreCod(String storeCod) { this.storeCod = storeCod; }
    }
}
