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
        // ✅ ENCODE AUTHORITIES/ROLES INTO JWT TOKEN
        String email = authentication.getName();
        String authorities = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .reduce("", (acc, auth) -> acc.isEmpty() ? auth : acc + "," + auth);
        
        log.info("═══════════════════════════════════════════════════════════");
        log.info("🔐 [JWT-GENERATE-TOKEN] Authentication object received");
        log.info("   Principal: {}", email);
        log.info("   Authorities count: {}", authentication.getAuthorities().size());
        log.info("   Authorities string: {}", authorities.isEmpty() ? "EMPTY⚠️" : authorities);
        
        if (authentication.getAuthorities().isEmpty()) {
            log.error("❌ [JWT-GENERATE] WARNING: Authentication has NO authorities!");
        } else {
            authentication.getAuthorities().forEach(auth ->
                log.info("     ✓ Authority detail: {}", auth.getAuthority()));
        }
        log.info("═══════════════════════════════════════════════════════════");
        
        return generateTokenFromEmailWithAuthorities(email, authorities);
    }

    public String generateTokenFromEmail(String email) {
        return generateTokenFromEmailWithAuthorities(email, "");
    }

    public String generateTokenFromEmailWithAuthorities(String email, String authorities) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("❌ JWT_SECRET is NULL or EMPTY!");
                throw new RuntimeException("JWT secret is not configured");
            }
            
            log.info("🔐 JWT Generation Debug:");
            log.info("   Email: {}", email);
            log.info("   Authorities: {}", authorities.isEmpty() ? "NONE" : authorities);
            log.info("   Secret length: {}", jwtSecret.length());
            log.info("   Expiration: {}ms", jwtExpirationMs);
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            JWTCreator.Builder builder = JWT.create()
                    .withSubject(email)
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + jwtExpirationMs));
            
            // ✅ ADD AUTHORITIES CLAIM IF PROVIDED
            if (authorities != null && !authorities.isEmpty()) {
                builder.withClaim("authorities", authorities);
                log.info("   ✅ Authorities ADDED to token!");
            }
            
            String token = builder.sign(algorithm);
            
            log.info("✅ JWT token generated for email: {} (Length: {})", email, token.length());
            return token;
        } catch (Exception e) {
            log.error("❌ Error generating JWT token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage());
        }
    }

    public String getEmailFromToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("❌ JWT_SECRET is NULL or EMPTY during extraction!");
                return null;
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            String email = JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .getSubject();
            
            log.debug("✅ Email extracted from token: {}", email);
            return email;
        } catch (JWTVerificationException e) {
            log.error("❌ JWT verification failed: {}", e.getMessage());
            return null;
        }
    }

    // ✅ NEW: Extract authorities from JWT token
    public String getAuthoritiesFromToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("❌ JWT_SECRET is NULL or EMPTY during authority extraction!");
                return "";
            }
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            String authorities = JWT.require(algorithm)
                    .build()
                    .verify(token)
                    .getClaim("authorities")
                    .asString();
            
            log.debug("✅ Authorities extracted from token: {}", authorities != null ? authorities : "NONE");
            return authorities != null ? authorities : "";
        } catch (Exception e) {
            log.debug("⚠️ No authorities claim in token (expected for some tokens): {}", e.getMessage());
            return "";
        }
    }

    public Boolean validateToken(String token) {
        try {
            if (jwtSecret == null || jwtSecret.isEmpty()) {
                log.error("❌ JWT_SECRET is NULL or EMPTY during validation!");
                return false;
            }
            
            log.info("🔐 JWT Validation Debug:");
            log.info("   Token length: {}", token.length());
            log.info("   Token prefix: {}...", token.substring(0, Math.min(20, token.length())));
            log.info("   Secret length: {}", jwtSecret.length());
            log.info("   Secret value: {}", jwtSecret);
            
            Algorithm algorithm = Algorithm.HMAC512(jwtSecret);
            JWT.require(algorithm)
                    .build()
                    .verify(token);
            
            log.debug("✅ JWT token validated successfully");
            return true;
        } catch (JWTVerificationException e) {
            log.error("❌ JWT verification failed: {}", e.getMessage());
            log.error("   Exception type: {}", e.getClass().getSimpleName());
            return false;
        } catch (Exception e) {
            log.error("❌ Unexpected error during JWT validation: {}", e.getMessage());
            log.error("   Exception: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
