package com.ccadmin.app.system.shared;

import com.ccadmin.app.system.model.entity.AppFileEntity;
import com.ccadmin.app.system.service.AppFileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AppFileShared {

    @Autowired
    private AppFileService appFileService;

    public AppFileEntity findById(String FileCod)
    {
        return this.appFileService.findById(FileCod);
    }
}
