package com.local.app.pinpad.service;

import com.local.app.pinpad.config.PinpadAgentProperties;
import com.local.app.pinpad.enums.PinpadProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PinpadInstallationCreateServiceTest {
    @TempDir Path installationDirectory;
    final PinpadInstallationCreateService service = new PinpadInstallationCreateService();

    @Test
    void createsLoadableSingleFileWithMatchingKeyAndDemoDefaults() throws Exception {
        Path file = service.prepare(installationDirectory);
        assertThat(Files.readString(file)).doesNotContain("__SIGNING_KEY__", "__AGENT_TOKEN__");
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.location=" + file.toUri())
                .withUserConfiguration(PropertiesConfiguration.class)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var properties = context.getBean(PinpadAgentProperties.class);
                    assertThat(properties.getAgentId()).isEqualTo("CAJA01");
                    assertThat(properties.getProvider()).isEqualTo(PinpadProvider.DEMO);
                    assertThat(properties.getBackendAuthorizationUrl()).isEqualTo("http://127.0.0.1:8090/api/v1/TrxPayment/authorizePinpad");
                    assertThat(context.getEnvironment().getProperty("pinpad.browser.agents.CAJA01.signing-key")).isNull();
                    assertThat(properties.getAgentToken()).hasSizeGreaterThanOrEqualTo(32);
                    assertThat(context.getEnvironment().getProperty("server.port")).isEqualTo("8094");
                    assertThat(context.getEnvironment().getProperty("pinpad.browser.agents.CAJA01.store-cod")).isEqualTo("T001");
                });
    }

    @Test
    void preservesEditedConfigurationAcrossRestartsAndUsesDifferentKeysPerInstallation() throws Exception {
        Path first = service.prepare(installationDirectory.resolve("first"));
        String original = Files.readString(first);
        String edited = original.replace("CAJA01", "CAJA02").replace("CAJA0001", "CAJA0002");
        Files.writeString(first, edited);
        assertThat(service.prepare(first.getParent())).isEqualTo(first);
        assertThat(Files.readString(first)).isEqualTo(edited);
        Path second = service.prepare(installationDirectory.resolve("second"));
        assertThat(Files.readString(second)).isNotEqualTo(original);
    }

    @Test
    void preservesConfigurationInConfigDirectoryWithoutCreatingAnotherFile() throws Exception {
        Path existing = installationDirectory.resolve("config/pinpad-cajas.yml");
        Files.createDirectories(existing.getParent());
        Files.writeString(existing, "pinpad:\n  agent-id: EXISTING\n");
        assertThat(service.prepare(installationDirectory)).isEqualTo(existing);
        assertThat(installationDirectory.resolve("pinpad-cajas.yml")).doesNotExist();
        assertThat(Files.readString(existing)).contains("EXISTING");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PinpadAgentProperties.class)
    static class PropertiesConfiguration {}
}
