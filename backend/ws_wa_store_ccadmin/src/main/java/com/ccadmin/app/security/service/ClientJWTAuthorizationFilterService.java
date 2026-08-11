package com.ccadmin.app.security.service;

import com.ccadmin.app.client.model.entity.ClientAccountEntity;
import com.ccadmin.app.client.repository.ClientAccountRepository;
import com.ccadmin.app.security.util.TokenUtil;
import com.ccadmin.app.shared.model.dto.ClientSessionDto;
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
public class ClientJWTAuthorizationFilterService extends OncePerRequestFilter {

    private final ClientAccountRepository clientAccountRepository;

    public ClientJWTAuthorizationFilterService(ClientAccountRepository clientAccountRepository) {
        this.clientAccountRepository = clientAccountRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");

        if (authorization != null && authorization.startsWith("Bearer ")) {
            String token = authorization.substring("Bearer ".length());
            UsernamePasswordAuthenticationToken authentication =
                    TokenUtil.getClientAuthenticationToken(token);

            if (authentication != null) {
                Long clientAccountID = (Long) authentication.getPrincipal();
                ClientAccountEntity clientAccount = clientAccountRepository
                        .findActiveByClientAccountID(clientAccountID)
                        .orElse(null);

                if (clientAccount != null && "S".equals(clientAccount.IsEmailVerified)) {
                    String names = clientAccountRepository.findClientNames(clientAccountID);
                    authentication.setDetails(new ClientSessionDto(
                            clientAccount.ClientAccountID,
                            clientAccount.ClientCod,
                            clientAccount.Email,
                            names
                    ));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request,response);
    }
}
