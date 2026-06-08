package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.IndustryDashboardService;
import com.artivisi.accountingfinance.service.ProjectDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/dashboard/industry")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.DASHBOARD_VIEW + "')")
public class IndustryDashboardController {

    private final IndustryDashboardService  industryService;
    private final ProjectDashboardService   projectService;

    // ── Widget: Sales Pipeline ────────────────────────────────

    @GetMapping("/sales-pipeline")
    public String salesPipeline(Model model) {
        model.addAttribute("pipeline", industryService.getSalesPipeline());
        return "fragments/industry/sales-pipeline :: widget";
    }

    // ── Widget: AR (Piutang) ──────────────────────────────────

    @GetMapping("/ar")
    public String ar(Model model) {
        model.addAttribute("ar", industryService.getArKPI());
        return "fragments/industry/ar-widget :: widget";
    }

    // ── Widget: AP (Hutang & PO) ──────────────────────────────

    @GetMapping("/ap")
    public String ap(Model model) {
        model.addAttribute("ap", industryService.getApKPI());
        return "fragments/industry/ap-widget :: widget";
    }

    // ── Widget: Cashflow 30 Hari ──────────────────────────────

    @GetMapping("/cashflow")
    public String cashflow(Model model) {
        model.addAttribute("cashflow", industryService.getCashflowForecast());
        return "fragments/industry/cashflow-widget :: widget";
    }

    // ── Widget: Procurement Status ────────────────────────────

    @GetMapping("/procurement")
    public String procurement(Model model) {
        model.addAttribute("proc", industryService.getProcurementKPI());
        return "fragments/industry/procurement-widget :: widget";
    }

    // ── Widget: Quick Stats ───────────────────────────────────

    @GetMapping("/quick-stats")
    public String quickStats(Model model) {
        model.addAttribute("stats", industryService.getQuickStats());
        return "fragments/industry/quick-stats :: widget";
    }

    // ── Widget: Active Projects ───────────────────────────────

    @GetMapping("/projects")
    public String projects(Model model) {
        model.addAttribute("projects", projectService.getActiveProjectCards());
        return "fragments/industry/projects-widget :: widget";
    }

    // ── Full Dashboard Page ───────────────────────────────────

    @GetMapping
    public String dashboard(Model model) {
        return "dashboard-industry";
    }
}
