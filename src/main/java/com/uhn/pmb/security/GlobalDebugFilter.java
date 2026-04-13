package com.uhn.pmb.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GlobalDebugFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🌍 [GLOBAL FILTER - START]");
        System.out.println("   ➡️ " + request.getMethod() + " " + request.getRequestURI());
        
        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            System.out.println("🌍 [GLOBAL FILTER - END]");
            System.out.println("   ⬅️ RESPONSE STATUS: " + response.getStatus());
            System.out.println("   ⏱️  DURATION: " + duration + "ms");
            System.out.println("═══════════════════════════════════════════════════════════");
        }
    }
}
