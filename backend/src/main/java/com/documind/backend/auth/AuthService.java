package com.documind.backend.auth;

import com.documind.backend.audit.AuditLogService;
import com.documind.backend.auth.dto.*;
import com.documind.backend.common.exception.ConflictException;
import com.documind.backend.user.User;
import com.documind.backend.user.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditLogService auditLogService;

    public AuthService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("An account with this email already exists.");
        }

        User user = new User(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.displayName().trim()
        );
        user = userRepository.save(user);

        auditLogService.record(null, user.getId(), "USER_REGISTERED", "USER", user.getId(),
                Map.of("email", user.getEmail()));

        return issueTokenPair(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password."));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password.");
        }

        auditLogService.record(null, user.getId(), "USER_LOGGED_IN", "USER", user.getId(),
                Map.of("email", user.getEmail()));

        return issueTokenPair(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken().trim());
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token."));

        Instant now = Instant.now();

        if (existing.isRevoked()) {
            // Token reuse detected: revoke entire family
            refreshTokenRepository.revokeFamily(existing.getFamilyId(), now);
            throw new BadCredentialsException("Refresh token has already been revoked.");
        }

        if (existing.isExpired()) {
            existing.setRevokedAt(now);
            refreshTokenRepository.save(existing);
            throw new BadCredentialsException("Refresh token has expired.");
        }

        User user = existing.getUser();
        String newRawRefresh = jwtService.generateOpaqueRefreshToken();
        RefreshToken replacement = new RefreshToken(
                UUID.randomUUID(),
                user,
                existing.getFamilyId(),
                jwtService.hashToken(newRawRefresh),
                jwtService.getRefreshTokenExpiry()
        );
        replacement = refreshTokenRepository.save(replacement);

        existing.setRevokedAt(now);
        existing.setReplacedByTokenId(replacement.getId());
        refreshTokenRepository.save(existing);

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(
                accessToken,
                newRawRefresh,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                UserProfileResponse.from(user)
        );
    }

    @Transactional
    public void logout(UUID currentUserId, RefreshTokenRequest request) {
        String tokenHash = jwtService.hashToken(request.refreshToken().trim());
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            if (token.getUser().getId().equals(currentUserId) && !token.isRevoked()) {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
                auditLogService.record(null, currentUserId, "USER_LOGGED_OUT", "USER", currentUserId, Map.of());
            }
        });
    }

    private AuthResponse issueTokenPair(User user, UUID familyId) {
        String rawRefresh = jwtService.generateOpaqueRefreshToken();
        RefreshToken refreshToken = new RefreshToken(
                UUID.randomUUID(),
                user,
                familyId,
                jwtService.hashToken(rawRefresh),
                jwtService.getRefreshTokenExpiry()
        );
        refreshTokenRepository.save(refreshToken);

        String accessToken = jwtService.generateAccessToken(user);
        return new AuthResponse(
                accessToken,
                rawRefresh,
                "Bearer",
                jwtService.getAccessTokenExpiresInSeconds(),
                UserProfileResponse.from(user)
        );
    }
}
