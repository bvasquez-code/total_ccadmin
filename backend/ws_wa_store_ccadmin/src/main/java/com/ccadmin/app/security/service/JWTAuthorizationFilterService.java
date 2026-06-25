package com.ccadmin.app.security.service;

import com.ccadmin.app.security.util.TokenUtil;
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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String bearerToken = request.getHeader("Authorization");

        if( bearerToken != null && bearerToken.startsWith("Bearer ") )
        {
            String token = bearerToken.replace("Bearer ","");
            UsernamePasswordAuthenticationToken userNamePAT = TokenUtil.getAuthenticationToken(token);
            SecurityContextHolder.getContext().setAuthentication(userNamePAT);
        }

        filterChain.doFilter(request,response);
    }
}
