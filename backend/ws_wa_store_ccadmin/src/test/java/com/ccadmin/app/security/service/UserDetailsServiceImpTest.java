package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.repository.AppUserRepository;
import com.ccadmin.app.shared.model.constants.AuditUserConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImpTest {

    @Mock
    private AppUserRepository appUserRepository;
    @InjectMocks
    private UserDetailsServiceImp userDetailsService;

    @Test
    void loadsARegularUserThroughTheAuthenticationQuery() {
        AppUserEntity appUser = new AppUserEntity();
        appUser.UserCod = "USER01";
        appUser.Password = "encoded-password";
        when(appUserRepository.findForAuthentication("USER01", AuditUserConstants.USER_WEB))
                .thenReturn(Optional.of(appUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("USER01");

        assertEquals("USER01", userDetails.getUsername());
        verify(appUserRepository).findForAuthentication("USER01", AuditUserConstants.USER_WEB);
    }

    @Test
    void rejectsTheTechnicalWebUserForAuthentication() {
        when(appUserRepository.findForAuthentication(
                AuditUserConstants.USER_WEB,
                AuditUserConstants.USER_WEB
        )).thenReturn(Optional.empty());

        assertThrows(
                UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername(AuditUserConstants.USER_WEB)
        );
    }
}
