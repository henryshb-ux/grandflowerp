package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.ProjectDashboardService;
import com.artivisi.accountingfinance.service.ProjectProfitabilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@Controller
@RequestMapping("/projects")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.PROJECT_VIEW + "')")
public class ProjectDashboardController {

    private final ProjectDashboardService    dashboardService;
    private final ProjectProfitabilityService profitabilityService;

    // ── Dashboard Portfolio ───────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("summary",  dashboardService.getPortfolioSummary());
        model.addAttribute("projects", dashboardService.getActiveProjectCards());
        return "projects/dashboard";
    }

    // ── Project Financial Detail ──────────────────────────────

    @GetMapping("/{id}/finance")
    public String projectFinance(@PathVariable UUID id, Model model) {
        model.addAttribute("report",  dashboardService.getProjectFinancialReport(id));
        return "projects/finance";
    }

    // ── Profitability Report (sudah ada di Balaka) ────────────

    @GetMapping("/{id}/profitability")
    public String profitability(
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {

        if (startDate == null) startDate = LocalDate.now().withDayOfYear(1);
        if (endDate   == null) endDate   = LocalDate.now();

        model.addAttribute("report",    profitabilityService.calculateProjectProfitability(id, startDate, endDate));
        model.addAttribute("overrun",   profitabilityService.calculateCostOverrun(id));
        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate",   endDate);
        return "projects/profitability";
    }
}
