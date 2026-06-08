package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.QuotationStatus;
import com.artivisi.accountingfinance.repository.*;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.QuotationService;
import com.artivisi.accountingfinance.service.SalesOrderService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
@RequestMapping("/quotations")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class QuotationController {

    private static final String VIEW_LIST   = "quotation/list";
    private static final String VIEW_FORM   = "quotation/form";
    private static final String VIEW_DETAIL = "quotation/detail";
    private static final String REDIRECT    = "redirect:/quotations/";

    private final QuotationService   quotationService;
    private final SalesOrderService  salesOrderService;
    private final ClientRepository   clientRepository;
    private final ProjectRepository  projectRepository;
    private final RfqRepository      rfqRepository;
    private final ProductRepository  productRepository;
    private final ObjectMapper       objectMapper;

    // ── List ──────────────────────────────────────────────────

    @GetMapping
    public String list(
            @RequestParam(required = false) QuotationStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        Page<Quotation> page = quotationService.findByFilters(status, search, pageable);
        model.addAttribute("page",           page);
        model.addAttribute("statuses",       QuotationStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        quotationService.getSummary());
        return VIEW_LIST;
    }

    // ── Detail ────────────────────────────────────────────────

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("quotation", quotationService.findById(id));
        return VIEW_DETAIL;
    }

    // ── New Form ──────────────────────────────────────────────

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(
            @RequestParam(required = false) UUID rfqId,
            Model model) throws JsonProcessingException {

        QuotationForm form = new QuotationForm();
        form.setQuotationDate(LocalDate.now());
        form.setValidUntil(LocalDate.now().plusDays(30));
        form.setCurrency("IDR");

        List<LineJson> initialLines = new ArrayList<>();

        // Prefill dari RFQ jika ada
        if (rfqId != null) {
            rfqRepository.findByIdWithLines(rfqId).ifPresent(rfq -> {
                form.setRfqId(rfq.getId());
                form.setClientId(rfq.getClient().getId());
                form.setSubject(rfq.getSubject());
                rfq.getLines().forEach(rl -> {
                    LineJson lj = new LineJson();
                    lj.setRfqLineId(rl.getId() != null ? rl.getId().toString() : "");
                    lj.setDescription(rl.getDescription());
                    lj.setQuantity(rl.getQuantity() != null ? rl.getQuantity().doubleValue() : 1.0);
                    lj.setUnit(rl.getUnit());
                    lj.setUnitPrice(0.0);
                    lj.setDiscountPct(0.0);
                    lj.setTaxPct(11.0);
                    initialLines.add(lj);
                });
            });
        }

        populateFormModel(model, form, initialLines, false, null);
        return VIEW_FORM;
    }

    // ── Create ────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(
            @ModelAttribute("form") QuotationForm form,
            BindingResult br,
            @RequestParam(value = "_action", defaultValue = "draft") String action,
            RedirectAttributes ra,
            Model model) throws JsonProcessingException {

        if (br.hasErrors()) {
            populateFormModel(model, form, buildLinesJson(form), false, null);
            return VIEW_FORM;
        }

        Quotation q = formToEntity(form);
        Quotation saved = quotationService.create(q);

        // Kirim langsung jika user klik "Simpan & Kirim"
        if ("send".equals(action)) {
            quotationService.send(saved.getId());
            ra.addFlashAttribute("successMessage",
                "Quotation " + saved.getQuotationNumber() + " berhasil dibuat dan dikirim ke customer");
        } else {
            ra.addFlashAttribute("successMessage",
                "Quotation " + saved.getQuotationNumber() + " berhasil disimpan sebagai draft");
        }
        return REDIRECT + saved.getId();
    }

    // ── Edit Form ─────────────────────────────────────────────

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String editForm(@PathVariable UUID id, Model model) throws JsonProcessingException {
        Quotation q = quotationService.findById(id);
        if (!q.isDraft()) {
            return "redirect:/quotations/" + id + "?error=only-draft-editable";
        }

        QuotationForm form = entityToForm(q);
        List<LineJson> initialLines = q.getLines().stream().map(l -> {
            LineJson lj = new LineJson();
            lj.setProductId(l.getProduct() != null ? l.getProduct().getId().toString() : "");
            lj.setProductLabel(l.getProduct() != null
                ? l.getProduct().getCode() + " — " + l.getProduct().getName() : "");
            lj.setDescription(l.getDescription());
            lj.setQuantity(l.getQuantity() != null ? l.getQuantity().doubleValue() : 1.0);
            lj.setUnit(l.getUnit());
            lj.setUnitPrice(l.getUnitPrice() != null ? l.getUnitPrice().doubleValue() : 0.0);
            lj.setDiscountPct(l.getDiscountPct() != null ? l.getDiscountPct().doubleValue() : 0.0);
            lj.setTaxPct(l.getTaxPct() != null ? l.getTaxPct().doubleValue() : 11.0);
            lj.setNotes(l.getNotes());
            return lj;
        }).toList();

        populateFormModel(model, form, initialLines, true, id);
        return VIEW_FORM;
    }

    // ── Update ────────────────────────────────────────────────

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String update(
            @PathVariable UUID id,
            @ModelAttribute("form") QuotationForm form,
            BindingResult br,
            @RequestParam(value = "_action", defaultValue = "draft") String action,
            RedirectAttributes ra,
            Model model) throws JsonProcessingException {

        if (br.hasErrors()) {
            populateFormModel(model, form, buildLinesJson(form), true, id);
            return VIEW_FORM;
        }

        Quotation updated = formToEntity(form);
        quotationService.update(id, updated);

        if ("send".equals(action)) {
            quotationService.send(id);
            ra.addFlashAttribute("successMessage", "Quotation berhasil diupdate dan dikirim ke customer");
        } else {
            ra.addFlashAttribute("successMessage", "Quotation berhasil diupdate");
        }
        return REDIRECT + id;
    }

    // ── Status Transitions ────────────────────────────────────

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_SEND + "')")
    public String send(@PathVariable UUID id, RedirectAttributes ra) {
        Quotation q = quotationService.send(id);
        ra.addFlashAttribute("successMessage",
            "Quotation " + q.getQuotationNumber() + " berhasil dikirim ke customer");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/won")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String markWon(@PathVariable UUID id, RedirectAttributes ra) {
        quotationService.markWon(id);
        ra.addFlashAttribute("successMessage",
            "Quotation ditandai MENANG. Silakan buat Sales Order.");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/lost")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String markLost(
            @PathVariable UUID id,
            @RequestParam(required = false) String reason,
            RedirectAttributes ra) {
        quotationService.markLost(id, reason);
        ra.addFlashAttribute("successMessage", "Quotation ditandai KALAH.");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/convert-to-so")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String convertToSo(
            @PathVariable UUID id,
            @RequestParam(required = false) String poNumberCustomer,
            RedirectAttributes ra) {
        SalesOrder so = salesOrderService.createFromQuotation(id, poNumberCustomer);
        ra.addFlashAttribute("successMessage",
            "Sales Order " + so.getSoNumber() + " berhasil dibuat dari quotation ini");
        return "redirect:/sales-orders/" + so.getId();
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String delete(@PathVariable UUID id, RedirectAttributes ra) {
        quotationService.delete(id);
        ra.addFlashAttribute("successMessage", "Quotation berhasil dihapus");
        return "redirect:/quotations";
    }

    // ── Typeahead search (AJAX) ───────────────────────────────

    @GetMapping("/search")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> search(
            @RequestParam(value = "q", required = false) String q) {
        var page = quotationService.findByFilters(
            QuotationStatus.WON, q == null ? "" : q,
            org.springframework.data.domain.PageRequest.of(0, 10));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Quotation qt : page.getContent()) {
            results.add(Map.of(
                "id",     qt.getId().toString(),
                "number", qt.getQuotationNumber(),
                "client", qt.getClient() != null ? qt.getClient().getName() : "",
                "total",  qt.getTotalAmount()
            ));
        }
        return results;
    }

    // ── Private Helpers ───────────────────────────────────────

    private void populateFormModel(
            Model model,
            QuotationForm form,
            List<LineJson> initialLines,
            boolean isEdit,
            UUID quotationId) throws JsonProcessingException {

        model.addAttribute("form",                form);
        model.addAttribute("isEdit",              isEdit);
        model.addAttribute("quotationId",         quotationId);
        model.addAttribute("initialLines",        objectMapper.writeValueAsString(initialLines));
        model.addAttribute("rfqList",             rfqRepository.findAllActive(
                                                      org.springframework.data.domain.PageRequest.of(0, 100))
                                                      .getContent());
        model.addAttribute("projects",            projectRepository.findAll());
        model.addAttribute("paymentTermOptions",  List.of(
            "NET 14", "NET 30", "NET 45", "NET 60", "NET 90",
            "DP 30% NET 30", "DP 50% NET 30", "DP 30% NET 60", "Cash Before Delivery"));
        model.addAttribute("deliveryTermOptions", List.of(
            "DDP (Delivered Duty Paid)",
            "DAP (Delivered at Place)",
            "FOB Surabaya",
            "FOB Jakarta",
            "EXW (Ex-Works Gudang Kami)",
            "CIF"));

        // Label untuk client picker (pre-fill)
        String clientLabel = "";
        if (form.getClientId() != null) {
            clientLabel = clientRepository.findById(form.getClientId())
                .map(c -> c.getCode() + " - " + c.getName())
                .orElse("");
        }
        model.addAttribute("clientLabel", clientLabel);
    }

    private List<LineJson> buildLinesJson(QuotationForm form) {
        if (form.getLines() == null) return List.of();
        return form.getLines().stream().map(lf -> {
            LineJson lj = new LineJson();
            lj.setProductId(lf.getProductId() != null ? lf.getProductId().toString() : "");
            lj.setDescription(lf.getDescription());
            lj.setQuantity(lf.getQuantity() != null ? lf.getQuantity().doubleValue() : 1.0);
            lj.setUnit(lf.getUnit());
            lj.setUnitPrice(lf.getUnitPrice() != null ? lf.getUnitPrice().doubleValue() : 0.0);
            lj.setDiscountPct(lf.getDiscountPct() != null ? lf.getDiscountPct().doubleValue() : 0.0);
            lj.setTaxPct(lf.getTaxPct() != null ? lf.getTaxPct().doubleValue() : 11.0);
            lj.setNotes(lf.getNotes());
            return lj;
        }).toList();
    }

    private Quotation formToEntity(QuotationForm form) {
        Quotation q = new Quotation();
        if (form.getRfqId()     != null) rfqRepository.findById(form.getRfqId()).ifPresent(q::setRfq);
        if (form.getClientId()  != null) clientRepository.findById(form.getClientId()).ifPresent(q::setClient);
        if (form.getProjectId() != null) projectRepository.findById(form.getProjectId()).ifPresent(q::setProject);

        q.setQuotationDate(form.getQuotationDate());
        q.setValidUntil(form.getValidUntil());
        q.setSubject(form.getSubject());
        q.setPaymentTerms(form.getPaymentTerms());
        q.setDeliveryTerms(form.getDeliveryTerms());
        q.setDeliveryDays(form.getDeliveryDays());
        q.setCurrency(form.getCurrency() != null ? form.getCurrency() : "IDR");
        q.setDiscountAmount(form.getDiscountAmount() != null ? form.getDiscountAmount() : BigDecimal.ZERO);
        q.setNotes(form.getNotes());
        q.setInternalNotes(form.getInternalNotes());

        if (form.getLines() != null) {
            int order = 1;
            for (QuotationLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                QuotationLine line = new QuotationLine();
                line.setQuotation(q);
                if (lf.getProductId() != null)
                    productRepository.findById(lf.getProductId()).ifPresent(line::setProduct);
                line.setLineOrder(order++);
                line.setDescription(lf.getDescription());
                line.setQuantity(lf.getQuantity() != null ? lf.getQuantity() : BigDecimal.ONE);
                line.setUnit(lf.getUnit());
                line.setUnitPrice(lf.getUnitPrice() != null ? lf.getUnitPrice() : BigDecimal.ZERO);
                line.setDiscountPct(lf.getDiscountPct() != null ? lf.getDiscountPct() : BigDecimal.ZERO);
                line.setTaxPct(lf.getTaxPct() != null ? lf.getTaxPct() : new BigDecimal("11"));
                line.setNotes(lf.getNotes());
                q.getLines().add(line);
            }
        }
        return q;
    }

    private QuotationForm entityToForm(Quotation q) {
        QuotationForm form = new QuotationForm();
        if (q.getRfq()     != null) form.setRfqId(q.getRfq().getId());
        if (q.getClient()  != null) form.setClientId(q.getClient().getId());
        if (q.getProject() != null) form.setProjectId(q.getProject().getId());
        form.setQuotationDate(q.getQuotationDate());
        form.setValidUntil(q.getValidUntil());
        form.setSubject(q.getSubject());
        form.setPaymentTerms(q.getPaymentTerms());
        form.setDeliveryTerms(q.getDeliveryTerms());
        form.setDeliveryDays(q.getDeliveryDays());
        form.setCurrency(q.getCurrency());
        form.setDiscountAmount(q.getDiscountAmount());
        form.setNotes(q.getNotes());
        form.setInternalNotes(q.getInternalNotes());

        List<QuotationLineForm> lineForms = new ArrayList<>();
        for (QuotationLine l : q.getLines()) {
            QuotationLineForm lf = new QuotationLineForm();
            if (l.getProduct() != null) lf.setProductId(l.getProduct().getId());
            lf.setDescription(l.getDescription());
            lf.setQuantity(l.getQuantity());
            lf.setUnit(l.getUnit());
            lf.setUnitPrice(l.getUnitPrice());
            lf.setDiscountPct(l.getDiscountPct());
            lf.setTaxPct(l.getTaxPct());
            lf.setNotes(l.getNotes());
            lineForms.add(lf);
        }
        form.setLines(lineForms);
        return form;
    }

    // ── Form DTOs ─────────────────────────────────────────────

    @Getter @Setter
    public static class QuotationForm {
        private UUID        rfqId;
        private UUID        clientId;
        private UUID        projectId;
        private LocalDate   quotationDate;
        private LocalDate   validUntil;
        private String      subject;
        private String      paymentTerms;
        private String      deliveryTerms;
        private Integer     deliveryDays;
        private String      currency = "IDR";
        private BigDecimal  discountAmount;
        private String      notes;
        private String      internalNotes;
        private List<QuotationLineForm> lines = new ArrayList<>();
    }

    @Getter @Setter
    public static class QuotationLineForm {
        private UUID        productId;
        private String      description;
        private BigDecimal  quantity;
        private String      unit;
        private BigDecimal  unitPrice;
        private BigDecimal  discountPct;
        private BigDecimal  taxPct = new BigDecimal("11");
        private String      notes;
    }

    /** DTO untuk serialisasi initial lines ke JSON (Alpine.js) */
    @Getter @Setter
    public static class LineJson {
        private String  productId    = "";
        private String  productLabel = "";
        private String  rfqLineId    = "";
        private String  description  = "";
        private double  quantity     = 1;
        private String  unit         = "pcs";
        private double  unitPrice    = 0;
        private double  discountPct  = 0;
        private double  taxPct       = 11;
        private String  notes        = "";
    }
}
