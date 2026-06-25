package com.local.app.pinpad.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.local.app.pinpad.constants.PinpadConstants;
import com.local.app.pinpad.enums.PinpadErrorCode;
import com.local.app.pinpad.model.dto.ResponseWsDto;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PinpadSecurityFilter extends OncePerRequestFilter {

    private final PinpadAgentProperties properties;
    private final ObjectMapper objectMapper;

    public PinpadSecurityFilter(PinpadAgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = request.getHeader(PinpadConstants.HEADER_AGENT_TOKEN);
        if (properties.getAgentToken() == null || !properties.getAgentToken().equals(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), ResponseWsDto.error(
                    PinpadErrorCode.UNAUTHORIZED_AGENT.name(), "Token local del agente invalido"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
