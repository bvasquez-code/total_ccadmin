package com.ccadmin.app.security.util;

import com.ccadmin.app.security.model.constants.SecurityAuthorityConstants;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TokenUtilTest {

    @Test
    void administrativeTokenKeepsAdministrativeAuthorityOnly() {
        String token = TokenUtil.createToken("ADMIN01", "admin@example.com");

        UsernamePasswordAuthenticationToken administration = TokenUtil.getAuthenticationToken(token);
        assertNotNull(administration);
        assertEquals("ADMIN01", administration.getPrincipal());
        assertEquals(
                SecurityAuthorityConstants.ADMIN_APPLICATION,
                administration.getAuthorities().iterator().next().getAuthority()
        );
        assertNull(TokenUtil.getClientAuthenticationToken(token));
    }

    @Test
    void clientTokenCannotBeUsedAsAdministrativeToken() {
        String token = TokenUtil.createClientToken(45L, "cliente@example.com");

        assertNull(TokenUtil.getAuthenticationToken(token));
        UsernamePasswordAuthenticationToken clientAuthentication =
                TokenUtil.getClientAuthenticationToken(token);
        assertNotNull(clientAuthentication);
        assertEquals(45L, clientAuthentication.getPrincipal());
        assertEquals(
                SecurityAuthorityConstants.CLIENT,
                clientAuthentication.getAuthorities().iterator().next().getAuthority()
        );
    }
}
