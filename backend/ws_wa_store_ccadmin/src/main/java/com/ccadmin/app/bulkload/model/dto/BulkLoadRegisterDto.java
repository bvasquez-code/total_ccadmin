package com.ccadmin.app.bulkload.model.dto;

import com.ccadmin.app.bulkload.model.entity.BulkLoadDestinationEntity;
import com.ccadmin.app.bulkload.model.entity.BulkLoadHeadEntity;

import java.util.List;

public class BulkLoadRegisterDto {
    public BulkLoadHeadEntity Head;
    public List<BulkLoadDestinationEntity> DestinationList;
    public List<BulkLoadErrorDto> ErrorList;

    public BulkLoadRegisterDto(BulkLoadHeadEntity head, List<BulkLoadDestinationEntity> destinationList) {
        this(head, destinationList, List.of());
    }

    public BulkLoadRegisterDto(BulkLoadHeadEntity head,
                               List<BulkLoadDestinationEntity> destinationList,
                               List<BulkLoadErrorDto> errorList) {
        this.Head = head;
        this.DestinationList = destinationList;
        this.ErrorList = errorList;
    }
}
