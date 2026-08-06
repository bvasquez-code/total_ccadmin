package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.dto.PucharseRequestDetailsDto;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseRequestHeadEntity;

import java.util.List;

public final class PucharseRequestDetailsDtoFactory {

    private PucharseRequestDetailsDtoFactory() {
    }

    public static PucharseRequestDetailsDto fromEntities(
            PucharseRequestHeadEntity head,
            List<PucharseRequestDetEntity> details
    ) {
        PucharseRequestDetailsDto result = new PucharseRequestDetailsDto();
        result.Headboard = head;
        result.DetailList = details;
        return result;
    }
}
