package com.nexaerp.auth;

import com.nexaerp.auth.dto.LoginRequestDto;
import com.nexaerp.auth.dto.LoginResponseDto;
import com.nexaerp.auth.dto.RefreshTokenRequestDto;
import com.nexaerp.common.exception.BusinessRuleException;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.security.JwtUtil;
import com.nexaerp.token.RefreshToken;
import com.nexaerp.token.RefreshTokenRepository;
import com.nexaerp.user.User;
import com.nexaerp.user.UserRepository;
import com.nexaerp.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService{

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;


    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${app.jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${app.security.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${app.security.lock-duration-minutes}")
    private int lockDurationMinutes;


    @Override
    public LoginResponseDto login(LoginRequestDto request, String ipAddress, String deviceName) {
        // find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessRuleException("Invalid email or password"));

        // Check lock
        if (user.getStatus() == UserStatus.LOCKED) {
            if (user.getLockedUntil() != null &&
                    LocalDateTime.now().isBefore(user.getLockedUntil())) {
                throw new BusinessRuleException(
                        "Account is locked. Try again after " + user.getLockedUntil());
            } else {
                user.setStatus(UserStatus.ACTIVE);
                user.setFailedLoginAttempts(0);
                user.setLockedUntil(null);
                userRepository.save(user);
            }
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessRuleException("Account is not active");
        }

        // Wrong password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {

            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            if (user.getFailedLoginAttempts() >= maxFailedAttempts) {
                user.setStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }

            userRepository.save(user); //

            if (user.getStatus() == UserStatus.LOCKED) {
                throw new BusinessRuleException(
                        "Account locked due to too many failed attempts. Try again after " +
                                lockDurationMinutes + " minutes");
            }

            throw new BusinessRuleException("Invalid email or password");
        }

        // Success
        user.setFailedLoginAttempts(0);
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // collect all permissions from all roles
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        //  generate access token
        String accessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), permissions);

        //  generate and save refresh token
        String refreshTokenValue = UUID.randomUUID().toString();

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(refreshTokenValue)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .revoked(false)
                .ipAddress(ipAddress)
                .deviceName(deviceName)
                .build();

        refreshTokenRepository.save(refreshToken);

        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public LoginResponseDto refresh(RefreshTokenRequestDto request) {
        // Find the refresh token in DB
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessRuleException("Invalid refresh token"));

        // Check if token is revoked
        if (refreshToken.getRevoked()) {
            throw new BusinessRuleException("Refresh token has been revoked");
        }

        // Check if token is expired
        if (LocalDateTime.now().isAfter(refreshToken.getExpiresAt())) {
            throw new BusinessRuleException("Refresh token has expired");
        }

        User user = refreshToken.getUser();

        // Collect permission
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(permission -> permission.getCode())
                .distinct()
                .collect(Collectors.toList());

        // Generate new access token
        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId(), user.getEmail(), permissions);

        return LoginResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken()) // same refresh token
                .expiresIn(accessTokenExpiration)
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        // Revoke all refresh tokens for this user
        refreshTokenRepository.deleteAllByUserId(userId);
    }
}
