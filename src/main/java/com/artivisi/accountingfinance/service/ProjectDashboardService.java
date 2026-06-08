package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.ProjectStatus;
import com.artivisi.accountingfinance.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ProjectDashboardService
 *
 * Menggabungkan data dari Sales (SO, Invoice), Purchasing (PO, GR),
 * dan journal entries untuk menghasilkan laporan profitabilitas
 * per proyek yang akurat untuk vendor industri seperti Grandindo.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectDashboardService {

    private final ProjectRepository          projectRepository;
    private final JdbcTemplate               jdbcTemplate;

    // ── Dashboard Summary ─────────────────────────────────────

    public ProjectPortfolioSummary getPortfolioSummary() {
        long activeCount    = projectRepository.countByStatus(ProjectStatus.ACTIVE);
        long completedCount = projectRepository.countByStatus(ProjectStatus.COMPLETED);

        // Aggregate dari database
        BigDecimal totalContractValue = sumBigDecimal(
            "SELECT COALESCE(SUM(contract_value), 0) FROM projects WHERE status = 'ACTIVE'");
        BigDecimal totalBudget = sumBigDecimal(
            "SELECT COALESCE(SUM(budget_amount), 0) FROM projects WHERE status = 'ACTIVE'");
        BigDecimal totalInvoiced = sumBigDecimal("""
            SELECT COALESCE(SUM(i.total_amount), 0)
            FROM invoices i
            JOIN projects p ON i.id_project = p.id
            WHERE p.status = 'ACTIVE' AND i.deleted_at IS NULL
            """);
        BigDecimal totalPoCost = sumBigDecimal("""
            SELECT COALESCE(SUM(po.total_amount), 0)
            FROM purchase_orders po
            JOIN projects p ON po.id_project = p.id
            WHERE p.status = 'ACTIVE'
              AND po.status NOT IN ('CANCELLED', 'DRAFT')
              AND po.deleted_at IS NULL
            """);
        BigDecimal totalCollected = sumBigDecimal("""
            SELECT COALESCE(SUM(bp.amount), 0)
            FROM bill_payments bp
            JOIN invoices i ON bp.id_invoice = i.id
            JOIN projects p ON i.id_project = p.id
            WHERE p.status = 'ACTIVE'
            """);

        return new ProjectPortfolioSummary(
            activeCount, completedCount,
            totalContractValue, totalBudget,
            totalInvoiced, totalPoCost,
            totalCollected
        );
    }

    // ── Per-Project Detail ────────────────────────────────────

    public ProjectFinancialReport getProjectFinancialReport(UUID projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new EntityNotFoundException("Project tidak ditemukan: " + projectId));

        // Revenue dari Invoice
        BigDecimal totalInvoiced = sumBigDecimal("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM invoices
            WHERE id_project = ? AND deleted_at IS NULL
            """, projectId);
        BigDecimal totalCollected = sumBigDecimal("""
            SELECT COALESCE(SUM(bp.amount), 0)
            FROM bill_payments bp
            JOIN invoices i ON bp.id_invoice = i.id
            WHERE i.id_project = ?
            """, projectId);
        BigDecimal outstandingReceivable = totalInvoiced.subtract(totalCollected);

        // Cost dari PO yang sudah confirmed/received
        BigDecimal totalPoCost = sumBigDecimal("""
            SELECT COALESCE(SUM(total_amount), 0)
            FROM purchase_orders
            WHERE id_project = ?
              AND status NOT IN ('DRAFT', 'CANCELLED')
              AND deleted_at IS NULL
            """, projectId);
        BigDecimal totalPoPaid = sumBigDecimal("""
            SELECT COALESCE(SUM(bp.amount), 0)
            FROM bill_payments bp
            JOIN bills b ON bp.id_bill = b.id
            JOIN purchase_orders po ON b.id_po = po.id
            WHERE po.id_project = ?
            """, projectId);
        BigDecimal outstandingPayable = totalPoCost.subtract(totalPoPaid);

        // Gross Profit
        BigDecimal grossProfit = totalInvoiced.subtract(totalPoCost);
        BigDecimal contractValue = project.getContractValue() != null
            ? project.getContractValue() : BigDecimal.ZERO;
        BigDecimal profitMargin = totalInvoiced.compareTo(BigDecimal.ZERO) > 0
            ? grossProfit.multiply(new BigDecimal("100"))
                .divide(totalInvoiced, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Budget vs actual
        BigDecimal budget = project.getBudgetAmount() != null
            ? project.getBudgetAmount() : BigDecimal.ZERO;
        BigDecimal budgetUsedPct = budget.compareTo(BigDecimal.ZERO) > 0
            ? totalPoCost.multiply(new BigDecimal("100"))
                .divide(budget, 2, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        // Sales Orders terkait
        List<Map<String, Object>> salesOrders = jdbcTemplate.queryForList("""
            SELECT so.so_number, so.total_amount, so.status, so.so_date,
                   c.name as client_name
            FROM sales_orders so
            JOIN clients c ON so.id_client = c.id
            WHERE so.id_project = ? AND so.deleted_at IS NULL
            ORDER BY so.so_date DESC
            """, projectId);

        // Purchase Orders terkait
        List<Map<String, Object>> purchaseOrders = jdbcTemplate.queryForList("""
            SELECT po.po_number, po.total_amount, po.status, po.po_date,
                   v.name as vendor_name
            FROM purchase_orders po
            JOIN vendors v ON po.id_vendor = v.id
            WHERE po.id_project = ? AND po.deleted_at IS NULL
            ORDER BY po.po_date DESC
            """, projectId);

        // Biaya per kategori
        List<Map<String, Object>> costByCategory = jdbcTemplate.queryForList("""
            SELECT
                'MATERIAL'      as category,
                COALESCE(SUM(pol.subtotal), 0) as amount
            FROM purchase_order_lines pol
            JOIN purchase_orders po ON pol.id_po = po.id
            WHERE po.id_project = ? AND po.status NOT IN ('DRAFT','CANCELLED')
            UNION ALL
            SELECT 'DIRECT_COST', COALESCE(SUM(total_cost), 0)
            FROM project_cost_entries
            WHERE id_project = ? AND cost_category = 'LABOR'
            UNION ALL
            SELECT 'SUBCON', COALESCE(SUM(total_cost), 0)
            FROM project_cost_entries
            WHERE id_project = ? AND cost_category = 'SUBCON'
            """, projectId, projectId, projectId);

        // Milestones
        List<Map<String, Object>> milestones = jdbcTemplate.queryForList("""
            SELECT name, weight_percent, status, target_date, actual_date
            FROM project_milestones
            WHERE id_project = ?
            ORDER BY sequence ASC
            """, projectId);

        // Invoice list
        List<Map<String, Object>> invoices = jdbcTemplate.queryForList("""
            SELECT invoice_number, invoice_date, total_amount, due_date, status
            FROM invoices
            WHERE id_project = ? AND deleted_at IS NULL
            ORDER BY invoice_date DESC
            """, projectId);

        return new ProjectFinancialReport(
            project,
            contractValue,
            budget,
            totalInvoiced,
            totalCollected,
            outstandingReceivable,
            totalPoCost,
            totalPoPaid,
            outstandingPayable,
            grossProfit,
            profitMargin,
            budgetUsedPct,
            salesOrders,
            purchaseOrders,
            costByCategory,
            milestones,
            invoices
        );
    }

    // ── Project List untuk Dashboard ──────────────────────────

    public List<ProjectCard> getActiveProjectCards() {
        List<Project> projects = projectRepository.findByStatus(ProjectStatus.ACTIVE);
        List<ProjectCard> cards = new ArrayList<>();

        for (Project p : projects) {
            BigDecimal invoiced = sumBigDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE id_project=? AND deleted_at IS NULL",
                p.getId());
            BigDecimal poCost = sumBigDecimal(
                "SELECT COALESCE(SUM(total_amount),0) FROM purchase_orders WHERE id_project=? AND status NOT IN ('DRAFT','CANCELLED') AND deleted_at IS NULL",
                p.getId());
            BigDecimal contractVal = p.getContractValue() != null ? p.getContractValue() : BigDecimal.ZERO;
            BigDecimal grossProfit = invoiced.subtract(poCost);
            BigDecimal margin = invoiced.compareTo(BigDecimal.ZERO) > 0
                ? grossProfit.multiply(new BigDecimal("100")).divide(invoiced, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
            int progress = p.getProgressPercent();

            // Risk level
            String riskLevel = "LOW";
            if (p.getEndDate() != null && LocalDate.now().isAfter(p.getEndDate())) {
                riskLevel = "HIGH";
            } else if (margin.compareTo(BigDecimal.ZERO) < 0) {
                riskLevel = "HIGH";
            } else if (margin.compareTo(new BigDecimal("10")) < 0) {
                riskLevel = "MEDIUM";
            }

            cards.add(new ProjectCard(
                p.getId(), p.getCode(), p.getName(),
                p.getClient() != null ? p.getClient().getName() : "—",
                contractVal, invoiced, poCost, grossProfit, margin,
                progress, p.getEndDate(), riskLevel
            ));
        }

        // Sort by contract value descending
        cards.sort((a, b) -> b.contractValue().compareTo(a.contractValue()));
        return cards;
    }

    // ── Helper ────────────────────────────────────────────────

    private BigDecimal sumBigDecimal(String sql, Object... args) {
        try {
            BigDecimal result = jdbcTemplate.queryForObject(sql, BigDecimal.class, args);
            return result != null ? result : BigDecimal.ZERO;
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    // ── Records / DTOs ────────────────────────────────────────

    public record ProjectPortfolioSummary(
        long activeProjectCount,
        long completedProjectCount,
        BigDecimal totalContractValue,
        BigDecimal totalBudget,
        BigDecimal totalInvoiced,
        BigDecimal totalPoCost,
        BigDecimal totalCollected
    ) {
        public BigDecimal estimatedGrossProfit() {
            return totalInvoiced.subtract(totalPoCost);
        }
        public BigDecimal outstandingReceivable() {
            return totalInvoiced.subtract(totalCollected);
        }
    }

    public record ProjectFinancialReport(
        Project project,
        BigDecimal contractValue,
        BigDecimal budget,
        BigDecimal totalInvoiced,
        BigDecimal totalCollected,
        BigDecimal outstandingReceivable,
        BigDecimal totalPoCost,
        BigDecimal totalPoPaid,
        BigDecimal outstandingPayable,
        BigDecimal grossProfit,
        BigDecimal profitMargin,
        BigDecimal budgetUsedPct,
        List<Map<String, Object>> salesOrders,
        List<Map<String, Object>> purchaseOrders,
        List<Map<String, Object>> costByCategory,
        List<Map<String, Object>> milestones,
        List<Map<String, Object>> invoices
    ) {
        public boolean isOverBudget() {
            return budget.compareTo(BigDecimal.ZERO) > 0
                && totalPoCost.compareTo(budget) > 0;
        }
        public BigDecimal remainingBudget() {
            return budget.subtract(totalPoCost);
        }
        public BigDecimal invoiceProgress() {
            return contractValue.compareTo(BigDecimal.ZERO) > 0
                ? totalInvoiced.multiply(new BigDecimal("100"))
                    .divide(contractValue, 1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        }
    }

    public record ProjectCard(
        UUID id,
        String code,
        String name,
        String clientName,
        BigDecimal contractValue,
        BigDecimal invoiced,
        BigDecimal poCost,
        BigDecimal grossProfit,
        BigDecimal profitMargin,
        int progressPercent,
        LocalDate endDate,
        String riskLevel   // LOW, MEDIUM, HIGH
    ) {
        public boolean isOverdue() {
            return endDate != null && LocalDate.now().isAfter(endDate);
        }
    }
}
