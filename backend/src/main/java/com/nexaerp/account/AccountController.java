package com.nexaerp.account;


import com.nexaerp.account.dto.AccountRequestDto;
import com.nexaerp.account.dto.AccountResponseDto;
import com.nexaerp.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor

public class AccountController {

    private final AccountService  accountService;

    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponseDto>> create(
            @Valid @RequestBody AccountRequestDto request) {
        AccountResponseDto response = accountService.create(request);
        return ResponseEntity.ok(ApiResponse.success("Account created", response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountResponseDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getAll()));
    }

    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getTree() {
        return ResponseEntity.ok(ApiResponse.success(accountService.getTree()));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<AccountResponseDto>>> getByType(
            @PathVariable AccountType type) {
        return ResponseEntity.ok(ApiResponse.success(accountService.getByType(type)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable Long id) {
        accountService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Account deactivated", null));
    }

}
