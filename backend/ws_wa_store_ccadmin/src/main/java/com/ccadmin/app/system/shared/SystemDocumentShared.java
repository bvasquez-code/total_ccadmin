package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.model.entity.SystemDocumentEntity;
import com.ccadmin.app.system.service.SystemDocumentSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SystemDocumentShared {

    @Autowired
    private SystemDocumentSearchService systemDocumentSearchService;

    public SystemDocumentEntity findById(String DocumentCod){
        return this.systemDocumentSearchService.findById(DocumentCod);
    }
}
