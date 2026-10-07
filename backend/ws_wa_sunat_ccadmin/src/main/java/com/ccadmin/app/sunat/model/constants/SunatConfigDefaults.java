package com.ccadmin.app.sunat.model.constants;

import com.ccadmin.app.sunat.model.entity.SunatConfigEntity;

public final class SunatConfigDefaults {
    public static final String BETA_CODE = "SUNAT_TEST_BETA";
    public static final String PRODUCTION_CODE = "SUNAT_PRODUCCION";
    public static final String BETA_ENDPOINT = "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";
    public static final String PRODUCTION_ENDPOINT = "https://e-factura.sunat.gob.pe/ol-ti-itcpfegem/billService";
    public static final String GUIDE_ENDPOINT = "https://api-cpe.sunat.gob.pe/v1/contribuyente/gem";
    public static final String GUIDE_TOKEN_ENDPOINT = "https://api-seguridad.sunat.gob.pe/v1/clientessol/{client_id}/oauth2/token/";
    public static final String TEST_CERTIFICATE = "classpath:certificate/certificado_prueba.pfx";

    private SunatConfigDefaults() {}

    public static SunatConfigEntity create(String environment) {
        SunatConfigEntity config = new SunatConfigEntity();
        boolean beta = "BETA".equals(environment);
        config.SunatConfigCod = beta ? BETA_CODE : PRODUCTION_CODE;
        config.Environment = environment;
        config.CertificateType = "PFX";
        config.StorageBasePath = "storage/sunat";
        config.InvoiceEndpoint = beta ? BETA_ENDPOINT : PRODUCTION_ENDPOINT;
        config.SummaryEndpoint = config.InvoiceEndpoint;
        config.TicketEndpoint = config.InvoiceEndpoint;
        config.GuideEndpoint = GUIDE_ENDPOINT;
        config.GuideTokenEndpoint = GUIDE_TOKEN_ENDPOINT;
        config.MaxSendAttempts = 3;
        config.MaxTicketAttempts = 3;
        config.SchedulerEnabled = "N";
        config.AutomaticRetryEnabled = "N";
        config.ActiveConfig = "S";
        if (beta) {
            config.SolUser = "MODDATOS";
            config.SolPassword = "MODDATOS";
            config.CertificatePath = TEST_CERTIFICATE;
            config.CertificatePassword = "taqini";
        }
        return config;
    }
}
