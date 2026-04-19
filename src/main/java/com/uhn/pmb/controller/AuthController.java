package com.uhn.pmb.controller;

import com.uhn.pmb.dto.AuthResponse;
import com.uhn.pmb.dto.LoginRequest;
import com.uhn.pmb.dto.RegisterRequest;
import com.uhn.pmb.dto.ForgotPasswordRequest;
import com.uhn.pmb.dto.ResetPasswordRequest;
import com.uhn.pmb.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse(false, "Password tidak cocok"));
            }

            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            log.error("Registration error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Login error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(@RequestParam String token) {
        try {
            AuthResponse response = authService.verifyEmail(token);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Email verification error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerification(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            AuthResponse response = authService.resendVerificationEmail(email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Resend verification error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            AuthResponse response = authService.forgotPassword(request.getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Forgot password error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            AuthResponse response = authService.resetPassword(request.getToken(), request.getPassword(), request.getConfirmPassword());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Reset password error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping("/validate-token")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String token) {
        try {
            String jwtToken = token.replace("Bearer ", "");
            Boolean isValid = authService.validateToken(jwtToken);
            
            if (isValid) {
                return ResponseEntity.ok(new ApiResponse(true, "Token valid"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(false, "Token tidak valid"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ApiResponse(false, "Token validation failed"));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(new ApiResponse(true, "Auth service is running"));
    }

    // Inner class for generic API response
    @lombok.Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class ApiResponse {
        private Boolean success;
        private String message;
    }
}
