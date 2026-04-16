package com.uhn.pmb.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .reduce("", (acc, auth) -> acc.isEmpty() ? auth : acc + "," + auth);
        
        if (log.isDebugEnabled()) {
            log.debug("JWT token generated for email: {} with authorities: {}", 
                    email, authorities.isEmpty() ? "NONE" : authorities);
        }
        
        return generateTokenFromEmailWithAuthorities(email, authorities);
    }

    public String generateTokenFromEmail(String email) {
        return generateTokenFromEmailWithAuthorities(email, "");
    }

    public String generateTokenFromEmailWithAuthorities(String email, String authorities) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret is not configured");
                throw new RuntimeException("JWT secret is not configured");
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            JWTCreator.Builder builder = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs));
            
            if (authorities != null && !authorities.isEmpty()) {
                builder.withClaim("authorities", authorities);
            }
            
            String token = builder.sign(algorithm);
            log.debug("JWT token generated for email: {}", email);
            return token;
        } catch (Exception e) {
            log.error("Error generating JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage());
        }
    }

    public String getEmailFromToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret is not configured during email extraction");
                return null;
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            String email = JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .getSubject();
            
            log.debug("Email extracted from token: {}", email);
            return email;
        } catch (JWTVerificationException e) {
            log.error("JWT verification failed: {}", e.getMessage());
            return null;
        }
    }

    public String getAuthoritiesFromToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret is not configured during authority extraction");
                return "";
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            String authorities = JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .getClaim("authorities")
                    .asString();
            
            log.debug("Authorities extracted from token: {}", authorities != null ? authorities : "NONE");
            return authorities != null ? authorities : "";
        } catch (Exception e) {
            log.debug("No authorities claim in token: {}", e.getMessage());
            return "";
        }
    }

    public Boolean validateToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("JWT secret is not configured during validation");
                return false;
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            JWT.require(algorithm)
                    .build()
                    .verify(token);
            
            log.debug("JWT token validated successfully");
            return true;
        } catch (JWTVerificationException e) {
            log.error("JWT verification failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Unexpected error during JWT validation: {}", e.getMessage());
            return false;
        }
    }
}
