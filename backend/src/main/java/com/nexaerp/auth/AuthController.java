package com.nexaerp.auth;


import com.nexaerp.auth.dto.LoginRequestDto;
import com.nexaerp.auth.dto.LoginResponseDto;
import com.nexaerp.auth.dto.RefreshTokenRequestDto;
import com.nexaerp.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {


    private final AuthService authService;


    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request,
            HttpServletRequest httpRequest) {

        String ipAddress = httpRequest.getRemoteAddr();
        String deviceName = httpRequest.getHeader("User-Agent");

        return ResponseEntity.ok(ApiResponse.success("Login successful",
                authService.login(request, ipAddress, deviceName)));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponseDto>> refresh(
            @Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(ApiResponse.success("Token refreshed",
                authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal String email) {
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }
}
