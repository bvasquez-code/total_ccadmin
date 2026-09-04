package com.ccadmin.app.sunat.model.constants;

public final class SunatSubmissionConstants {

    public static final int PAGE_SIZE = 15;
    public static final long SENDING_RETRY_DELAY_MILLIS = 5 * 60 * 1000L;

    public static final String REQUEST_TYPE_INVOICE = "INVOICE";
    public static final String REQUEST_TYPE_RECEIPT = "RECEIPT";
    public static final String REQUEST_TYPE_CREDIT_NOTE = "CREDIT_NOTE";
    public static final String REQUEST_TYPE_DEBIT_NOTE = "DEBIT_NOTE";
    public static final String REQUEST_TYPE_DESPATCH_ADVICE = "DESPATCH_ADVICE";

    public static final String SEND_STATUS_PENDING = "P";
    public static final String SEND_STATUS_SENDING = "W";
    public static final String SEND_STATUS_SENT = "S";
    public static final String SEND_STATUS_ERROR = "E";

    private SunatSubmissionConstants() {
    }
}
