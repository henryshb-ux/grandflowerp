package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.RfqStatus;
import com.artivisi.accountingfinance.repository.ClientRepository;
import com.artivisi.accountingfinance.repository.ProductRepository;
import com.artivisi.accountingfinance.repository.ProjectRepository;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.RfqService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/rfq")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class RfqController {

    private static final String VIEW_LIST   = "rfq/list";
    private static final String VIEW_FORM   = "rfq/form";
    private static final String VIEW_DETAIL = "rfq/detail";
    private static final String REDIRECT    = "redirect:/rfq/";

    private final RfqService        rfqService;
    private final ClientRepository  clientRepository;
    private final ProjectRepository projectRepository;
    private final ProductRepository productRepository;
    private final ObjectMapper      objectMapper;

    @GetMapping
    public String list(
            @RequestParam(required = false) RfqStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {
        Page<Rfq> page = rfqService.findByFilters(status, search, pageable);
        model.addAttribute("page",           page);
        model.addAttribute("statuses",       RfqStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("openCount",      rfqService.countByStatus(RfqStatus.OPEN));
        model.addAttribute("quotedCount",    rfqService.countByStatus(RfqStatus.QUOTED));
        return VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("rfq", rfqService.findById(id));
        return VIEW_DETAIL;
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(Model model) throws JsonProcessingException {
        RfqForm form = new RfqForm();
        form.setRfqDate(LocalDate.now());
        form.setResponseDate(LocalDate.now().plusDays(7));
        populateModel(model, form, List.of(), false, null);
        return VIEW_FORM;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(
            @ModelAttribute("form") RfqForm form,
            BindingResult br,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile file,
            RedirectAttributes ra,
            Model model) throws JsonProcessingException {
        if (br.hasErrors()) {
            populateModel(model, form, buildLinesJson(form), false, null);
            return VIEW_FORM;
        }
        Rfq saved = rfqService.create(formToEntity(form), file);
        ra.addFlashAttribute("successMessage",
            "RFQ " + saved.getRfqNumber() + " berhasil dicatat");
        return REDIRECT + saved.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String editForm(@PathVariable UUID id, Model model) throws JsonProcessingException {
        Rfq rfq = rfqService.findById(id);
        if (rfq.isCancelled()) return "redirect:/rfq/" + id;
        List<LineJson> lines = rfq.getLines().stream().map(l -> {
            LineJson lj = new LineJson();
            lj.setProductId(l.getProduct() != null ? l.getProduct().getId().toString() : "");
            lj.setProductLabel(l.getProduct() != null
                ? l.getProduct().getCode() + " — " + l.getProduct().getName() : "");
            lj.setDescription(l.getDescription());
            lj.setQuantity(l.getQuantity() != null ? l.getQuantity().doubleValue() : 1.0);
            lj.setUnit(l.getUnit() != null ? l.getUnit() : "");
            lj.setNotes(l.getNotes() != null ? l.getNotes() : "");
            return lj;
        }).toList();
        populateModel(model, entityToForm(rfq), lines, true, id);
        return VIEW_FORM;
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String update(
            @PathVariable UUID id,
            @ModelAttribute("form") RfqForm form,
            BindingResult br,
            @RequestParam(value = "attachmentFile", required = false) MultipartFile file,
            RedirectAttributes ra,
            Model model) throws JsonProcessingException {
        if (br.hasErrors()) {
            populateModel(model, form, buildLinesJson(form), true, id);
            return VIEW_FORM;
        }
        rfqService.update(id, formToEntity(form), file);
        ra.addFlashAttribute("successMessage", "RFQ berhasil diupdate");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        rfqService.cancel(id);
        ra.addFlashAttribute("successMessage", "RFQ berhasil dibatalkan");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/create-quotation")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createQuotation(@PathVariable UUID id) {
        return "redirect:/quotations/new?rfqId=" + id;
    }

    @GetMapping("/search")
    @ResponseBody
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> search(@RequestParam(value = "q", required = false) String q) {
        var page = rfqService.findByFilters(RfqStatus.OPEN, q == null ? "" : q,
            org.springframework.data.domain.PageRequest.of(0, 10));
        List<Map<String, Object>> results = new ArrayList<>();
        for (Rfq r : page.getContent()) {
            results.add(Map.of("id", r.getId().toString(),
                "number", r.getRfqNumber(),
                "client", r.getClient() != null ? r.getClient().getName() : ""));
        }
        return results;
    }

    private void populateModel(Model model, RfqForm form, List<LineJson> lines,
            boolean isEdit, UUID rfqId) throws JsonProcessingException {
        model.addAttribute("form",         form);
        model.addAttribute("isEdit",       isEdit);
        model.addAttribute("rfqId",        rfqId);
        model.addAttribute("initialLines", objectMapper.writeValueAsString(lines));
        model.addAttribute("projects",     projectRepository.findAll());
        String lbl = "";
        if (form.getClientId() != null)
            lbl = clientRepository.findById(form.getClientId())
                .map(c -> c.getCode() + " - " + c.getName()).orElse("");
        model.addAttribute("clientLabel", lbl);
    }

    private List<LineJson> buildLinesJson(RfqForm form) {
        if (form.getLines() == null) return List.of();
        return form.getLines().stream().map(lf -> {
            LineJson lj = new LineJson();
            lj.setProductId(lf.getProductId() != null ? lf.getProductId().toString() : "");
            lj.setDescription(lf.getDescription() != null ? lf.getDescription() : "");
            lj.setQuantity(lf.getQuantity() != null ? lf.getQuantity().doubleValue() : 1.0);
            lj.setUnit(lf.getUnit() != null ? lf.getUnit() : "");
            lj.setNotes(lf.getNotes() != null ? lf.getNotes() : "");
            return lj;
        }).toList();
    }

    private Rfq formToEntity(RfqForm form) {
        Rfq rfq = new Rfq();
        if (form.getClientId()  != null) clientRepository.findById(form.getClientId()).ifPresent(rfq::setClient);
        if (form.getProjectId() != null) projectRepository.findById(form.getProjectId()).ifPresent(rfq::setProject);
        rfq.setRfqDate(form.getRfqDate());
        rfq.setResponseDate(form.getResponseDate());
        rfq.setSubject(form.getSubject());
        rfq.setNotes(form.getNotes());
        if (form.getLines() != null) {
            int order = 1;
            for (RfqLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                RfqLine line = new RfqLine();
                line.setRfq(rfq);
                if (lf.getProductId() != null)
                    productRepository.findById(lf.getProductId()).ifPresent(line::setProduct);
                line.setLineOrder(order++);
                line.setDescription(lf.getDescription());
                line.setQuantity(lf.getQuantity() != null ? lf.getQuantity() : BigDecimal.ONE);
                line.setUnit(lf.getUnit());
                line.setNotes(lf.getNotes());
                rfq.getLines().add(line);
            }
        }
        return rfq;
    }

    private RfqForm entityToForm(Rfq rfq) {
        RfqForm form = new RfqForm();
        if (rfq.getClient()  != null) form.setClientId(rfq.getClient().getId());
        if (rfq.getProject() != null) form.setProjectId(rfq.getProject().getId());
        form.setRfqDate(rfq.getRfqDate());
        form.setResponseDate(rfq.getResponseDate());
        form.setSubject(rfq.getSubject());
        form.setNotes(rfq.getNotes());
        List<RfqLineForm> lines = new ArrayList<>();
        for (RfqLine l : rfq.getLines()) {
            RfqLineForm lf = new RfqLineForm();
            if (l.getProduct() != null) lf.setProductId(l.getProduct().getId());
            lf.setDescription(l.getDescription());
            lf.setQuantity(l.getQuantity());
            lf.setUnit(l.getUnit());
            lf.setNotes(l.getNotes());
            lines.add(lf);
        }
        form.setLines(lines);
        return form;
    }

    @Getter @Setter
    public static class RfqForm {
        @NotNull private UUID      clientId;
        private UUID               projectId;
        @NotNull private LocalDate rfqDate;
        private LocalDate          responseDate;
        private String             subject;
        private String             notes;
        private List<RfqLineForm>  lines = new ArrayList<>();
    }

    @Getter @Setter
    public static class RfqLineForm {
        private UUID        productId;
        @NotBlank private String      description;
        @NotNull  private BigDecimal  quantity;
        private String      unit;
        private String      notes;
    }

    @Getter @Setter
    public static class LineJson {
        private String productId    = "";
        private String productLabel = "";
        private String description  = "";
        private double quantity     = 1;
        private String unit         = "";
        private String notes        = "";
    }
}
