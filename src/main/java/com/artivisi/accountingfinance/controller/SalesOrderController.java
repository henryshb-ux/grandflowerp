package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.SalesOrderStatus;
import com.artivisi.accountingfinance.repository.ClientRepository;
import com.artivisi.accountingfinance.repository.ProjectRepository;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.SalesOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;

@Controller
@RequestMapping("/sales-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class SalesOrderController {

    private static final String VIEW_LIST   = "salesorder/list";
    private static final String VIEW_DETAIL = "salesorder/detail";
    private static final String REDIRECT    = "redirect:/sales-orders/";

    private final SalesOrderService salesOrderService;

    // ── List ──────────────────────────────────────────────────

    @GetMapping
    public String list(
            @RequestParam(required = false) SalesOrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Page<SalesOrder> page = salesOrderService.findByFilters(status, search, pageable);
        model.addAttribute("page",           page);
        model.addAttribute("statuses",       SalesOrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        salesOrderService.getSummary());
        return VIEW_LIST;
    }

    // ── Detail ────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        SalesOrder so = salesOrderService.findById(id);
        model.addAttribute("so",          so);
        model.addAttribute("doList",      salesOrderService.findDeliveryOrders(id));
        model.addAttribute("invoiceList", salesOrderService.findInvoices(id));
        return VIEW_DETAIL;
    }

    // ── Cancel ────────────────────────────────────────────────

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        salesOrderService.cancel(id);
        ra.addFlashAttribute("successMessage", "Sales Order berhasil dibatalkan");
        return REDIRECT + id;
    }

    // ── Buat Delivery Order dari SO ───────────────────────────

    @PostMapping("/{id}/create-do")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createDo(@PathVariable UUID id) {
        return "redirect:/delivery-orders/new?soId=" + id;
    }

    // ── Buat Invoice dari SO ──────────────────────────────────

    @PostMapping("/{id}/create-invoice")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createInvoice(@PathVariable UUID id) {
        return "redirect:/invoices/new?soId=" + id;
    }

    // ── Typeahead search (AJAX) ───────────────────────────────

    @GetMapping("/search")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> search(
            @RequestParam(value = "q", required = false) String q) {
        var page = salesOrderService.findByFilters(
            null, q == null ? "" : q,
            org.springframework.data.domain.PageRequest.of(0, 10));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SalesOrder so : page.getContent()) {
            out.add(Map.of(
                "id",     so.getId().toString(),
                "number", so.getSoNumber(),
                "client", so.getClient() != null ? so.getClient().getName() : "",
                "total",  so.getTotalAmount()
            ));
        }
        return out;
    }
}
