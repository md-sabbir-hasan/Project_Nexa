package com.nexaerp.auth;

import com.nexaerp.auth.dto.LoginRequestDto;
import com.nexaerp.auth.dto.LoginResponseDto;
import com.nexaerp.auth.dto.RefreshTokenRequestDto;

public interface AuthService {
    // Authenticate user and return JWT tokens
    LoginResponseDto login(LoginRequestDto request, String ipAddress, String deviceName);

    // Generate new access token using refresh token
    LoginResponseDto refresh(RefreshTokenRequestDto request);

    // Revoke all refresh tokens for current user (logout)
    void logout(Long userId);
}
