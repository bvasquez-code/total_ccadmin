package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.model.entity.AppUserEntity;
import com.ccadmin.app.security.util.TokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
public class JWTAuthenticationFilterService extends UsernamePasswordAuthenticationFilter {

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        AppUserEntity appUserEntity = new AppUserEntity();
        try{
            appUserEntity = new ObjectMapper().readValue(request.getReader(),AppUserEntity.class);
        }
        catch (IOException e)
        {

        }

        UsernamePasswordAuthenticationToken usernamePAT = new UsernamePasswordAuthenticationToken(
                appUserEntity.UserCod,
                appUserEntity.Password,
                Collections.emptyList()
        );

        return getAuthenticationManager().authenticate(usernamePAT);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {

        UserDetailsImp userDetailsImp = (UserDetailsImp) authResult.getPrincipal();

        String token = TokenUtil.createToken(userDetailsImp.getUsername(),userDetailsImp.getEmail());

        sessionDB(userDetailsImp,token);

        response.addHeader("Authorization", "Bearer "+token);
        response.getWriter().flush();
        super.successfulAuthentication(request, response, chain, authResult);
    }

    @Transactional
    public void sessionDB(UserDetailsImp userDetailsImp,String token)
    {
        userDetailsImp.appSessionRepository.save(new AppSessionEntity(userDetailsImp.getUsername(),token));
    }
}
