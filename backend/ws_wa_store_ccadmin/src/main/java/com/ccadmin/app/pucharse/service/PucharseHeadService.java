package com.ccadmin.app.pucharse.service;

import com.ccadmin.app.pucharse.model.entity.PucharseHeadEntity;
import com.ccadmin.app.pucharse.repository.PucharseHeadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PucharseHeadService {

    @Autowired
    private PucharseHeadRepository pucharseHeadRepository;

    public List<PucharseHeadEntity> findAllById(List<String> PucharseCodList){
        return this.pucharseHeadRepository.findAllById(PucharseCodList);
    }
}
