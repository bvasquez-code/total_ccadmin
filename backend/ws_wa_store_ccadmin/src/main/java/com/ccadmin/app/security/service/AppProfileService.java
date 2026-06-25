package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppProfileEntity;
import com.ccadmin.app.security.model.entity.id.ProfileMenuID;
import com.ccadmin.app.security.repository.AppProfileRepository;
import com.ccadmin.app.security.repository.ProfileMenuRepository;
import com.ccadmin.app.shared.model.dto.ResponsePageSearch;
import com.ccadmin.app.shared.model.dto.ResponseWsDto;
import com.ccadmin.app.shared.model.dto.SearchDto;
import com.ccadmin.app.shared.service.SearchService;
import com.ccadmin.app.shared.service.SessionService;
import com.ccadmin.app.user.model.dto.AppMenuStructureDto;
import com.ccadmin.app.user.shared.AppMenuShared;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppProfileService extends SessionService {

    @Autowired
    private AppProfileRepository appProfileRepository;
    @Autowired
    private ProfileMenuRepository profileMenuRepository;

    @Autowired
    private AppMenuShared appMenuShared;

    private SearchService searchService;

    @Transactional
    public AppProfileEntity save(AppProfileEntity profile)
    {
        this.profileMenuRepository.updateAllStatus("I",profile.ProfileCod);

        profile.addSession(getUserCod(),!this.appProfileRepository.existsById(profile.ProfileCod));
        this.appProfileRepository.save(profile);

        for(var item : profile.permissionsList)
        {
            item.addSession(getUserCod(),!this.profileMenuRepository.existsById( new ProfileMenuID(item.ProfileCod,item.MenuCod)));
            item.ProfileCod = profile.ProfileCod;
        }
        this.profileMenuRepository.saveAll(profile.permissionsList);

        return profile;
    }

    public ResponseWsDto findDataForm(String ProfileCod)
    {
        ResponseWsDto rpt = new ResponseWsDto();

        Optional<AppProfileEntity> appProfileOptional = this.appProfileRepository.findById(ProfileCod);

        if( appProfileOptional.isPresent() )
        {
            appProfileOptional.get().permissionsList = this.profileMenuRepository.findAllByProfile(
                    appProfileOptional.get().ProfileCod
            );
            rpt.AddResponseAdditional("Profile",appProfileOptional.get());
        }

        List<AppMenuStructureDto> appMenuStructureList = this.appMenuShared.findMenuStructure();

        rpt.AddResponseAdditional("AppMenuStructure",appMenuStructureList);

        return rpt;
    }

    public ResponsePageSearch findAll(String Query, int Page)
    {
        SearchDto search = new SearchDto(Query,Page);
        this.searchService = new SearchService(this.appProfileRepository);

        return this.searchService.findAll(search,10);
    }
}
