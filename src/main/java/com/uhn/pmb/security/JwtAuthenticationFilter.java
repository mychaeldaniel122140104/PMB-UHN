package com.uhn.pmb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            System.out.println("╔════════════════════════════════════════════╗");
            System.out.println("📥 [REQUEST MASUK]");
            System.out.println("   PATH: " + request.getRequestURI());
            System.out.println("   METHOD: " + request.getMethod());
            
            String requestPath = request.getRequestURI();
            String method = request.getMethod();
            String jwt = getJwtFromRequest(request);

            System.out.println("   JWT TOKEN: " + (jwt != null ? "✅ EXIST" : "❌ NULL"));
            
            // 🔓 Only process JWT if token exists, otherwise skip
            if (StringUtils.hasText(jwt)) {
                try {
                    System.out.println("   ├─ Validating token...");
                    if (jwtTokenProvider.validateToken(jwt)) {
                        String email = jwtTokenProvider.getEmailFromToken(jwt);
                        System.out.println("   ├─ Token valid ✅");
                        System.out.println("   ├─ Email: " + email);
                        
                        if (email != null) {
                            // ✅ NEW: Try to extract authorities from JWT token first
                            String tokenAuthorities = jwtTokenProvider.getAuthoritiesFromToken(jwt);
                            
                            System.out.println("   ├─ Authorities from JWT: " + (tokenAuthorities != null && !tokenAuthorities.isEmpty() ? tokenAuthorities : "EMPTY/NULL"));
                            
                            if (tokenAuthorities != null && !tokenAuthorities.isEmpty()) {
                                // ✅ Use authorities from JWT token
                                org.springframework.security.core.GrantedAuthority[] authorities = 
                                    java.util.Arrays.stream(tokenAuthorities.split(","))
                                        .map(auth -> {
                                            String trimmed = auth.trim();
                                            System.out.println("   │  ✓ Setting authority: " + trimmed);
                                            return new org.springframework.security.core.authority.SimpleGrantedAuthority(trimmed);
                                        })
                                        .toArray(org.springframework.security.core.GrantedAuthority[]::new);
                                
                                System.out.println("   ├─ Total authorities: " + authorities.length);
                                
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                email, null, java.util.Arrays.asList(authorities));
                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                
                                System.out.println("✅ [SECURITY CONTEXT SET]");
                                System.out.println("   USER: " + email);
                                System.out.println("   AUTHORITIES: " + java.util.Arrays.stream(authorities).map(a -> a.getAuthority()).collect(java.util.stream.Collectors.toList()));
                            } else {
                                // Fallback: Load from database if no authorities in token
                                System.out.println("   ⚠️ No authorities in JWT, loading from database...");
                                UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                                System.out.println("   ├─ DB Authorities: " + userDetails.getAuthorities().stream()
                                    .map(a -> a.getAuthority()).collect(java.util.stream.Collectors.joining(", ")));
                                
                                UsernamePasswordAuthenticationToken authentication =
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());
                                authentication.setDetails(
                                        new WebAuthenticationDetailsSource().buildDetails(request));
                                SecurityContextHolder.getContext().setAuthentication(authentication);
                                
                                System.out.println("✅ [SECURITY CONTEXT SET FROM DB]");
                                System.out.println("   USER: " + email);
                            }
                        }
                    } else {
                        System.out.println("   ❌ Token validation FAILED");
                    }
                } catch (Exception e) {
                    System.out.println("   ❌ Token error: " + e.getMessage());
                }
            } else {
                System.out.println("   ℹ️ No JWT token in request");
            }
            
            System.out.println("➡️ [LANJUT KE FILTER BERIKUTNYA]");
            System.out.println("╚════════════════════════════════════════════╝");
        } catch (Exception e) {
            System.out.println("   ❌ JWT Filter error: " + e.getMessage());
        }

        // ALWAYS continue - never block
        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
