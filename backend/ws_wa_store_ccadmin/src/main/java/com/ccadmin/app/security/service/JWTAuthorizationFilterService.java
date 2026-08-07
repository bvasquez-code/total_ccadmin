package com.ccadmin.app.security.service;

import com.ccadmin.app.security.model.entity.AppSessionEntity;
import com.ccadmin.app.security.repository.AppSessionRepository;
import com.ccadmin.app.security.util.TokenUtil;
import com.ccadmin.app.shared.model.dto.SessionDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JWTAuthorizationFilterService extends OncePerRequestFilter {

    private final AppSessionRepository appSessionRepository;

    public JWTAuthorizationFilterService(AppSessionRepository appSessionRepository) {
        this.appSessionRepository = appSessionRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        if( bearerToken != null && bearerToken.startsWith("Bearer ") )
        {
            String token = bearerToken.replace("Bearer ","");
            UsernamePasswordAuthenticationToken userNamePAT = TokenUtil.getAuthenticationToken(token);
            if (userNamePAT != null) {
                AppSessionEntity appSession = appSessionRepository.findActiveByToken(token).orElse(null);
                if (appSession != null && appSession.UserCod.equals(userNamePAT.getPrincipal().toString())) {
                    SessionDto sessionDto = new SessionDto();
                    sessionDto.SessionID = appSession.SessionID;
                    sessionDto.CashSessionID = appSession.CashSessionID;
                    sessionDto.UserCod = appSession.UserCod;
                    userNamePAT.setDetails(sessionDto);
                    SecurityContextHolder.getContext().setAuthentication(userNamePAT);
                }
            }
        }

        filterChain.doFilter(request,response);
    }
}
