package com.ccadmin.app.sale.model.constants;

public final class SaleConstants {

    public static final String KARDEX_ZONE_SOURCE_PRESALE = "presale_head";
    public static final String KARDEX_ZONE_EVENT_RESERVATION = "PRESALE_RESERVATION";
    public static final String KARDEX_ZONE_SOURCE_SALE = "sale_head";
    public static final String KARDEX_ZONE_EVENT_CONFIRMATION = "SALE_CONFIRMATION";
    public static final String KARDEX_ZONE_EVENT_EXPIRATION_RELEASE = "SALE_EXPIRATION_RELEASE";
    public static final String KARDEX_ZONE_SOURCE_CREDIT_NOTE = "credit_note_head";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION = "CREDIT_NOTE_CONFIRMATION";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_ACCEPTED_RETURN = "CREDIT_NOTE_ACCEPTED_RETURN";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_REJECTED_STOCK_EXIT = "CREDIT_NOTE_REJECTED_STOCK_EXIT";

    /**
     * Status: Pending
     */
    public final static String PENDING = "P";

    /**
     * Status: Confirmed
     */
    public final static String CONFIRMED = "C";

    /**
     * Status: Rejected
     */
    public final static String REJECTED = "R";
    /**
     * Status: Finalized
     */
    public final static String FINALIZED = "F";
    public final static String CANCELLED = "X";
}
