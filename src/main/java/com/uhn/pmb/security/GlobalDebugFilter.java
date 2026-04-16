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
        
        long startTime = System.currentTimeMillis();
        
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalDebugFilter.class);
            if (log.isDebugEnabled()) {
                log.debug("{} {} - Status: {} - Duration: {}ms", 
                        request.getMethod(), request.getRequestURI(), response.getStatus(), duration);
            }
        }
    }
}
