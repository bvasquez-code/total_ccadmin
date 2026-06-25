package com.ccadmin.app.security.util;



import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.*;

import static io.jsonwebtoken.Jwts.*;

public class TokenUtil {

    private final static String ACCESS_TOKEN_SECRET = "PERO LA PUTA MADRE,FUNCIONA TOKEN DE MIERDA, APURAJ CARAJO, MALNACIDO, HIJO DE LA CHINGADA, LA QUE TE PARIO 544545454545454545454545454545";
    private final static Long ACCESS_TOKEN_VALIDITY_SECONDS = (long)(30 * 24 * 60 * 60);

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

    public static UsernamePasswordAuthenticationToken getAuthenticationToken(String token)
    {
        try{
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(ACCESS_TOKEN_SECRET.getBytes())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userCod = claims.getSubject();

            return new UsernamePasswordAuthenticationToken(userCod,null, Collections.emptyList());
        }
        catch (JwtException ex)
        {
            return null;
        }
    }
}
