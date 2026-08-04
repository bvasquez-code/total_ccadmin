package com.ccadmin.app.shared.model.dto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ResponseWsDto {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseWsDto.class);

    public String Status;
    public String Message;
    public Object Data;
    public boolean ErrorStatus;
    public long ErrorID;
    public List<ResponseAdditionalDto> DataAdditional;

    public ResponseWsDto() {
        this.DataAdditional = new ArrayList<>();
    }

    public ResponseWsDto(Object data) {
        this();
        this.Data = data;
        this.ok();
    }

    public ResponseWsDto(Exception exception) {
        this();
        LOGGER.error(exception.getMessage(), exception);
        this.Data = exception.getMessage();
        this.error();
        this.Message = exception.getMessage();
    }

    private void ok() {
        this.Status = "200";
        this.Message = "operation performed successfully";
    }

    private void error() {
        this.Status = "500";
        this.Message = "Error : An unexpected error occurred";
        this.ErrorStatus = true;
    }
}
