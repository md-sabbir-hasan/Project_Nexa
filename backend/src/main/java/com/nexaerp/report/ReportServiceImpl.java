package com.nexaerp.report;


import com.nexaerp.account.Account;
import com.nexaerp.account.AccountRepository;
import com.nexaerp.account.AccountType;
import com.nexaerp.common.exception.ResourceNotFoundException;
import com.nexaerp.journal.JournalLine;
import com.nexaerp.journal.JournalLineRepository;
import com.nexaerp.report.dto.*;
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

    @Override
    public ProfitLossResponseDto getProfitLoss(LocalDate fromDate, LocalDate toDate) {
        List<Account> revenueAccounts = accountRepository.findByType(AccountType.REVENUE);
        List<Account> expenseAccounts = accountRepository.findByType(AccountType.EXPENSE);

        List<ProfitLossRowDto> revenues = new java.util.ArrayList<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Account account : revenueAccounts) {
            BigDecimal amount = calculatePeriodBalance(account, fromDate, toDate);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                revenues.add(ProfitLossRowDto.builder()
                        .accountId(account.getId())
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .amount(amount)
                        .build());
                totalRevenue = totalRevenue.add(amount);
            }
        }

        List<ProfitLossRowDto> expenses = new java.util.ArrayList<>();
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Account account : expenseAccounts) {
            BigDecimal amount = calculatePeriodBalance(account, fromDate, toDate);
            if (amount.compareTo(BigDecimal.ZERO) != 0) {
                expenses.add(ProfitLossRowDto.builder()
                        .accountId(account.getId())
                        .accountCode(account.getCode())
                        .accountName(account.getName())
                        .amount(amount)
                        .build());
                totalExpense = totalExpense.add(amount);
            }
        }
        return ProfitLossResponseDto.builder()
                .fromDate(fromDate)
                .toDate(toDate)
                .revenues(revenues)
                .totalRevenue(totalRevenue)
                .expenses(expenses)
                .totalExpense(totalExpense)
                .netProfit(totalRevenue.subtract(totalExpense))
                .build();
    }

    @Override
    public BalanceSheetResponseDto getBalanceSheet(LocalDate asOfDate) {
        List<Account> assetAccounts = accountRepository.findByType(AccountType.ASSET);
        List<Account> liabilityAccounts = accountRepository.findByType(AccountType.LIABILITY);
        List<Account> equityAccounts = accountRepository.findByType(AccountType.EQUITY);

        // Assets — only leaf accounts (no children) to avoid double counting
        List<BalanceSheetRowDto> assets = new java.util.ArrayList<>();
        BigDecimal totalAssets = BigDecimal.ZERO;

        for (Account account : assetAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    assets.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalAssets = totalAssets.add(balance);
                }
            }
        }

        // Liabilities
        List<BalanceSheetRowDto> liabilities = new java.util.ArrayList<>();
        BigDecimal totalLiabilities = BigDecimal.ZERO;

        for (Account account : liabilityAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    liabilities.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalLiabilities = totalLiabilities.add(balance);
                }
            }
        }

        // Equity (excluding current period profit, that's added separately)
        List<BalanceSheetRowDto> equity = new java.util.ArrayList<>();
        BigDecimal totalEquityExcludingProfit = BigDecimal.ZERO;

        for (Account account : equityAccounts) {
            if (account.getChildren() == null || account.getChildren().isEmpty()) {
                BigDecimal balance = account.getCurrentBalance();
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    equity.add(BalanceSheetRowDto.builder()
                            .accountId(account.getId())
                            .accountCode(account.getCode())
                            .accountName(account.getName())
                            .amount(balance)
                            .build());
                    totalEquityExcludingProfit = totalEquityExcludingProfit.add(balance);
                }
            }
        }

        // Net profit till date (Revenue - Expense), added into equity
        BigDecimal totalRevenue = accountRepository.findByType(AccountType.REVENUE)
                .stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalExpense = accountRepository.findByType(AccountType.EXPENSE)
                .stream()
                .map(Account::getCurrentBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal netProfit = totalRevenue.subtract(totalExpense);

        BigDecimal totalEquity = totalEquityExcludingProfit.add(netProfit);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(totalEquity);

        return BalanceSheetResponseDto.builder()
                .asOfDate(asOfDate)
                .assets(assets)
                .totalAssets(totalAssets)
                .liabilities(liabilities)
                .totalLiabilities(totalLiabilities)
                .equity(equity)
                .totalEquityExcludingProfit(totalEquityExcludingProfit)
                .netProfit(netProfit)
                .totalEquity(totalEquity)
                .totalLiabilitiesAndEquity(totalLiabilitiesAndEquity)
                .isBalanced(totalAssets.compareTo(totalLiabilitiesAndEquity) == 0)
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


    private BigDecimal calculatePeriodBalance(Account account, LocalDate fromDate, LocalDate toDate) {

        List<JournalLine> lines = journalLineRepository
                .findByAccountIdAndJournalEntry_DateBetweenOrderByJournalEntry_DateAsc(
                        account.getId(), fromDate, toDate);

        BigDecimal balance = BigDecimal.ZERO;
        for (JournalLine line : lines) {
            balance = applySingleLineEffect(account, balance, line);
        }
        return balance;
    }
}
