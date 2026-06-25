package com.ccadmin.app.user.service;

import com.ccadmin.app.user.model.entity.UserStoreEntity;
import com.ccadmin.app.user.repository.UserStoreRepository;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserStoreService {

    @Autowired
    private UserStoreRepository userStoreRepository;

    public String getMainStore(String userCod){
        return this.userStoreRepository.getMainStore(userCod);
    }

    public List<UserStoreEntity> findByUserCod(String userCod){
        return this.userStoreRepository.findByUserCod(userCod);
    }
}
