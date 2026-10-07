package com.local.app.pinpad.config;

import com.local.app.pinpad.adapter.PinpadAdapter;
import com.local.app.pinpad.adapter.PinpadCulqiAdapter;
import com.local.app.pinpad.adapter.PinpadNiubizAdapter;
import com.local.app.pinpad.adapter.PinpadSimulatorAdapter;
import com.local.app.pinpad.adapter.culqi.CulqiTerminalClient;
import com.local.app.pinpad.adapter.niubiz.NiubizTerminalClient;
import com.local.app.pinpad.enums.PinpadPaymentMethod;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class PinpadAdapterConfigTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class, PinpadAdapterConfig.class);

    @Test
    void defaultsToDemo() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PinpadAdapter.class);
            assertThat(context.getBean(PinpadAdapter.class)).isInstanceOf(PinpadSimulatorAdapter.class);
        });
    }

    @Test
    void culqiDoesNotFallbackToSimulatorWithoutClient() {
        culqiContext().run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseMessage("Modo culqi sin conector: implementar CulqiTerminalClient "
                        + "con el SDK/protocolo oficial del terminal entregado por Culqi. "
                        + "Ver README.md; usar pinpad.provider=demo para simulacion."));
    }

    @Test
    void selectsOnlyCulqiWhenClientIsInstalled() {
        culqiContext().withBean(CulqiTerminalClient.class, () -> mock(CulqiTerminalClient.class)).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(PinpadAdapter.class);
            assertThat(context.getBean(PinpadAdapter.class)).isInstanceOf(PinpadCulqiAdapter.class);
            assertThat(context.getBean(PinpadAgentProperties.class).isSimulatorEnabled()).isFalse();
        });
    }

    @Test
    void rejectsUnknownProvider() {
        contextRunner.withPropertyValues("pinpad.provider=other").run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsDemoIdentityInCulqiMode() {
        contextRunner.withPropertyValues("pinpad.provider=culqi").run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseMessage("Modo culqi requiere pinpad.terminal-id y pinpad.merchant-id reales"));
    }

    @Test
    void explicitProviderTakesPrecedenceOverLegacyFlag() {
        contextRunner.withPropertyValues("pinpad.provider=demo", "pinpad.simulator-enabled=false")
                .run(context -> assertThat(context.getBean(PinpadAdapter.class)).isInstanceOf(PinpadSimulatorAdapter.class));
    }

    @Test
    void disabledLegacySimulatorCannotAccidentallySimulate() {
        contextRunner.withPropertyValues("pinpad.simulator-enabled=false")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rejectsDefaultAgentTokenForRealPayments() {
        culqiContext().withPropertyValues("pinpad.agent-token=change-me-local-token").run(context ->
                assertThat(context).hasFailed().getFailure()
                        .hasRootCauseMessage("Modo culqi requiere un pinpad.agent-token propio"));
    }

    @Test
    void niubizDoesNotFallbackToAnotherProviderWithoutClient() {
        niubizContext().withBean(CulqiTerminalClient.class, () -> mock(CulqiTerminalClient.class)).run(context ->
                assertThat(context).hasFailed().getFailure().hasRootCauseMessage(
                        "Modo niubiz sin conector: implementar NiubizTerminalClient "
                                + "con el SDK/protocolo presencial oficial del terminal entregado por Niubiz. "
                                + "Ver README.md; usar pinpad.provider=demo para simulacion."));
    }

    @Test
    void selectsOnlyNiubizWhenBothClientsAreInstalled() {
        niubizContext().withBean(CulqiTerminalClient.class, () -> mock(CulqiTerminalClient.class))
                .withBean(NiubizTerminalClient.class, () -> mock(NiubizTerminalClient.class)).run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(PinpadAdapter.class);
                    assertThat(context.getBean(PinpadAdapter.class)).isInstanceOf(PinpadNiubizAdapter.class);
                    PinpadAgentProperties properties = context.getBean(PinpadAgentProperties.class);
                    assertThat(properties.isSimulatorEnabled()).isFalse();
                    assertThat(properties.getNiubizSupportedPaymentMethods()).containsExactly(PinpadPaymentMethod.CARD);
                });
    }

    @Test
    void niubizRequiresRealIdentityAndPrivateAgentToken() {
        contextRunner.withPropertyValues("pinpad.provider=niubiz").run(context -> assertThat(context).hasFailed()
                .getFailure().hasRootCauseMessage("Modo niubiz requiere pinpad.terminal-id y pinpad.merchant-id reales"));
        niubizContext().withPropertyValues("pinpad.agent-token=change-me-local-token").run(context ->
                assertThat(context).hasFailed().getFailure()
                        .hasRootCauseMessage("Modo niubiz requiere un pinpad.agent-token propio"));
    }

    @Test
    void niubizHasItsOwnPaymentMethodConfiguration() {
        niubizContext().withPropertyValues("pinpad.niubiz-supported-payment-methods=CARD,QR")
                .withBean(NiubizTerminalClient.class, () -> mock(NiubizTerminalClient.class)).run(context -> {
                    assertThat(context).hasNotFailed();
                    PinpadAgentProperties properties = context.getBean(PinpadAgentProperties.class);
                    assertThat(properties.getNiubizSupportedPaymentMethods())
                            .containsExactly(PinpadPaymentMethod.CARD, PinpadPaymentMethod.QR);
                    assertThat(properties.getCulqiSupportedPaymentMethods()).containsExactly(PinpadPaymentMethod.CARD);
                });
    }

    @Test
    void rejectsNiubizWithoutEnabledPaymentMethods() {
        niubizContext().withPropertyValues("pinpad.niubiz-supported-payment-methods=").run(context ->
                assertThat(context).hasFailed().getFailure()
                        .hasRootCauseMessage("Configurar pinpad.niubiz-supported-payment-methods"));
    }

    private ApplicationContextRunner niubizContext() {
        return contextRunner.withPropertyValues("pinpad.provider=niubiz", "pinpad.terminal-id=TERM-001",
                "pinpad.merchant-id=MERCHANT-001", "pinpad.agent-token=test-agent-token");
    }

    private ApplicationContextRunner culqiContext() {
        return contextRunner.withPropertyValues("pinpad.provider=culqi", "pinpad.terminal-id=TERM-001",
                "pinpad.merchant-id=MERCHANT-001", "pinpad.agent-token=test-agent-token");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PinpadAgentProperties.class)
    static class PropertiesConfiguration { }
}
