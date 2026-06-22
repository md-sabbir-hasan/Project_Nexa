package com.nexaerp.report;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.report.dto.LedgerEntryDto;
import com.nexaerp.report.dto.LedgerResponseDto;
import com.nexaerp.report.dto.TrialBalanceResponseDto;
import com.nexaerp.report.dto.TrialBalanceRowDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService{

    private final AccountRepository accountRepository;
    private final JournalLineRepository journalLineRepository;



    @Override
    public LedgerResponseDto getLedger(Long accountId, LocalDate fromDate, LocalDate toDate) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Step 1 — calculate opening balance from all lines BEFORE fromDate
        List<JournalLine> beforeLines =
                journalLineRepository.findByAccountIdAndJournalEntry_DateBefore(accountId, fromDate);

        BigDecimal openingBalance = calculateNetEffect(account, beforeLines);

        // Step 2 — get all lines WITHIN the date range, sorted by date
        List<JournalLine> rangeLines = journalLineRepository
                .findByAccountIdAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
                        accountId, fromDate, toDate);

        // Step 3 — walk through lines one by one, building running balance
        BigDecimal runningBalance = openingBalance;
        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<LedgerEntryDto> entries = new java.util.ArrayList<>();

        for (JournalLine line : rangeLines) {

            // Debit/Credit affect balance differently depending on account type
            runningBalance = applySingleLineEffect(account, runningBalance, line);

            totalDebit = totalDebit.add(line.getDebit());
            totalCredit = totalCredit.add(line.getCredit());

            entries.add(LedgerEntryDto.builder()
                    .journalEntryId(line.getJournalEntry().getId())
                    .date(line.getJournalEntry().getDate())
                    .journalEntryNumber(line.getJournalEntry().getEntryNumber())
                    .sourceType(line.getJournalEntry().getSourceType())
                    .sourceId(line.getJournalEntry().getSourceId())
                    .referenceNumber(line.getJournalEntry().getReferenceNumber())
                    .description(line.getDescription())
                    .debit(line.getDebit())
                    .credit(line.getCredit())
                    .runningBalance(runningBalance)
                    .build());
        }

        return LedgerResponseDto.builder()
                .accountId(account.getId())
                .accountCode(account.getCode())
                .accountName(account.getName())
                .accountType(account.getType())
                .fromDate(fromDate)
                .toDate(toDate)
                .openingBalance(openingBalance)
                .closingBalance(runningBalance)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .entries(entries)
                .build();
    }

    @Override
    public TrialBalanceResponseDto getTrialBalance(LocalDate asOfDate) {
        // Trial Balance simply reads the current stored balance of every account.
        // Note: this reflects the balance AS OF NOW, since we don't keep historical
        // snapshots — asOfDate is accepted for future use (e.g. historical reports).
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<TrialBalanceRowDto> rows = new java.util.ArrayList<>();

        for (Account account : accounts) {

            BigDecimal balance = account.getCurrentBalance();

            // Determine which side (debit or credit) this account's balance sits on,
            // based on its type's natural balance.
            BigDecimal debitBalance = BigDecimal.ZERO;
            BigDecimal creditBalance = BigDecimal.ZERO;

            boolean isNaturallyDebit =
                    account.getType() == AccountType.ASSET ||
                            account.getType() == AccountType.EXPENSE;

            if (isNaturallyDebit) {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    debitBalance = balance;
                } else {
                    // Negative balance on a debit-natured account shows on credit side
                    creditBalance = balance.abs();
                }
            } else {
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    creditBalance = balance;
                } else {
                    debitBalance = balance.abs();
                }
            }

            totalDebit = totalDebit.add(debitBalance);
            totalCredit = totalCredit.add(creditBalance);

            rows.add(TrialBalanceRowDto.builder()
                    .accountId(account.getId())
                    .accountCode(account.getCode())
                    .accountName(account.getName())
                    .accountType(account.getType())
                    .debitBalance(debitBalance)
                    .creditBalance(creditBalance)
                    .build());
        }

        // Sort rows by account code for readability
        rows.sort(Comparator.comparing(TrialBalanceRowDto::getAccountCode));

        return TrialBalanceResponseDto.builder()
                .asOfDate(asOfDate)
                .rows(rows)
                .totalDebit(totalDebit)
                .totalCredit(totalCredit)
                .isBalanced(totalDebit.compareTo(totalCredit) == 0)
                .build();
    }




                            // ---Privet-----Helper--------



//     Calculates the net balance effect of a list of journal lines on an account,
//     based on the account's type (used for opening balance calculation).

    private BigDecimal calculateNetEffect(Account account, List<JournalLine> lines) {
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            balance = applySingleLineEffect(account, balance, line);
        }
        return balance;
    }


//     Applies one journal line's debit/credit to a running balance,
//     following the same Debit/Credit rule used everywhere else in the system.

    private BigDecimal applySingleLineEffect(Account account, BigDecimal currentBalance, JournalLine line) {
        switch (account.getType()) {
            case ASSET:
            case EXPENSE:
                return currentBalance.add(line.getDebit()).subtract(line.getCredit());
            case LIABILITY:
            case EQUITY:
            case REVENUE:
                return currentBalance.add(line.getCredit()).subtract(line.getDebit());
            default:
                return currentBalance;
        }
    }
}
