package com.nexaerp.report;


import com.nexaerp.common.response.ApiResponse;
import com.nexaerp.report.dto.LedgerResponseDto;
import com.nexaerp.report.dto.TrialBalanceResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {
    private final ReportService reportService;

    @GetMapping("/ledger/{accountId}")
    public ResponseEntity<ApiResponse<LedgerResponseDto>> getLedger(
            @PathVariable Long accountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ApiResponse.success(
                reportService.getLedger(accountId, fromDate, toDate)));
    }

    @GetMapping("/trial-balance")
    public ResponseEntity<ApiResponse<TrialBalanceResponseDto>> getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        // If not provided, default to today
        LocalDate date = asOfDate != null ? asOfDate : LocalDate.now();

        return ResponseEntity.ok(ApiResponse.success(
                reportService.getTrialBalance(date)));
    }
}
