package com.ccadmin.app.transfer.model.constants;

public final class TransferConstants {

    public static final String TYPE_OPERATION_REQUEST = "TR";
    public static final String TYPE_OPERATION_SEND = "TS";

    public static final String STATUS_APPROVED = "A";
    public static final String STATUS_PENDING = "P";
    public static final String STATUS_CONFIRMED = "C";
    public static final String STATUS_REJECTED = "R";
    public static final String STATUS_CANCELLED = "X";

    public static final String TRANSPORT_PUBLIC = "01";
    public static final String TRANSPORT_PRIVATE = "02";

    public static final String KARDEX_SOURCE_TABLE = "transfer_head";
    public static final String KARDEX_TYPE_OUT = "R";
    public static final String KARDEX_TYPE_IN = "S";

    public static final String DOCUMENT_TYPE_TRANSFER = "09";

    private TransferConstants() {
    }
}
