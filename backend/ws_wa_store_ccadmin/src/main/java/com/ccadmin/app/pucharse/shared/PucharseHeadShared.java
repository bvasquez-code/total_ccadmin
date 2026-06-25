package com.ccadmin.app.pucharse.shared;

import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.service.PucharseHeadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PucharseHeadShared {

    @Autowired
    private PucharseHeadService pucharseHeadService;

    public List<PucharseHeadEntity> findAllById(List<String> PucharseCodList){
        return this.pucharseHeadService.findAllById(PucharseCodList);
    }
}
