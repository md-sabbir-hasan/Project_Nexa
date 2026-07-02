package com.nexaerp.report;

import com.nexaerp.report.dto.BalanceSheetResponseDto;
import com.nexaerp.report.dto.LedgerResponseDto;
import com.nexaerp.report.dto.ProfitLossResponseDto;
import com.nexaerp.report.dto.TrialBalanceResponseDto;

import java.time.LocalDate;

public interface ReportService {
    LedgerResponseDto getLedger(Long accountId, LocalDate fromDate, LocalDate toDate);
    TrialBalanceResponseDto getTrialBalance(LocalDate asOfDate);

    ProfitLossResponseDto getProfitLoss(LocalDate fromDate, LocalDate toDate);

    BalanceSheetResponseDto getBalanceSheet(LocalDate asOfDate);
}
