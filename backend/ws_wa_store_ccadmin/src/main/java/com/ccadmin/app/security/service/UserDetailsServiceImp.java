package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.security.repository.AppUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class UserDetailsServiceImp implements UserDetailsService {

    @Autowired
    AppUserRepository appUserRepository;

    @Autowired
    AppSessionRepository appSessionRepository;

    @Override
    public UserDetails loadUserByUsername(String UserCod) throws UsernameNotFoundException {

        if(!appUserRepository.existsById(UserCod) )
        {
            throw new UsernameNotFoundException("User no exist");
        }
        AppUserEntity appUserEntity = appUserRepository.findById(UserCod).get();

        UserDetailsImp userDetailsImp = new UserDetailsImp(appUserEntity,this.appSessionRepository);

        return userDetailsImp;
    }

}
