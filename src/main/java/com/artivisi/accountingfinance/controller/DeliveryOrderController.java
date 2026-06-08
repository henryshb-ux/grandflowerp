package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.DeliveryOrderStatus;
import com.artivisi.accountingfinance.repository.SalesOrderRepository;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.CompanyConfigService;
import com.artivisi.accountingfinance.service.DeliveryOrderService;
import com.artivisi.accountingfinance.service.ThemeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/delivery-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class DeliveryOrderController {

    private static final String VIEW_LIST    = "deliveryorder/list";
    private static final String VIEW_FORM    = "deliveryorder/form";
    private static final String VIEW_DETAIL  = "deliveryorder/detail";
    private static final String VIEW_PRINT   = "deliveryorder/print";
    private static final String VIEW_BAST    = "deliveryorder/print-bast";
    private static final String REDIRECT     = "redirect:/delivery-orders/";

    private final DeliveryOrderService    deliveryOrderService;
    private final SalesOrderRepository   salesOrderRepository;
    private final CompanyConfigService   companyConfigService;
    private final ThemeService           themeService;
    private final ObjectMapper           objectMapper;

    // ── List ──────────────────────────────────────────────────

    @GetMapping
    public String list(
            @RequestParam(required = false) DeliveryOrderStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Page<DeliveryOrder> page = deliveryOrderService.findByFilters(status, search, pageable);
        model.addAttribute("page",           page);
        model.addAttribute("statuses",       DeliveryOrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        deliveryOrderService.getSummary());
        return VIEW_LIST;
    }

    // ── Detail ────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("do_", deliveryOrderService.findById(id));
        return VIEW_DETAIL;
    }

    // ── New Form (prefill dari SO) ────────────────────────────

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(
            @RequestParam(required = false) UUID soId,
            Model model) throws JsonProcessingException {

        DoForm form = new DoForm();
        form.setDeliveryDate(LocalDate.now());
        List<LineJson> initialLines = new ArrayList<>();

        if (soId != null) {
            salesOrderRepository.findByIdWithLines(soId).ifPresent(so -> {
                form.setSoId(so.getId());
                form.setDeliveryAddress(so.getDeliveryAddress());
                // Prefill item yang belum selesai dikirim
                so.getLines().forEach(sol -> {
                    BigDecimal sisa = sol.getQtyRemaining();
                    if (sisa.compareTo(BigDecimal.ZERO) > 0) {
                        LineJson lj = new LineJson();
                        lj.setSoLineId(sol.getId().toString());
                        lj.setProductId(sol.getProduct() != null ? sol.getProduct().getId().toString() : "");
                        lj.setProductLabel(sol.getProduct() != null
                            ? sol.getProduct().getCode() + " — " + sol.getProduct().getName() : "");
                        lj.setDescription(sol.getDescription());
                        lj.setQuantity(sisa.doubleValue());
                        lj.setUnit(sol.getUnit() != null ? sol.getUnit() : "");
                        initialLines.add(lj);
                    }
                });
            });
        }

        populateModel(model, form, initialLines, false, null);
        return VIEW_FORM;
    }

    // ── Create ────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(
            @ModelAttribute("form") DoForm form,
            BindingResult br,
            RedirectAttributes ra,
            Model model) throws JsonProcessingException {

        if (br.hasErrors() || form.getSoId() == null) {
            populateModel(model, form, buildLinesJson(form), false, null);
            return VIEW_FORM;
        }

        DeliveryOrder doo = formToEntity(form);
        DeliveryOrder saved = deliveryOrderService.create(doo, form.getSoId());
        ra.addFlashAttribute("successMessage",
            "Delivery Order " + saved.getDoNumber() + " berhasil dibuat");
        return REDIRECT + saved.getId();
    }

    // ── Status transitions ────────────────────────────────────

    @PostMapping("/{id}/ship")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String ship(
            @PathVariable UUID id,
            @RequestParam(required = false) String shippedBy,
            @RequestParam(required = false) String trackingNo,
            RedirectAttributes ra) {
        deliveryOrderService.markShipped(id, shippedBy, trackingNo);
        ra.addFlashAttribute("successMessage", "Status diupdate: Dikirim");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String deliver(
            @PathVariable UUID id,
            @RequestParam(required = false) String receivedBy,
            RedirectAttributes ra) {
        deliveryOrderService.markDelivered(id, receivedBy);
        ra.addFlashAttribute("successMessage", "Status diupdate: Diterima");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/sign-bast")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String signBast(
            @PathVariable UUID id,
            @RequestParam String bastNumber,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bastDate,
            RedirectAttributes ra) {
        deliveryOrderService.signBast(id, bastNumber, bastDate);
        ra.addFlashAttribute("successMessage", "BAST " + bastNumber + " berhasil ditandatangani");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        deliveryOrderService.cancel(id);
        ra.addFlashAttribute("successMessage", "Delivery Order dibatalkan");
        return REDIRECT + id;
    }

    // ── Print Surat Jalan ─────────────────────────────────────

    @GetMapping("/{id}/print")
    public String printDo(@PathVariable UUID id, Model model) {
        DeliveryOrder doo = deliveryOrderService.findById(id);
        model.addAttribute("do_",     doo);
        model.addAttribute("company", companyConfigService.getConfig());
        model.addAttribute("theme",   themeService.getCurrentTheme());
        model.addAttribute("backUrl", "/delivery-orders/" + id);
        return VIEW_PRINT;
    }

    // ── Print BAST ────────────────────────────────────────────

    @GetMapping("/{id}/print-bast")
    public String printBast(@PathVariable UUID id, Model model) {
        DeliveryOrder doo = deliveryOrderService.findById(id);
        if (!doo.isBastSigned() && doo.getBastNumber() == null) {
            return "redirect:/delivery-orders/" + id + "?error=bast-not-signed";
        }
        model.addAttribute("do_",     doo);
        model.addAttribute("company", companyConfigService.getConfig());
        model.addAttribute("theme",   themeService.getCurrentTheme());
        model.addAttribute("backUrl", "/delivery-orders/" + id);
        return VIEW_BAST;
    }

    // ── Helpers ───────────────────────────────────────────────

    private void populateModel(Model model, DoForm form, List<LineJson> lines,
            boolean isEdit, UUID doId) throws JsonProcessingException {

        model.addAttribute("form",         form);
        model.addAttribute("isEdit",       isEdit);
        model.addAttribute("doId",         doId);
        model.addAttribute("initialLines", objectMapper.writeValueAsString(lines));

        // SO untuk pilih di form
        if (form.getSoId() != null) {
            salesOrderRepository.findByIdWithLines(form.getSoId())
                .ifPresent(so -> model.addAttribute("so", so));
        }
    }

    private List<LineJson> buildLinesJson(DoForm form) {
        if (form.getLines() == null) return List.of();
        return form.getLines().stream().map(lf -> {
            LineJson lj = new LineJson();
            lj.setSoLineId(lf.getSoLineId() != null ? lf.getSoLineId().toString() : "");
            lj.setProductId(lf.getProductId() != null ? lf.getProductId().toString() : "");
            lj.setDescription(lf.getDescription() != null ? lf.getDescription() : "");
            lj.setQuantity(lf.getQuantity() != null ? lf.getQuantity().doubleValue() : 0);
            lj.setUnit(lf.getUnit() != null ? lf.getUnit() : "");
            lj.setSerialNumbers(lf.getSerialNumbers() != null ? lf.getSerialNumbers() : "");
            lj.setNotes(lf.getNotes() != null ? lf.getNotes() : "");
            return lj;
        }).toList();
    }

    private DeliveryOrder formToEntity(DoForm form) {
        DeliveryOrder doo = new DeliveryOrder();
        doo.setDeliveryDate(form.getDeliveryDate());
        doo.setShippedBy(form.getShippedBy());
        doo.setTrackingNo(form.getTrackingNo());
        doo.setDeliveryAddress(form.getDeliveryAddress());
        doo.setNotes(form.getNotes());

        if (form.getLines() != null) {
            int order = 1;
            for (DoLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                DeliveryOrderLine line = new DeliveryOrderLine();
                line.setDeliveryOrder(doo);
                line.setLineOrder(order++);
                line.setDescription(lf.getDescription());
                line.setQuantity(lf.getQuantity() != null ? lf.getQuantity() : BigDecimal.ZERO);
                line.setUnit(lf.getUnit());
                line.setSerialNumbers(lf.getSerialNumbers());
                line.setNotes(lf.getNotes());

                // Link ke SO line
                if (lf.getSoLineId() != null) {
                    SalesOrderLine sol = new SalesOrderLine();
                    sol.setId(lf.getSoLineId());
                    line.setSoLine(sol);
                }
                doo.getLines().add(line);
            }
        }
        return doo;
    }

    // ── Form DTOs ─────────────────────────────────────────────

    @Getter @Setter
    public static class DoForm {
        private UUID              soId;
        private LocalDate         deliveryDate;
        private String            shippedBy;
        private String            trackingNo;
        private String            deliveryAddress;
        private String            notes;
        private List<DoLineForm>  lines = new ArrayList<>();
    }

    @Getter @Setter
    public static class DoLineForm {
        private UUID       soLineId;
        private UUID       productId;
        private String     description;
        private BigDecimal quantity;
        private String     unit;
        private String     serialNumbers;
        private String     notes;
    }

    @Getter @Setter
    public static class LineJson {
        private String soLineId      = "";
        private String productId     = "";
        private String productLabel  = "";
        private String description   = "";
        private double quantity      = 0;
        private String unit          = "";
        private String serialNumbers = "";
        private String notes         = "";
    }
}
