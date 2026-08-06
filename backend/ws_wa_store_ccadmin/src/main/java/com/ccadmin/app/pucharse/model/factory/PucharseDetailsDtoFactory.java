package com.ccadmin.app.pucharse.model.factory;

import com.ccadmin.app.pucharse.model.dto.PucharseDetailsDto;
import com.ccadmin.app.pucharse.model.entity.PucharseDetEntity;
import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;

import java.util.List;

public final class PucharseDetailsDtoFactory {

    private PucharseDetailsDtoFactory() {
    }

    public static PucharseDetailsDto fromEntities(
            PucharseHeadEntity head,
            List<PucharseDetEntity> details
    ) {
        PucharseDetailsDto result = new PucharseDetailsDto();
        result.Headboard = head;
        result.DetailList = details;
        return result;
    }
}
