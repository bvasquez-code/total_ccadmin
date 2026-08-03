package com.ccadmin.app.shared.model.constants;

public final class BusinessConfigConstants {

    private BusinessConfigConstants(){

    }

    public static class GroupCod{
        public static final String SYSTEM_FUNCTIONALITY_ACTIVATOR = "SystemFunctionalityActivator";
        public static final String SYSTEM_FUNCTIONALITY_CONFIG = "SystemFunctionalityConfig";
        public static final String CONFIG_AUTOMATIC_PROCESS_THREADS = "ConfigAutomaticProcessThreads";
    }

    public static class ConfigCod {
        public static final String IND_PROFORMA_SALES = "IND_PROFORMA_SALES";
        public static final String IND_ADVANCE_PAYMENT = "IND_ADVANCE_PAYMENT";
        public static final String IND_MANUAL_DISCOUNT = "IND_MANUAL_DISCOUNT";
        public static final String IND_CANCEL_PENDING_AUTOMATIC_SALE = "IND_CANCEL_PENDING_AUTOMATIC_SALE";
        public static final String CANCEL_PENDING_AUTOMATIC_SALE_TIME = "CANCEL_PENDING_AUTOMATIC_SALE_TIME";
        public static final String SALE_PENDING_EXPIRATION = "SALE_PENDING_EXPIRATION";
    }

}
