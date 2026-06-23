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

        // calculate opening balance from all lines BEFORE fromDate
        List<JournalLine> beforeLines =
                journalLineRepository.findByAccountIdAndJournalEntry_DateBefore(accountId, fromDate);

        BigDecimal openingBalance = calculateNetEffect(account, beforeLines);

        // get all lines WITHIN the date range, sorted by date
        List<JournalLine> rangeLines = journalLineRepository
                .findByAccountIdAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
                        accountId, fromDate, toDate);

        // walk through lines one by one, building running balance
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
        // Trial Balance reads current account balances.
        // as of Date is currently unused (reserved for historical reports).
        List<Account> accounts = accountRepository.findAll();

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;

        List<TrialBalanceRowDto> rows = new java.util.ArrayList<>();

        for (Account account : accounts) {

            BigDecimal balance = account.getCurrentBalance();

            //Get balance side based on account type.
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

        // Sort rows by account
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


    // Calculate net Balance effect of journal Line in an Account
    private BigDecimal calculateNetEffect(Account account, List<JournalLine> lines) {
        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            balance = applySingleLineEffect(account, balance, line);
        }
        return balance;
    }


//     Applies Single journal line debit/credit to a running balance (debit=credit),

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
