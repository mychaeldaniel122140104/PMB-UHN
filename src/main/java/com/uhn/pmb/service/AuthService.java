package com.uhn.pmb.service;

import com.uhn.pmb.dto.LoginRequest;
import com.uhn.pmb.dto.RegisterRequest;
import com.uhn.pmb.dto.AuthResponse;
import com.uhn.pmb.entity.User;
import com.uhn.pmb.entity.Student;
import com.uhn.pmb.entity.PasswordResetToken;
import com.uhn.pmb.repository.UserRepository;
import com.uhn.pmb.repository.StudentRepository;
import com.uhn.pmb.repository.PasswordResetTokenRepository;
import com.uhn.pmb.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final StudentRepository studentRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:9500}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email sudah terdaftar");
        }

        // Determine role from request, default to CAMABA if not specified
        User.UserRole role = User.UserRole.CAMABA;
        if (request.getRole() != null && !request.getRole().isEmpty()) {
            try {
                role = User.UserRole.valueOf(request.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role provided: {}, defaulting to CAMABA", request.getRole());
                role = User.UserRole.CAMABA;
            }
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .isActive(true)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {} with role: {}", request.getEmail(), role);

        // Create student profile only for CAMABA users
        if (role == User.UserRole.CAMABA) {
            Student student = Student.builder()
                    .user(user)
                    .fullName(request.getFullName() != null ? request.getFullName() : request.getEmail().split("@")[0])
                    .nik(UUID.randomUUID().toString().substring(0, 12))
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            studentRepository.save(student);
            log.info("Student profile created for: {}", request.getEmail());
        }

        return AuthResponse.builder()
                .message("Registrasi berhasil sebagai " + role)
                .success(true)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            log.info("═══════════════════════════════════════════════════════════");
            log.info("🔐 [LOGIN] Starting authentication for: {}", request.getEmail());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );
            
            log.info("✅ [LOGIN] Authentication successful for: {}", request.getEmail());
            log.info("👮‍♂️ [LOGIN] Authorities in Authentication object: {}", 
                authentication.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .reduce("", (acc, a) -> acc.isEmpty() ? a : acc + ", " + a));
            log.info("═══════════════════════════════════════════════════════════");

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User tidak ditemukan"));

            if (!user.getIsActive()) {
                throw new RuntimeException("Akun telah dinonaktifkan");
            }

            String token = jwtTokenProvider.generateToken(authentication);
            log.info("User login successfully: {}", request.getEmail());

            return AuthResponse.builder()
                    .token(token)
                    .email(user.getEmail())
                    .role(user.getRole().toString())
                    .message("Login berhasil")
                    .success(true)
                    .build();

        } catch (AuthenticationException e) {
            log.error("Login failed: {}", e.getMessage());
            throw new RuntimeException("Email atau password salah");
        }
    }

    public AuthResponse forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email tidak ditemukan"));

        // Generate reset token
        String resetToken = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);

        // Invalidate previous tokens
        tokenRepository.findByUserAndIsUsedFalse(user).ifPresent(token -> {
            token.setIsUsed(true);
            tokenRepository.save(token);
        });

        PasswordResetToken token = PasswordResetToken.builder()
                .token(resetToken)
                .user(user)
                .expiryDate(expiryDate)
                .isUsed(false)
                .build();

        tokenRepository.save(token);

        // Send email
        sendResetPasswordEmail(user.getEmail(), resetToken);
        log.info("Password reset token sent to: {}", email);

        return AuthResponse.builder()
                .message("Link verifikasi telah dikirim ke email Anda. Silahkan cek email Anda dalam 1 jam.")
                .success(true)
                .build();
    }

    public AuthResponse resetPassword(String token, String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new RuntimeException("Password tidak cocok");
        }

        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token tidak valid"));

        if (resetToken.isExpired()) {
            throw new RuntimeException("Token sudah kadaluarsa. Silahkan request ulang.");
        }

        if (resetToken.getIsUsed()) {
            throw new RuntimeException("Token sudah digunakan");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(password));
        userRepository.save(user);

        resetToken.setIsUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successfully for: {}", user.getEmail());

        return AuthResponse.builder()
                .message("Password berhasil diubah")
                .success(true)
                .build();
    }

    public Boolean validateToken(String token) {
        try {
            return jwtTokenProvider.validateToken(token);
        } catch (Exception e) {
            log.error("Token validation error: {}", e.getMessage());
            return false;
        }
    }

    private void sendResetPasswordEmail(String email, String token) {
        try {
            String resetLink = frontendUrl + "/reset-password.html?token=" + token;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Reset Password - PMB HKBP Nommensen");
            message.setText("Halo,\n\n" +
                    "Kami menerima permintaan untuk reset password akun Anda. Silahkan klik link di bawah ini untuk membuat password baru:\n\n" +
                    resetLink + "\n\n" +
                    "Link ini berlaku selama 1 jam. Jika Anda tidak melakukan permintaan ini, abaikan email ini.\n\n" +
                    "Terima kasih,\nTim PMB HKBP Nommensen");

            mailSender.send(message);
            log.info("Reset password email sent to: {}", email);
            log.info("Reset Link: {}", resetLink);
        } catch (Exception e) {
            log.error("Failed to send reset password email to {}: {}", email, e.getMessage(), e);
            log.error("Mail config - host: {}, port: {}, username: {}, from: {}", 
                    mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl ? 
                    ((org.springframework.mail.javamail.JavaMailSenderImpl) mailSender).getHost() : "unknown",
                    mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl ? 
                    ((org.springframework.mail.javamail.JavaMailSenderImpl) mailSender).getPort() : "unknown",
                    mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl ? 
                    ((org.springframework.mail.javamail.JavaMailSenderImpl) mailSender).getUsername() : "unknown",
                    fromEmail);
            throw new RuntimeException("Gagal mengirim email. Silahkan coba lagi.");
        }
    }
}