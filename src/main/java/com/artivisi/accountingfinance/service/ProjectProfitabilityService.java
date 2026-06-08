package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.ChartOfAccount;
import com.artivisi.accountingfinance.entity.Client;
import com.artivisi.accountingfinance.entity.Project;
import com.artivisi.accountingfinance.enums.AccountType;
import com.artivisi.accountingfinance.enums.NormalBalance;
import com.artivisi.accountingfinance.repository.ChartOfAccountRepository;
import com.artivisi.accountingfinance.repository.ClientRepository;
import com.artivisi.accountingfinance.repository.JournalEntryRepository;
import com.artivisi.accountingfinance.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectProfitabilityService {

    private final ProjectRepository projectRepository;
    private final ClientRepository clientRepository;
    private final ChartOfAccountRepository chartOfAccountRepository;
    private final JournalEntryRepository journalEntryRepository;

    public ProjectProfitabilityReport calculateProjectProfitability(UUID projectId, LocalDate startDate, LocalDate endDate) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        List<ChartOfAccount> revenueAccounts = chartOfAccountRepository
                .findByAccountTypeAndActiveOrderByAccountCodeAsc(AccountType.REVENUE, true);
        List<ChartOfAccount> expenseAccounts = chartOfAccountRepository
                .findByAccountTypeAndActiveOrderByAccountCodeAsc(AccountType.EXPENSE, true);

        List<ProfitabilityLineItem> revenueItems = new ArrayList<>();
        List<ProfitabilityLineItem> expenseItems = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        // Calculate revenue by project
        for (ChartOfAccount account : revenueAccounts) {
            if (account.getIsHeader()) continue;

            BigDecimal debit = journalEntryRepository.sumDebitByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);
            BigDecimal credit = journalEntryRepository.sumCreditByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);

            BigDecimal balance = account.getNormalBalance() == NormalBalance.CREDIT
                    ? credit.subtract(debit)
                    : debit.subtract(credit);

            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                revenueItems.add(new ProfitabilityLineItem(account, balance));
                totalRevenue = totalRevenue.add(balance);
            }
        }

        // Calculate expenses by project
        for (ChartOfAccount account : expenseAccounts) {
            if (account.getIsHeader()) continue;

            BigDecimal debit = journalEntryRepository.sumDebitByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);
            BigDecimal credit = journalEntryRepository.sumCreditByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);

            BigDecimal balance = account.getNormalBalance() == NormalBalance.DEBIT
                    ? debit.subtract(credit)
                    : credit.subtract(debit);

            if (balance.compareTo(BigDecimal.ZERO) != 0) {
                expenseItems.add(new ProfitabilityLineItem(account, balance));
                totalExpense = totalExpense.add(balance);
            }
        }

        BigDecimal grossProfit = totalRevenue.subtract(totalExpense);
        BigDecimal profitMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ProjectProfitabilityReport(
                project, startDate, endDate,
                revenueItems, expenseItems,
                totalRevenue, totalExpense,
                grossProfit, profitMargin
        );
    }

    public ClientProfitabilityReport calculateClientProfitability(UUID clientId, LocalDate startDate, LocalDate endDate) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new EntityNotFoundException("Client not found: " + clientId));

        List<Project> projects = projectRepository.findByClientId(clientId);
        List<ProjectProfitabilitySummary> projectSummaries = new ArrayList<>();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (Project project : projects) {
            ProjectProfitabilityReport report = calculateProjectProfitability(project.getId(), startDate, endDate);
            if (report.totalRevenue().compareTo(BigDecimal.ZERO) > 0 ||
                report.totalExpense().compareTo(BigDecimal.ZERO) > 0) {
                projectSummaries.add(new ProjectProfitabilitySummary(
                        project,
                        report.totalRevenue(),
                        report.grossProfit(),
                        report.profitMargin()
                ));
                totalRevenue = totalRevenue.add(report.totalRevenue());
                totalProfit = totalProfit.add(report.grossProfit());
            }
        }

        BigDecimal overallMargin = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.multiply(BigDecimal.valueOf(100)).divide(totalRevenue, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return new ClientProfitabilityReport(
                client, startDate, endDate,
                projectSummaries,
                totalRevenue, totalProfit, overallMargin
        );
    }

    public List<ClientRankingItem> getClientRanking(LocalDate startDate, LocalDate endDate, int limit) {
        List<Client> clients = clientRepository.findByActiveTrue();
        List<ClientRankingItem> rankings = new ArrayList<>();

        BigDecimal grandTotalRevenue = BigDecimal.ZERO;

        for (Client client : clients) {
            ClientProfitabilityReport report = calculateClientProfitability(client.getId(), startDate, endDate);
            if (report.totalRevenue().compareTo(BigDecimal.ZERO) > 0) {
                rankings.add(new ClientRankingItem(
                        client,
                        report.totalRevenue(),
                        report.totalProfit(),
                        report.overallMargin(),
                        BigDecimal.ZERO // Will calculate percentage later
                ));
                grandTotalRevenue = grandTotalRevenue.add(report.totalRevenue());
            }
        }

        // Sort by revenue descending
        rankings.sort(Comparator.comparing(ClientRankingItem::totalRevenue).reversed());

        // Calculate percentage of total revenue
        final BigDecimal finalGrandTotal = grandTotalRevenue;
        List<ClientRankingItem> finalRankings = new ArrayList<>();
        int rank = 1;
        for (ClientRankingItem item : rankings) {
            BigDecimal percentage = finalGrandTotal.compareTo(BigDecimal.ZERO) > 0
                    ? item.totalRevenue().multiply(BigDecimal.valueOf(100)).divide(finalGrandTotal, 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            finalRankings.add(new ClientRankingItem(
                    item.client(),
                    item.totalRevenue(),
                    item.totalProfit(),
                    item.profitMargin(),
                    percentage
            ));
            if (limit > 0 && rank++ >= limit) break;
        }

        return finalRankings;
    }

    public CostOverrunReport calculateCostOverrun(UUID projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("Project not found: " + projectId));

        BigDecimal budget = project.getBudgetAmount() != null ? project.getBudgetAmount() : BigDecimal.ZERO;
        BigDecimal contractValue = project.getContractValue() != null ? project.getContractValue() : BigDecimal.ZERO;

        // Calculate total spent (expenses) for this project
        List<ChartOfAccount> expenseAccounts = chartOfAccountRepository
                .findByAccountTypeAndActiveOrderByAccountCodeAsc(AccountType.EXPENSE, true);

        BigDecimal totalSpent = BigDecimal.ZERO;
        LocalDate startDate = LocalDate.of(1900, 1, 1);
        LocalDate endDate = LocalDate.now();

        for (ChartOfAccount account : expenseAccounts) {
            if (account.getIsHeader()) continue;

            BigDecimal debit = journalEntryRepository.sumDebitByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);
            BigDecimal credit = journalEntryRepository.sumCreditByProjectAndAccountAndDateRange(
                    projectId, account.getId(), startDate, endDate);

            BigDecimal balance = account.getNormalBalance() == NormalBalance.DEBIT
                    ? debit.subtract(credit)
                    : credit.subtract(debit);

            if (balance.compareTo(BigDecimal.ZERO) > 0) {
                totalSpent = totalSpent.add(balance);
            }
        }

        // Calculate progress from milestones
        int progress = project.getProgressPercent();

        // Calculate percentages
        BigDecimal budgetSpentPercent = budget.compareTo(BigDecimal.ZERO) > 0
                ? totalSpent.multiply(BigDecimal.valueOf(100)).divide(budget, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // Projected final cost: if progress > 0, project = (spent / progress%) * 100
        BigDecimal projectedFinalCost = progress > 0
                ? totalSpent.multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(progress), 2, RoundingMode.HALF_UP)
                : totalSpent;

        BigDecimal projectedOverrun = projectedFinalCost.subtract(budget);
        BigDecimal projectedLoss = projectedFinalCost.subtract(contractValue);

        // Risk level based on budget spent vs progress
        CostOverrunRisk riskLevel;
        if (budget.compareTo(BigDecimal.ZERO) == 0) {
            riskLevel = CostOverrunRisk.UNKNOWN;
        } else if (budgetSpentPercent.doubleValue() <= progress + 5) {
            riskLevel = CostOverrunRisk.LOW;
        } else if (budgetSpentPercent.doubleValue() <= progress + 15) {
            riskLevel = CostOverrunRisk.MEDIUM;
        } else {
            riskLevel = CostOverrunRisk.HIGH;
        }

        return new CostOverrunReport(
                project,
                budget,
                totalSpent,
                progress,
                budgetSpentPercent,
                projectedFinalCost,
                projectedOverrun,
                projectedLoss,
                riskLevel
        );
    }

    // DTOs
    public record ProjectProfitabilityReport(
            Project project,
            LocalDate startDate,
            LocalDate endDate,
            List<ProfitabilityLineItem> revenueItems,
            List<ProfitabilityLineItem> expenseItems,
            BigDecimal totalRevenue,
            BigDecimal totalExpense,
            BigDecimal grossProfit,
            BigDecimal profitMargin
    ) {}

    public record ProfitabilityLineItem(
            ChartOfAccount account,
            BigDecimal amount
    ) {}

    public record ClientProfitabilityReport(
            Client client,
            LocalDate startDate,
            LocalDate endDate,
            List<ProjectProfitabilitySummary> projects,
            BigDecimal totalRevenue,
            BigDecimal totalProfit,
            BigDecimal overallMargin
    ) {}

    public record ProjectProfitabilitySummary(
            Project project,
            BigDecimal revenue,
            BigDecimal profit,
            BigDecimal profitMargin
    ) {}

    public record ClientRankingItem(
            Client client,
            BigDecimal totalRevenue,
            BigDecimal totalProfit,
            BigDecimal profitMargin,
            BigDecimal revenuePercentage
    ) {}

    public record CostOverrunReport(
            Project project,
            BigDecimal budget,
            BigDecimal spent,
            int progressPercent,
            BigDecimal budgetSpentPercent,
            BigDecimal projectedFinalCost,
            BigDecimal projectedOverrun,
            BigDecimal projectedLoss,
            CostOverrunRisk riskLevel
    ) {}

    public enum CostOverrunRisk {
        LOW, MEDIUM, HIGH, UNKNOWN
    }
}
