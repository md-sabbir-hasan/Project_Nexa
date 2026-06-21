package com.nexaerp.report;

import com.nexaerp.report.dto.LedgerResponseDto;
import com.nexaerp.report.dto.TrialBalanceResponseDto;

import java.time.LocalDate;

public interface ReportService {
    LedgerResponseDto getLedger(Long accountId, LocalDate fromDate, LocalDate toDate);
    TrialBalanceResponseDto getTrialBalanceResponseDto(LocalDate asOfDate);
}
