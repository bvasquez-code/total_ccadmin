package com.local.app.pinpad.model.dto;

public class ResponseWsDto<T> {

    private boolean success;
    private String message;
    private String errorCode;
    private T data;

    public ResponseWsDto() {
    }

    public ResponseWsDto(boolean success, String message, String errorCode, T data) {
        this.success = success;
        this.message = message;
        this.errorCode = errorCode;
        this.data = data;
    }

    public static <T> ResponseWsDto<T> ok(T data, String message) {
        return new ResponseWsDto<>(true, message, null, data);
    }

    public static <T> ResponseWsDto<T> error(String errorCode, String message) {
        return new ResponseWsDto<>(false, message, errorCode, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
