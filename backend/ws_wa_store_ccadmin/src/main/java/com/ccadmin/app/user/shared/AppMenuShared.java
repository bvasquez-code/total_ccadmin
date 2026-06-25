package com.ccadmin.app.user.shared;

import com.ccadmin.app.user.model.dto.AppMenuStructureDto;
import com.ccadmin.app.user.model.entity.AppMenuEntity;
import com.ccadmin.app.user.service.AppMenuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppMenuShared {

    @Autowired
    private AppMenuService appMenuService;

    public List<AppMenuStructureDto> findMenuStructure()
    {
        return this.appMenuService.findMenuStructure();
    }

    public List<AppMenuEntity> findByUser(String UserCod){
        return this.appMenuService.findByUser(UserCod);
    }
}
