package com.ccadmin.app.sale.model.factory;

import com.ccadmin.app.sale.model.dto.PresaleDetailDto;
import com.ccadmin.app.sale.model.entity.PresaleChannelEntity;
import com.ccadmin.app.sale.model.entity.PresaleDetEntity;
import com.ccadmin.app.sale.model.entity.PresaleHeadEntity;

import java.util.List;

public final class PresaleDetailDtoFactory {

    private PresaleDetailDtoFactory() {
    }

    public static PresaleDetailDto fromEntities(
            PresaleHeadEntity head,
            List<PresaleDetEntity> details,
            PresaleChannelEntity channel
    ) {
        PresaleDetailDto result = new PresaleDetailDto();
        result.Headboard = head;
        result.DetailList = details;
        result.PresaleChannel = channel;
        return result;
    }
}
