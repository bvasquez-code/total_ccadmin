package com.ccadmin.app.user.service;

import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.user.model.dto.AppMenuStructureDto;
import com.ccadmin.app.user.model.entity.AppMenuEntity;
import com.ccadmin.app.user.repository.AppMenuRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppMenuService extends SessionService {

    private static int limitSearchMenu = 10;
    @Autowired
    private AppMenuRepository appMenuRepository;

    private SearchService searchService;


    public AppMenuEntity save(AppMenuEntity appMenu)
    {
        if(!this.appMenuRepository.existsById(appMenu.MenuCod))
        {
            appMenu.addSession(getUserCod(),true);
        }
        else
        {
            appMenu.addSession(getUserCod(),false);
        }
        return this.appMenuRepository.save(appMenu);
    }

    public ResponsePageSearch findAll(String Query,int Page)
    {
        SearchDto search = new SearchDto(Query,Page);

        this.searchService = new SearchService(this.appMenuRepository);
        return this.searchService.findAll(search,limitSearchMenu);
    }

    public ResponseWsDto findDataForm(String MenuCod)
    {
        ResponseWsDto rpt = new ResponseWsDto();

        rpt.AddResponseAdditional(
                "AppMenu",
                this.appMenuRepository.findById(MenuCod)
        );

        rpt.AddResponseAdditional(
                "AppMenuDadList",
                this.appMenuRepository.findMenuDad()
        );

        return rpt;
    }

    public AppMenuEntity updateStatus(AppMenuEntity appMenuStatus)
    {
        AppMenuEntity appMenu = this.appMenuRepository.findById(appMenuStatus.MenuCod).get();
        appMenu.addSession(getUserCod(),false);
        appMenu.Status = appMenuStatus.Status;
        return this.appMenuRepository.save(appMenu);
    }


    public List<AppMenuStructureDto> findMenuStructure()
    {
        List<AppMenuStructureDto> appMenuStructureList = new ArrayList<>();
        List<AppMenuEntity> MenuDadList = this.appMenuRepository.findMenuDad();

        for(var MenuDad : MenuDadList)
        {
            AppMenuStructureDto appMenuStructure = new AppMenuStructureDto();
            appMenuStructure.MenuDad = MenuDad;
            appMenuStructure.MenuChildList = this.appMenuRepository.findMenuChild(MenuDad.MenuCod);
            appMenuStructureList.add(appMenuStructure);
        }

        return appMenuStructureList;
    }

    public List<AppMenuEntity> findByUser(String UserCod){
        return this.appMenuRepository.findByUser(UserCod);
    }

    public AppMenuEntity findById(String MenuCod){
        return this.appMenuRepository.findById(MenuCod).orElse(null);
    }
}
