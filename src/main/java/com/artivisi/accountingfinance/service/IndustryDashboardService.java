package com.artivisi.accountingfinance.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * IndustryDashboardService
 * KPI khusus untuk PT Grandindo Mitra Abadi (Industrial Supply & EPC Vendor)
 *
 * Menyediakan data untuk dashboard widget:
 * 1. Outstanding Quotation
 * 2. Outstanding PO Supplier
 * 3. Outstanding Invoice Customer
 * 4. Proyek Berjalan + Margin
 * 5. Cashflow 30 hari ke depan
 * 6. Aging Piutang
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IndustryDashboardService {

    private final JdbcTemplate jdbc;

    // ── 1. Sales Pipeline ─────────────────────────────────────

    public SalesPipelineKPI getSalesPipeline() {

        // Quotation outstanding (Draft + Sent)
        Long quotDraft = queryLong(
            "SELECT COUNT(*) FROM quotations WHERE status='DRAFT' AND deleted_at IS NULL");
        Long quotSent  = queryLong(
            "SELECT COUNT(*) FROM quotations WHERE status='SENT'  AND deleted_at IS NULL");
        BigDecimal quotSentValue = queryBD(
            "SELECT COALESCE(SUM(total_amount),0) FROM quotations WHERE status='SENT' AND deleted_at IS NULL");

        // RFQ open
        Long rfqOpen = queryLong(
            "SELECT COUNT(*) FROM rfq WHERE status='OPEN' AND deleted_at IS NULL");

        // SO belum fully delivered
        Long soActive = queryLong("""
            SELECT COUNT(*) FROM sales_orders
            WHERE status IN ('CONFIRMED','IN_PROGRESS','PARTIALLY_DELIVERED')
            AND deleted_at IS NULL""");
        BigDecimal soActiveValue = queryBD("""
            SELECT COALESCE(SUM(total_amount),0) FROM sales_orders
            WHERE status IN ('CONFIRMED','IN_PROGRESS','PARTIALLY_DELIVERED')
            AND deleted_at IS NULL""");

        return new SalesPipelineKPI(rfqOpen, quotDraft, quotSent, quotSentValue, soActive, soActiveValue);
    }

    // ── 2. Piutang / AR ───────────────────────────────────────

    public ArKPI getArKPI() {
        // Total outstanding invoice
        BigDecimal totalOutstanding = queryBD("""
            SELECT COALESCE(SUM(i.total_amount - COALESCE(paid.paid,0)), 0)
            FROM invoices i
            LEFT JOIN (
                SELECT id_invoice, SUM(amount) as paid
                FROM bill_payments GROUP BY id_invoice
            ) paid ON paid.id_invoice = i.id
            WHERE i.status NOT IN ('PAID','CANCELLED')
            AND i.deleted_at IS NULL""");

        // Sudah jatuh tempo
        BigDecimal overdue = queryBD("""
            SELECT COALESCE(SUM(i.total_amount - COALESCE(paid.paid,0)), 0)
            FROM invoices i
            LEFT JOIN (
                SELECT id_invoice, SUM(amount) as paid
                FROM bill_payments GROUP BY id_invoice
            ) paid ON paid.id_invoice = i.id
            WHERE i.status NOT IN ('PAID','CANCELLED')
            AND i.due_date < CURRENT_DATE
            AND i.deleted_at IS NULL""");

        // Aging buckets
        List<Map<String,Object>> aging = jdbc.queryForList("""
            SELECT
                CASE
                    WHEN due_date >= CURRENT_DATE           THEN 'Belum Jatuh Tempo'
                    WHEN due_date >= CURRENT_DATE - 30      THEN '1-30 Hari'
                    WHEN due_date >= CURRENT_DATE - 60      THEN '31-60 Hari'
                    WHEN due_date >= CURRENT_DATE - 90      THEN '61-90 Hari'
                    ELSE '> 90 Hari'
                END as bucket,
                COUNT(*) as count,
                COALESCE(SUM(i.total_amount - COALESCE(paid.paid,0)), 0) as amount
            FROM invoices i
            LEFT JOIN (
                SELECT id_invoice, SUM(amount) as paid
                FROM bill_payments GROUP BY id_invoice
            ) paid ON paid.id_invoice = i.id
            WHERE i.status NOT IN ('PAID','CANCELLED') AND i.deleted_at IS NULL
            GROUP BY 1
            ORDER BY MIN(due_date)
            """);

        // Top 5 piutang terbesar
        List<Map<String,Object>> top5 = jdbc.queryForList("""
            SELECT c.name as client_name, i.invoice_number,
                   i.due_date,
                   i.total_amount - COALESCE(paid.paid,0) as outstanding
            FROM invoices i
            JOIN clients c ON i.id_client = c.id
            LEFT JOIN (
                SELECT id_invoice, SUM(amount) as paid
                FROM bill_payments GROUP BY id_invoice
            ) paid ON paid.id_invoice = i.id
            WHERE i.status NOT IN ('PAID','CANCELLED') AND i.deleted_at IS NULL
            ORDER BY outstanding DESC LIMIT 5
            """);

        Long overdueCount = queryLong("""
            SELECT COUNT(*) FROM invoices
            WHERE status NOT IN ('PAID','CANCELLED')
            AND due_date < CURRENT_DATE
            AND deleted_at IS NULL""");

        return new ArKPI(totalOutstanding, overdue, overdueCount, aging, top5);
    }

    // ── 3. Hutang / AP ────────────────────────────────────────

    public ApKPI getApKPI() {
        // Outstanding PO (Sent + Confirmed + Partially Received)
        Long poOutstandingCount = queryLong("""
            SELECT COUNT(*) FROM purchase_orders
            WHERE status IN ('SENT','CONFIRMED','PARTIALLY_RECEIVED')
            AND deleted_at IS NULL""");
        BigDecimal poOutstandingValue = queryBD("""
            SELECT COALESCE(SUM(total_amount),0) FROM purchase_orders
            WHERE status IN ('SENT','CONFIRMED','PARTIALLY_RECEIVED')
            AND deleted_at IS NULL""");

        // Bill belum dibayar
        BigDecimal billOutstanding = queryBD("""
            SELECT COALESCE(SUM(b.total_amount - COALESCE(paid.paid,0)), 0)
            FROM bills b
            LEFT JOIN (
                SELECT id_bill, SUM(amount) as paid
                FROM bill_payments GROUP BY id_bill
            ) paid ON paid.id_bill = b.id
            WHERE b.status NOT IN ('PAID','CANCELLED')
            AND b.deleted_at IS NULL""");

        // Tagihan jatuh tempo 7 hari ke depan
        BigDecimal dueSoon = queryBD("""
            SELECT COALESCE(SUM(b.total_amount - COALESCE(paid.paid,0)), 0)
            FROM bills b
            LEFT JOIN (
                SELECT id_bill, SUM(amount) as paid
                FROM bill_payments GROUP BY id_bill
            ) paid ON paid.id_bill = b.id
            WHERE b.status NOT IN ('PAID','CANCELLED')
            AND b.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 7
            AND b.deleted_at IS NULL""");

        return new ApKPI(poOutstandingCount, poOutstandingValue, billOutstanding, dueSoon);
    }

    // ── 4. Cashflow 30 Hari ───────────────────────────────────

    public CashflowForecast getCashflowForecast() {
        // Cash saat ini
        BigDecimal cashNow = queryBD("""
            SELECT COALESCE(SUM(
                CASE WHEN je.normal_balance = 'DEBIT' THEN jel.debit_amount - jel.credit_amount
                     ELSE jel.credit_amount - jel.debit_amount END
            ), 0)
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.id_journal_entry = je.id
            JOIN chart_of_accounts coa ON jel.id_chart_of_account = coa.id
            WHERE coa.account_type = 'ASSET'
            AND LOWER(coa.account_name) LIKE '%kas%'
            OR LOWER(coa.account_name) LIKE '%bank%'
            """);

        // Invoice jatuh tempo 30 hari ke depan (expected inflow)
        List<Map<String,Object>> inflows = jdbc.queryForList("""
            SELECT
                due_date,
                SUM(i.total_amount - COALESCE(paid.paid,0)) as amount,
                COUNT(*) as count
            FROM invoices i
            LEFT JOIN (
                SELECT id_invoice, SUM(amount) as paid
                FROM bill_payments GROUP BY id_invoice
            ) paid ON paid.id_invoice = i.id
            WHERE i.status NOT IN ('PAID','CANCELLED')
            AND i.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 30
            AND i.deleted_at IS NULL
            GROUP BY due_date ORDER BY due_date
            """);

        // Bill jatuh tempo 30 hari ke depan (expected outflow)
        List<Map<String,Object>> outflows = jdbc.queryForList("""
            SELECT
                due_date,
                SUM(b.total_amount - COALESCE(paid.paid,0)) as amount,
                COUNT(*) as count
            FROM bills b
            LEFT JOIN (
                SELECT id_bill, SUM(amount) as paid
                FROM bill_payments GROUP BY id_bill
            ) paid ON paid.id_bill = b.id
            WHERE b.status NOT IN ('PAID','CANCELLED')
            AND b.due_date BETWEEN CURRENT_DATE AND CURRENT_DATE + 30
            AND b.deleted_at IS NULL
            GROUP BY due_date ORDER BY due_date
            """);

        BigDecimal totalInflow  = inflows.stream()
            .map(r -> toBD(r.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalOutflow = outflows.stream()
            .map(r -> toBD(r.get("amount"))).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal netCashflow  = totalInflow.subtract(totalOutflow);

        return new CashflowForecast(cashNow, totalInflow, totalOutflow, netCashflow, inflows, outflows);
    }

    // ── 5. Procurement Status ─────────────────────────────────

    public ProcurementKPI getProcurementKPI() {
        // PR menunggu persetujuan
        Long prPending = queryLong("""
            SELECT COUNT(*) FROM purchase_requests
            WHERE status = 'SUBMITTED' AND deleted_at IS NULL""");

        // PR disetujui belum jadi PO
        Long prApproved = queryLong("""
            SELECT COUNT(*) FROM purchase_requests
            WHERE status = 'APPROVED' AND deleted_at IS NULL""");

        // GR belum dikonfirmasi
        Long grPending = queryLong("""
            SELECT COUNT(*) FROM goods_receipts
            WHERE status = 'DRAFT' AND deleted_at IS NULL""");

        // PO terlambat (expected delivery sudah lewat)
        Long poOverdue = queryLong("""
            SELECT COUNT(*) FROM purchase_orders
            WHERE status IN ('SENT','CONFIRMED','PARTIALLY_RECEIVED')
            AND expected_delivery < CURRENT_DATE
            AND deleted_at IS NULL""");

        return new ProcurementKPI(prPending, prApproved, grPending, poOverdue);
    }

    // ── 6. Quick Stats ────────────────────────────────────────

    public QuickStats getQuickStats() {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        // Revenue bulan ini
        BigDecimal revenueThisMonth = queryBD("""
            SELECT COALESCE(SUM(jel.credit_amount - jel.debit_amount), 0)
            FROM journal_entry_lines jel
            JOIN journal_entries je ON jel.id_journal_entry = je.id
            JOIN chart_of_accounts coa ON jel.id_chart_of_account = coa.id
            WHERE coa.account_type = 'REVENUE'
            AND je.transaction_date >= ?
            """, monthStart);

        // Invoice baru bulan ini
        Long invoiceThisMonth = queryLong("""
            SELECT COUNT(*) FROM invoices
            WHERE invoice_date >= ? AND deleted_at IS NULL
            """, monthStart);

        // PO baru bulan ini
        Long poThisMonth = queryLong("""
            SELECT COUNT(*) FROM purchase_orders
            WHERE po_date >= ? AND deleted_at IS NULL
            """, monthStart);

        // Win rate quotation (bulan ini)
        Long quotWon  = queryLong("SELECT COUNT(*) FROM quotations WHERE status='WON' AND deleted_at IS NULL AND won_at >= ?", monthStart);
        Long quotLost = queryLong("SELECT COUNT(*) FROM quotations WHERE status='LOST' AND deleted_at IS NULL AND lost_at >= ?", monthStart);
        Long quotTotal = quotWon + quotLost;
        BigDecimal winRate = quotTotal > 0
            ? new BigDecimal(quotWon * 100).divide(new BigDecimal(quotTotal), 1, RoundingMode.HALF_UP)
            : BigDecimal.ZERO;

        return new QuickStats(revenueThisMonth, invoiceThisMonth, poThisMonth, winRate, quotWon, quotTotal);
    }

    // ── Private helpers ───────────────────────────────────────

    private Long queryLong(String sql, Object... args) {
        try {
            Long r = jdbc.queryForObject(sql, Long.class, args);
            return r != null ? r : 0L;
        } catch (Exception e) { return 0L; }
    }

    private BigDecimal queryBD(String sql, Object... args) {
        try {
            BigDecimal r = jdbc.queryForObject(sql, BigDecimal.class, args);
            return r != null ? r : BigDecimal.ZERO;
        } catch (Exception e) { return BigDecimal.ZERO; }
    }

    private BigDecimal toBD(Object val) {
        if (val == null) return BigDecimal.ZERO;
        if (val instanceof BigDecimal bd) return bd;
        return new BigDecimal(val.toString());
    }

    // ── Records ───────────────────────────────────────────────

    public record SalesPipelineKPI(
        long rfqOpenCount,
        long quotDraftCount,
        long quotSentCount,
        BigDecimal quotSentValue,
        long soActiveCount,
        BigDecimal soActiveValue
    ) {}

    public record ArKPI(
        BigDecimal totalOutstanding,
        BigDecimal overdueAmount,
        long overdueCount,
        List<Map<String,Object>> aging,
        List<Map<String,Object>> top5
    ) {}

    public record ApKPI(
        long poOutstandingCount,
        BigDecimal poOutstandingValue,
        BigDecimal billOutstanding,
        BigDecimal billDueSoon
    ) {}

    public record CashflowForecast(
        BigDecimal cashNow,
        BigDecimal expectedInflow,
        BigDecimal expectedOutflow,
        BigDecimal netCashflow,
        List<Map<String,Object>> inflows,
        List<Map<String,Object>> outflows
    ) {}

    public record ProcurementKPI(
        long prPendingApproval,
        long prApprovedNoPO,
        long grPendingConfirm,
        long poOverdue
    ) {}

    public record QuickStats(
        BigDecimal revenueThisMonth,
        long invoiceThisMonth,
        long poThisMonth,
        BigDecimal winRate,
        long quotWon,
        long quotTotal
    ) {}
}
