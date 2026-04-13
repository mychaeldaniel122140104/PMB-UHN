package com.uhn.pmb.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Handle unauthorized access attempts (missing/invalid JWT token)
 * Returns JSON error response for API calls
 * Redirects to login for HTML page requests
 */
@Component
@Slf4j
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request,
                        HttpServletResponse response,
                        AuthenticationException authException)
            throws IOException, ServletException {

        String requestPath = request.getRequestURI();
        String acceptHeader = request.getHeader("Accept");

        log.error("❌ UNAUTHORIZED ACCESS ATTEMPT");
        log.error("   Path: {}", requestPath);
        log.error("   Method: {}", request.getMethod());
        log.error("   Error: {}", authException.getMessage());

        // 🔒 JANGAN redirect public pages - mereka harus bisa diakses langsung
        if (isPublicPage(requestPath)) {
            log.warn("   → Public page, NOT redirecting, returning 401");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", 401);
            errorResponse.put("error", "UNAUTHORIZED");
            errorResponse.put("message", "Authentication required");
            errorResponse.put("path", requestPath);
            ObjectMapper mapper = new ObjectMapper();
            response.getWriter().write(mapper.writeValueAsString(errorResponse));
            return;
        }

        // 🔒 Untuk HTML file requests, redirect ke login
        if (isHtmlPageRequest(requestPath, acceptHeader)) {
            log.warn("   → Redirecting to login.html (HTML page request)");
            response.sendRedirect("/login.html");
            return;
        }

        // 📡 Untuk API requests, return JSON error response
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", 401);
        errorResponse.put("error", "UNAUTHORIZED");
        errorResponse.put("message", "Authentication token missing or invalid");
        errorResponse.put("path", requestPath);

        if (authException instanceof BadCredentialsException) {
            errorResponse.put("message", "Invalid credentials");
        } else if (authException instanceof InsufficientAuthenticationException) {
            errorResponse.put("message", "Authentication required");
        } else {
            errorResponse.put("message", authException.getMessage());
        }

        ObjectMapper mapper = new ObjectMapper();
        response.getWriter().write(mapper.writeValueAsString(errorResponse));

        log.error("   → Returning 401 JSON response");
    }

    /**
     * Check if this is a public page or public API endpoint that should never redirect
     */
    private boolean isPublicPage(String requestPath) {
        // Public API debug endpoints
        if (requestPath.equals("/api/camaba/debug-auth") || 
            requestPath.equals("/api/admin/debug-auth")) {
            return true;
        }
        
        return requestPath.equals("/") || 
               requestPath.equals("/index.html") ||
               requestPath.equals("/login.html") ||
               requestPath.equals("/register.html") ||
               requestPath.equals("/forgot-password.html") ||
               requestPath.equals("/reset-password.html") ||
               requestPath.equals("/debug-auth.html") ||
               requestPath.equals("/test-api.html") ||
               requestPath.equals("/test-login.html") ||
               requestPath.equals("/test-token.html") ||
               requestPath.equals("/view-formulir.html") ||
               requestPath.equals("/edit-formulir.html") ||
               requestPath.equals("/form-pendaftaran.html") ||
               requestPath.equals("/dashboard-camaba.html") ||
               requestPath.equals("/dashboard-admin-pusat.html") ||
               requestPath.equals("/dashboard-admin-validasi.html") ||
               requestPath.equals("/profile.html") ||
               requestPath.equals("/notifications.html") ||
               requestPath.equals("/gelombang-selection.html") ||
               requestPath.equals("/formula-selection.html") ||
               requestPath.equals("/payment-method.html") ||
               requestPath.equals("/ujian.html");
    }

    /**
     * Check if this is an HTML page request vs API call
     */
    private boolean isHtmlPageRequest(String requestPath, String acceptHeader) {
        // If Accept header includes text/html, treat as page request
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            return true;
        }

        // If path ends with .html, treat as page request
        if (requestPath.endsWith(".html")) {
            return true;
        }

        // If path is root or contains dashboard/profile, treat as page request
        if (requestPath.equals("/") || 
            requestPath.contains("/dashboard") || 
            requestPath.contains("/profile") ||
            requestPath.contains("/form-pendaftaran") ||
            requestPath.contains("/gelombang") ||
            requestPath.contains("/formula")) {
            return true;
        }

        return false;
    }
}
