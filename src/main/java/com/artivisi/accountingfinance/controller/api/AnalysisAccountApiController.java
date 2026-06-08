package com.artivisi.accountingfinance.controller.api;

import com.artivisi.accountingfinance.entity.ChartOfAccount;
import com.artivisi.accountingfinance.enums.AuditEventType;
import com.artivisi.accountingfinance.repository.ChartOfAccountRepository;
import com.artivisi.accountingfinance.service.JournalEntryService;
import com.artivisi.accountingfinance.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analysis API for chart of accounts and account ledger data.
 */
@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Financial Analysis", description = "Read-only financial data for AI analysis (reports, snapshots, ledgers)")
@PreAuthorize("hasAuthority('SCOPE_analysis:read')")
@RequiredArgsConstructor
public class AnalysisAccountApiController {

    private static final String META_CURRENCY = "currency";
    private static final String META_CURRENCY_IDR = "IDR";
    private static final String META_DESCRIPTION = "description";
    private static final String PARAM_START_DATE = "startDate";
    private static final String PARAM_END_DATE = "endDate";

    private final ChartOfAccountRepository chartOfAccountRepository;
    private final JournalEntryService journalEntryService;
    private final SecurityAuditService securityAuditService;

    @GetMapping("/accounts")
    public ResponseEntity<AnalysisResponse<AccountsDto>> getAccounts() {

        List<ChartOfAccount> accounts = chartOfAccountRepository.findAllTransactableAccounts();

        List<AccountDto> items = accounts.stream()
                .map(a -> new AccountDto(
                        a.getId(), a.getAccountCode(), a.getAccountName(),
                        a.getAccountType().name(), a.getNormalBalance().name()))
                .toList();

        AccountsDto data = new AccountsDto(items);

        auditAccess("accounts", Map.of());

        return ResponseEntity.ok(new AnalysisResponse<>(
                "accounts", LocalDateTime.now(),
                Map.of(),
                data,
                Map.of(META_DESCRIPTION, "Chart of accounts (leaf/transactable accounts only). "
                        + "normalBalance indicates whether the account normally carries a DEBIT or CREDIT balance.")));
    }

    @GetMapping("/accounts/{id}/ledger")
    @Transactional(readOnly = true)
    public ResponseEntity<AnalysisResponse<AccountLedgerDto>> getAccountLedger(
            @PathVariable UUID id,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        JournalEntryService.GeneralLedgerData ledger = journalEntryService.getGeneralLedger(id, start, end);
        ChartOfAccount account = ledger.account();

        List<LedgerEntryDto> entries = ledger.entries().stream()
                .map(item -> new LedgerEntryDto(
                        item.entry().getJournalDate(),
                        item.entry().getTransaction().getId(),
                        item.entry().getJournalNumber(),
                        item.entry().getTransaction().getDescription(),
                        item.entry().getDebitAmount(),
                        item.entry().getCreditAmount(),
                        item.runningBalance()))
                .toList();

        AccountLedgerDto data = new AccountLedgerDto(
                account.getAccountCode(),
                account.getAccountName(),
                account.getAccountType().name(),
                account.getNormalBalance().name(),
                ledger.openingBalance(),
                ledger.totalDebit(),
                ledger.totalCredit(),
                ledger.closingBalance(),
                entries);

        auditAccess("account-ledger", Map.of("accountId", id.toString(),
                PARAM_START_DATE, startDate, PARAM_END_DATE, endDate));

        return ResponseEntity.ok(new AnalysisResponse<>(
                "account-ledger", LocalDateTime.now(),
                Map.of("accountId", id.toString(), PARAM_START_DATE, startDate, PARAM_END_DATE, endDate),
                data,
                Map.of(META_CURRENCY, META_CURRENCY_IDR,
                        META_DESCRIPTION, "Account ledger for " + account.getAccountCode()
                                + " " + account.getAccountName()
                                + " from " + startDate + " to " + endDate
                                + ". Running balance reflects account's normal balance convention.")));
    }

    private void auditAccess(String reportType, Map<String, String> params) {
        securityAuditService.logAsync(AuditEventType.API_CALL,
                "Analysis API: " + reportType + " " + params);
    }

    // --- DTOs ---

    public record AccountsDto(
            List<AccountDto> accounts
    ) {}

    public record AccountDto(
            UUID id, String code, String name,
            String type, String normalBalance
    ) {}

    public record AccountLedgerDto(
            String accountCode, String accountName, String accountType, String normalBalance,
            BigDecimal openingBalance, BigDecimal totalDebit, BigDecimal totalCredit, BigDecimal closingBalance,
            List<LedgerEntryDto> entries
    ) {}

    public record LedgerEntryDto(
            LocalDate transactionDate, UUID transactionId, String journalNumber,
            String description, BigDecimal debitAmount, BigDecimal creditAmount,
            BigDecimal runningBalance
    ) {}
}
