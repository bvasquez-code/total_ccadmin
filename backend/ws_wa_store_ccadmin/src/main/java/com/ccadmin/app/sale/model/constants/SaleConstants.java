package com.ccadmin.app.sale.model.constants;

public final class SaleConstants {

    public static final String DOCUMENT_TYPE_INVOICE = "01";
    public static final String DOCUMENT_TYPE_RECEIPT = "03";
    public static final String DOCUMENT_TYPE_PROFORMA = "99";
    public static final String DOCUMENT_ROLE_INTERNAL = "I";
    public static final String DOCUMENT_ROLE_FISCAL = "F";
    public static final String DOCUMENT_ROLE_OTHER = "O";
    public static final String COMMERCIAL_CHANNEL_IN_PERSON = "IN_PERSON";
    public static final String COMMERCIAL_CHANNEL_WEB = "WEB";

    public static final String KARDEX_ZONE_SOURCE_PRESALE = "presale_head";
    public static final String KARDEX_ZONE_EVENT_RESERVATION = "PRESALE_RESERVATION";
    public static final String KARDEX_ZONE_SOURCE_SALE = "sale_head";
    public static final String KARDEX_ZONE_EVENT_CONFIRMATION = "SALE_CONFIRMATION";
    public static final String KARDEX_ZONE_EVENT_EXPIRATION_RELEASE = "SALE_EXPIRATION_RELEASE";
    public static final String KARDEX_ZONE_SOURCE_CREDIT_NOTE = "credit_note_head";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_CONFIRMATION = "CREDIT_NOTE_CONFIRMATION";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_ACCEPTED_RETURN = "CREDIT_NOTE_ACCEPTED_RETURN";
    public static final String KARDEX_ZONE_EVENT_CREDIT_NOTE_REJECTED_STOCK_EXIT = "CREDIT_NOTE_REJECTED_STOCK_EXIT";

    public static final String DELIVERY_STATUS_PENDING = "P";
    public static final String DELIVERY_STATUS_SCHEDULED = "S";
    public static final String DELIVERY_STATUS_PREPARING = "R";
    public static final String DELIVERY_STATUS_READY_FOR_PICKUP = "L";
    public static final String DELIVERY_STATUS_DISPATCHED = "D";
    public static final String DELIVERY_STATUS_DELIVERED = "E";
    public static final String DELIVERY_STATUS_CANCELLED = "X";
    public static final String DELIVERY_STATUS_FAILED = "F";

    public static final String CART_STATUS_ACTIVE = "A";
    public static final String CART_STATUS_CONVERTED = "C";
    public static final String CART_STATUS_ABANDONED = "B";
    public static final String CART_STATUS_EXPIRED = "E";

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
