package com.ccadmin.app.shared.model.dto;

public class ResponseValidationDto <T> {

    public String Message;
    public T Data;
    public boolean ErrorStatus;

    public ResponseValidationDto(){

    }

    public ResponseValidationDto<T> ok(T Data){
        this.Data = Data;
        this.Message = "ok";
        this.ErrorStatus = false;
        return this;
    }

    public ResponseValidationDto<T> ok(){
        this.Message = "ok";
        this.ErrorStatus = false;
        return this;
    }

    public ResponseValidationDto<T> error(String Message){
        this.Message = Message;
        this.ErrorStatus = true;
        return this;
    }
}
