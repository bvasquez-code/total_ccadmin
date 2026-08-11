package com.ccadmin.app.security.util;



import com.ccadmin.app.security.model.constants.SecurityAuthorityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

import static io.jsonwebtoken.Jwts.*;

public class TokenUtil {

    private final static String ACCESS_TOKEN_SECRET = "PERO LA PUTA MADRE,FUNCIONA TOKEN DE MIERDA, APURAJ CARAJO, MALNACIDO, HIJO DE LA CHINGADA, LA QUE TE PARIO 544545454545454545454545454545";
    private final static Long ACCESS_TOKEN_VALIDITY_SECONDS = (long)(30 * 24 * 60 * 60);
    private final static String TOKEN_TYPE_CLAIM = "tokenType";
    private final static String TOKEN_TYPE_CLIENT = "CLIENT";

    public static String createToken(String userCod,String email)
    {
        long expirationTime = ACCESS_TOKEN_VALIDITY_SECONDS * 1000;
        Date dateExpiration = new Date(System.currentTimeMillis()+expirationTime);

        Map<String,Object> extraData = new HashMap<>();
        extraData.put("email",email);

        return builder()
                .setSubject(userCod)
                .setExpiration(dateExpiration)
                .addClaims(extraData)
                .signWith(Keys.hmacShaKeyFor(ACCESS_TOKEN_SECRET.getBytes()))
                .compact();
    }

    public static String createClientToken(Long clientAccountID, String email)
    {
        long expirationTime = ACCESS_TOKEN_VALIDITY_SECONDS * 1000;
        Date dateExpiration = new Date(System.currentTimeMillis()+expirationTime);

        Map<String,Object> extraData = new HashMap<>();
        extraData.put("email",email);
        extraData.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_CLIENT);

        return builder()
                .setSubject(clientAccountID.toString())
                .setExpiration(dateExpiration)
                .addClaims(extraData)
                .signWith(Keys.hmacShaKeyFor(ACCESS_TOKEN_SECRET.getBytes()))
                .compact();
    }

    public static UsernamePasswordAuthenticationToken getAuthenticationToken(String token)
    {
        try{
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(ACCESS_TOKEN_SECRET.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (TOKEN_TYPE_CLIENT.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return null;
            }

            String userCod = claims.getSubject();

            return new UsernamePasswordAuthenticationToken(
                    userCod,
                    null,
                    List.of(new SimpleGrantedAuthority(SecurityAuthorityConstants.ADMIN_APPLICATION))
            );
        }
        catch (JwtException ex)
        {
            return null;
        }
    }

    public static UsernamePasswordAuthenticationToken getClientAuthenticationToken(String token)
    {
        try{
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(ACCESS_TOKEN_SECRET.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (!TOKEN_TYPE_CLIENT.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                return null;
            }

            Long clientAccountID = Long.valueOf(claims.getSubject());
            return new UsernamePasswordAuthenticationToken(
                    clientAccountID,
                    null,
                    List.of(new SimpleGrantedAuthority(SecurityAuthorityConstants.CLIENT))
            );
        }
        catch (JwtException | IllegalArgumentException ex)
        {
            return null;
        }
    }
}
