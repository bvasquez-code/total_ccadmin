package com.ccadmin.app.sunat.model.constants;

public final class SunatClientConstants {

    public static final String URL_STATUS_ACTIVE = "A";
    public static final String RESPONSE_STATUS_OK = "200";
    public static final String OPERATION_PROCESS = "process";

    public static final String URL_KEY_INVOICE = "01_invoice";
    public static final String URL_KEY_RECEIPT = "03_receipt";
    public static final String URL_KEY_CREDIT_NOTE = "07_creditNote";
    public static final String URL_KEY_DEBIT_NOTE = "08_debitNote";
    public static final String URL_KEY_DESPATCH_ADVICE = "09_despatchAdvice";

    public static final String MESSAGE_INACTIVE_INVOICE_URL = "Url processInvoice inactiva";
    public static final String MESSAGE_INACTIVE_RECEIPT_URL = "Url processReceipt inactiva";
    public static final String MESSAGE_INACTIVE_CREDIT_NOTE_URL = "Url processCreditNote inactiva";
    public static final String MESSAGE_INACTIVE_DEBIT_NOTE_URL = "Url processDebitNote inactiva";
    public static final String MESSAGE_INACTIVE_DESPATCH_ADVICE_URL = "Url processDespatchAdvice inactiva";

    private SunatClientConstants() {
    }
}
