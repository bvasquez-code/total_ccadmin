package com.ccadmin.app.system.service;

import com.ccadmin.app.system.model.entity.SystemDocumentEntity;
import com.ccadmin.app.system.repository.SystemDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemDocumentSearchService {

    @Autowired
    private SystemDocumentRepository systemDocumentRepository;


    public SystemDocumentEntity findById(String DocumentCod){
        return this.systemDocumentRepository.findById(DocumentCod).get();
    }

}
