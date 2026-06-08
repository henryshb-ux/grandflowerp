// ── PurchaseRequestController.java ──────────────────────────────────────────
package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.*;
import com.artivisi.accountingfinance.repository.*;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.PurchaseRequestService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/purchase-requests")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class PurchaseRequestController {

    private static final String VIEW_LIST   = "purchaserequest/list";
    private static final String VIEW_FORM   = "purchaserequest/form";
    private static final String VIEW_DETAIL = "purchaserequest/detail";
    private static final String REDIRECT    = "redirect:/purchase-requests/";

    private final PurchaseRequestService prService;
    private final ProjectRepository      projectRepository;
    private final ProductRepository      productRepository;
    private final ObjectMapper           objectMapper;

    @GetMapping
    public String list(@RequestParam(required = false) PurchaseRequestStatus status,
                       @RequestParam(required = false) String search,
                       @PageableDefault(size = 20) Pageable pageable,
                       Model model) {
        model.addAttribute("page",           prService.findByFilters(status, search, pageable));
        model.addAttribute("statuses",       PurchaseRequestStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        prService.getSummary());
        return VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("pr", prService.findById(id));
        return VIEW_DETAIL;
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(Model model) throws JsonProcessingException {
        PrForm form = new PrForm();
        form.setRequestDate(LocalDate.now());
        form.setRequiredDate(LocalDate.now().plusDays(14));
        form.setPriority(PurchaseRequestPriority.NORMAL);
        populateModel(model, form, List.of(), false, null);
        return VIEW_FORM;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(@ModelAttribute("form") PrForm form,
                         RedirectAttributes ra,
                         Model model) throws JsonProcessingException {
        PurchaseRequest pr = formToEntity(form);
        PurchaseRequest saved = prService.create(pr);
        ra.addFlashAttribute("successMessage", "PR " + saved.getPrNumber() + " berhasil dibuat");
        return REDIRECT + saved.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String editForm(@PathVariable UUID id, Model model) throws JsonProcessingException {
        PurchaseRequest pr = prService.findById(id);
        List<LineJson> lines = pr.getLines().stream().map(l -> {
            LineJson lj = new LineJson();
            lj.setProductId(l.getProduct() != null ? l.getProduct().getId().toString() : "");
            lj.setProductLabel(l.getProduct() != null ? l.getProduct().getCode() + " — " + l.getProduct().getName() : "");
            lj.setDescription(l.getDescription());
            lj.setQuantity(l.getQuantity() != null ? l.getQuantity().doubleValue() : 1.0);
            lj.setUnit(l.getUnit() != null ? l.getUnit() : "");
            lj.setEstimatedPrice(l.getEstimatedPrice() != null ? l.getEstimatedPrice().doubleValue() : 0.0);
            lj.setNotes(l.getNotes() != null ? l.getNotes() : "");
            return lj;
        }).toList();
        populateModel(model, entityToForm(pr), lines, true, id);
        return VIEW_FORM;
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String update(@PathVariable UUID id, @ModelAttribute("form") PrForm form,
                         RedirectAttributes ra, Model model) throws JsonProcessingException {
        prService.update(id, formToEntity(form));
        ra.addFlashAttribute("successMessage", "PR berhasil diupdate");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable UUID id, RedirectAttributes ra) {
        prService.submit(id);
        ra.addFlashAttribute("successMessage", "PR berhasil diajukan untuk persetujuan");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String approve(@PathVariable UUID id,
                          @RequestParam(required = false) String approvedBy,
                          RedirectAttributes ra) {
        prService.approve(id, approvedBy);
        ra.addFlashAttribute("successMessage", "PR disetujui. Silakan buat Purchase Order.");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String reject(@PathVariable UUID id,
                         @RequestParam(required = false) String reason,
                         RedirectAttributes ra) {
        prService.reject(id, reason != null ? reason : "");
        ra.addFlashAttribute("successMessage", "PR ditolak.");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/create-po")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createPo(@PathVariable UUID id) {
        return "redirect:/purchase-orders/new?prId=" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        prService.cancel(id);
        ra.addFlashAttribute("successMessage", "PR dibatalkan");
        return REDIRECT + id;
    }

    private void populateModel(Model model, PrForm form, List<LineJson> lines,
                               boolean isEdit, UUID prId) throws JsonProcessingException {
        model.addAttribute("form",         form);
        model.addAttribute("isEdit",       isEdit);
        model.addAttribute("prId",         prId);
        model.addAttribute("initialLines", objectMapper.writeValueAsString(lines));
        model.addAttribute("projects",     projectRepository.findAll());
        model.addAttribute("priorities",   PurchaseRequestPriority.values());
    }

    private PurchaseRequest formToEntity(PrForm form) {
        PurchaseRequest pr = new PurchaseRequest();
        if (form.getProjectId() != null)
            projectRepository.findById(form.getProjectId()).ifPresent(pr::setProject);
        pr.setRequestDate(form.getRequestDate());
        pr.setRequiredDate(form.getRequiredDate());
        pr.setPriority(form.getPriority() != null ? form.getPriority() : PurchaseRequestPriority.NORMAL);
        pr.setRequestedBy(form.getRequestedBy());
        pr.setSubject(form.getSubject());
        pr.setNotes(form.getNotes());
        if (form.getLines() != null) {
            int order = 1;
            for (PrLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                PurchaseRequestLine line = new PurchaseRequestLine();
                line.setPurchaseRequest(pr);
                if (lf.getProductId() != null)
                    productRepository.findById(lf.getProductId()).ifPresent(line::setProduct);
                line.setLineOrder(order++);
                line.setDescription(lf.getDescription());
                line.setQuantity(lf.getQuantity() != null ? lf.getQuantity() : BigDecimal.ONE);
                line.setUnit(lf.getUnit());
                line.setEstimatedPrice(lf.getEstimatedPrice());
                line.setNotes(lf.getNotes());
                pr.getLines().add(line);
            }
        }
        return pr;
    }

    private PrForm entityToForm(PurchaseRequest pr) {
        PrForm form = new PrForm();
        if (pr.getProject() != null) form.setProjectId(pr.getProject().getId());
        form.setRequestDate(pr.getRequestDate());
        form.setRequiredDate(pr.getRequiredDate());
        form.setPriority(pr.getPriority());
        form.setRequestedBy(pr.getRequestedBy());
        form.setSubject(pr.getSubject());
        form.setNotes(pr.getNotes());
        List<PrLineForm> lines = new ArrayList<>();
        for (PurchaseRequestLine l : pr.getLines()) {
            PrLineForm lf = new PrLineForm();
            if (l.getProduct() != null) lf.setProductId(l.getProduct().getId());
            lf.setDescription(l.getDescription());
            lf.setQuantity(l.getQuantity());
            lf.setUnit(l.getUnit());
            lf.setEstimatedPrice(l.getEstimatedPrice());
            lf.setNotes(l.getNotes());
            lines.add(lf);
        }
        form.setLines(lines);
        return form;
    }

    @Getter @Setter
    public static class PrForm {
        private UUID                      projectId;
        private LocalDate                 requestDate;
        private LocalDate                 requiredDate;
        private PurchaseRequestPriority   priority;
        private String                    requestedBy;
        private String                    subject;
        private String                    notes;
        private List<PrLineForm>          lines = new ArrayList<>();
    }

    @Getter @Setter
    public static class PrLineForm {
        private UUID       productId;
        private String     description;
        private BigDecimal quantity;
        private String     unit;
        private BigDecimal estimatedPrice;
        private String     notes;
    }

    @Getter @Setter
    public static class LineJson {
        private String productId     = "";
        private String productLabel  = "";
        private String description   = "";
        private double quantity      = 1;
        private String unit          = "";
        private double estimatedPrice = 0;
        private String notes         = "";
    }
}
