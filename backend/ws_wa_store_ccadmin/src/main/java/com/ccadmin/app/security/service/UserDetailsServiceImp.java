package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppUserRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImp implements UserDetailsService {

    @Autowired
    AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String UserCod) throws UsernameNotFoundException {

        AppUserEntity appUserEntity = appUserRepository.findForAuthentication(
                        UserCod,
                        AuditUserConstants.USER_WEB
                )
                .orElseThrow(() -> new UsernameNotFoundException("User no exist"));

        UserDetailsImp userDetailsImp = new UserDetailsImp(appUserEntity);

        return userDetailsImp;
    }

}
