// ── PurchaseOrderController.java ─────────────────────────────────────────────
package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.PurchaseOrderStatus;
import com.artivisi.accountingfinance.repository.*;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.*;
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
@RequestMapping("/purchase-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class PurchaseOrderController {

    private static final String VIEW_LIST   = "purchaseorder/list";
    private static final String VIEW_FORM   = "purchaseorder/form";
    private static final String VIEW_DETAIL = "purchaseorder/detail";
    private static final String VIEW_PRINT  = "purchaseorder/print";
    private static final String REDIRECT    = "redirect:/purchase-orders/";

    private final PurchaseOrderService   poService;
    private final PurchaseRequestService prService;
    private final VendorRepository       vendorRepository;
    private final ProjectRepository      projectRepository;
    private final ProductRepository      productRepository;
    private final CompanyConfigService   companyConfigService;
    private final ThemeService           themeService;
    private final ObjectMapper           objectMapper;

    @GetMapping
    public String list(@RequestParam(required = false) PurchaseOrderStatus status,
                       @RequestParam(required = false) String search,
                       @PageableDefault(size = 20) Pageable pageable,
                       Model model) {
        model.addAttribute("page",           poService.findByFilters(status, search, pageable));
        model.addAttribute("statuses",       PurchaseOrderStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        poService.getSummary());
        return VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        PurchaseOrder po = poService.findById(id);
        model.addAttribute("po",     po);
        model.addAttribute("grList", poService.findGoodsReceipts(id));
        return VIEW_DETAIL;
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(@RequestParam(required = false) UUID prId,
                          Model model) throws JsonProcessingException {
        PoForm form = new PoForm();
        form.setPoDate(LocalDate.now());
        List<LineJson> initialLines = new ArrayList<>();

        if (prId != null) {
            var pr = prService.findById(prId);
            form.setPrId(pr.getId());
            if (pr.getProject() != null) form.setProjectId(pr.getProject().getId());
            pr.getLines().forEach(prl -> {
                LineJson lj = new LineJson();
                lj.setPrLineId(prl.getId().toString());
                lj.setDescription(prl.getDescription());
                lj.setQuantity(prl.getQuantity() != null ? prl.getQuantity().doubleValue() : 1.0);
                lj.setUnit(prl.getUnit() != null ? prl.getUnit() : "");
                lj.setUnitPrice(prl.getEstimatedPrice() != null ? prl.getEstimatedPrice().doubleValue() : 0.0);
                lj.setTaxPct(11.0);
                initialLines.add(lj);
            });
        }
        populateModel(model, form, initialLines, false, null);
        return VIEW_FORM;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(@ModelAttribute("form") PoForm form,
                         @RequestParam(value = "_action", defaultValue = "draft") String action,
                         RedirectAttributes ra, Model model) throws JsonProcessingException {
        PurchaseOrder po = formToEntity(form);
        PurchaseOrder saved = poService.create(po);
        if ("send".equals(action)) poService.send(saved.getId());
        ra.addFlashAttribute("successMessage", "PO " + saved.getPoNumber() + " berhasil dibuat");
        return REDIRECT + saved.getId();
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String editForm(@PathVariable UUID id, Model model) throws JsonProcessingException {
        PurchaseOrder po = poService.findById(id);
        if (!po.isDraft()) return "redirect:/purchase-orders/" + id;
        List<LineJson> lines = po.getLines().stream().map(l -> {
            LineJson lj = new LineJson();
            lj.setDescription(l.getDescription());
            lj.setQuantity(l.getQuantity() != null ? l.getQuantity().doubleValue() : 1.0);
            lj.setUnit(l.getUnit() != null ? l.getUnit() : "");
            lj.setUnitPrice(l.getUnitPrice() != null ? l.getUnitPrice().doubleValue() : 0.0);
            lj.setDiscountPct(l.getDiscountPct() != null ? l.getDiscountPct().doubleValue() : 0.0);
            lj.setTaxPct(l.getTaxPct() != null ? l.getTaxPct().doubleValue() : 11.0);
            lj.setNotes(l.getNotes() != null ? l.getNotes() : "");
            return lj;
        }).toList();
        populateModel(model, entityToForm(po), lines, true, id);
        return VIEW_FORM;
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String update(@PathVariable UUID id, @ModelAttribute("form") PoForm form,
                         RedirectAttributes ra, Model model) throws JsonProcessingException {
        poService.update(id, formToEntity(form));
        ra.addFlashAttribute("successMessage", "PO berhasil diupdate");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String send(@PathVariable UUID id, RedirectAttributes ra) {
        poService.send(id);
        ra.addFlashAttribute("successMessage", "PO berhasil dikirim ke supplier");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String confirm(@PathVariable UUID id,
                          @RequestParam(required = false) String vendorRef,
                          RedirectAttributes ra) {
        poService.confirm(id, vendorRef);
        ra.addFlashAttribute("successMessage", "PO dikonfirmasi supplier");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/create-gr")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createGr(@PathVariable UUID id) {
        return "redirect:/goods-receipts/new?poId=" + id;
    }

    @PostMapping("/{id}/create-bill")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createBill(@PathVariable UUID id) {
        return "redirect:/bills/new?poId=" + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        poService.cancel(id);
        ra.addFlashAttribute("successMessage", "PO dibatalkan");
        return REDIRECT + id;
    }

    @GetMapping("/{id}/print")
    public String print(@PathVariable UUID id, Model model) {
        model.addAttribute("po",      poService.findById(id));
        model.addAttribute("company", companyConfigService.getConfig());
        model.addAttribute("theme",   themeService.getCurrentTheme());
        model.addAttribute("backUrl", "/purchase-orders/" + id);
        return VIEW_PRINT;
    }

    private void populateModel(Model model, PoForm form, List<LineJson> lines,
                               boolean isEdit, UUID poId) throws JsonProcessingException {
        model.addAttribute("form",         form);
        model.addAttribute("isEdit",       isEdit);
        model.addAttribute("poId",         poId);
        model.addAttribute("initialLines", objectMapper.writeValueAsString(lines));
        model.addAttribute("projects",     projectRepository.findAll());
        model.addAttribute("paymentTermOptions", List.of(
            "NET 14","NET 30","NET 45","NET 60","DP 30% NET 30","DP 50% NET 30","Cash Before Delivery"));

        String vendorLabel = "";
        if (form.getVendorId() != null)
            vendorLabel = vendorRepository.findById(form.getVendorId())
                .map(v -> v.getCode() + " - " + v.getName()).orElse("");
        model.addAttribute("vendorLabel", vendorLabel);
    }

    private PurchaseOrder formToEntity(PoForm form) {
        PurchaseOrder po = new PurchaseOrder();
        if (form.getVendorId()  != null) vendorRepository.findById(form.getVendorId()).ifPresent(po::setVendor);
        if (form.getProjectId() != null) projectRepository.findById(form.getProjectId()).ifPresent(po::setProject);
        po.setPoDate(form.getPoDate());
        po.setExpectedDelivery(form.getExpectedDelivery());
        po.setPaymentTerms(form.getPaymentTerms());
        po.setDeliveryAddress(form.getDeliveryAddress());
        po.setNotes(form.getNotes());
        po.setDiscountAmount(form.getDiscountAmount() != null ? form.getDiscountAmount() : BigDecimal.ZERO);
        if (form.getLines() != null) {
            int order = 1;
            for (PoLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                PurchaseOrderLine line = new PurchaseOrderLine();
                line.setPurchaseOrder(po);
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
                po.getLines().add(line);
            }
        }
        return po;
    }

    private PoForm entityToForm(PurchaseOrder po) {
        PoForm form = new PoForm();
        if (po.getVendor()  != null) form.setVendorId(po.getVendor().getId());
        if (po.getProject() != null) form.setProjectId(po.getProject().getId());
        form.setPoDate(po.getPoDate());
        form.setExpectedDelivery(po.getExpectedDelivery());
        form.setPaymentTerms(po.getPaymentTerms());
        form.setDeliveryAddress(po.getDeliveryAddress());
        form.setNotes(po.getNotes());
        form.setDiscountAmount(po.getDiscountAmount());
        List<PoLineForm> lines = new ArrayList<>();
        for (PurchaseOrderLine l : po.getLines()) {
            PoLineForm lf = new PoLineForm();
            if (l.getProduct() != null) lf.setProductId(l.getProduct().getId());
            lf.setDescription(l.getDescription());
            lf.setQuantity(l.getQuantity());
            lf.setUnit(l.getUnit());
            lf.setUnitPrice(l.getUnitPrice());
            lf.setDiscountPct(l.getDiscountPct());
            lf.setTaxPct(l.getTaxPct());
            lf.setNotes(l.getNotes());
            lines.add(lf);
        }
        form.setLines(lines);
        return form;
    }

    @Getter @Setter
    public static class PoForm {
        private UUID       vendorId;
        private UUID       prId;
        private UUID       projectId;
        private LocalDate  poDate;
        private LocalDate  expectedDelivery;
        private String     paymentTerms;
        private String     deliveryAddress;
        private BigDecimal discountAmount;
        private String     notes;
        private List<PoLineForm> lines = new ArrayList<>();
    }

    @Getter @Setter
    public static class PoLineForm {
        private UUID       prLineId;
        private UUID       productId;
        private String     description;
        private BigDecimal quantity;
        private String     unit;
        private BigDecimal unitPrice;
        private BigDecimal discountPct;
        private BigDecimal taxPct = new BigDecimal("11");
        private String     notes;
    }

    @Getter @Setter
    public static class LineJson {
        private String prLineId     = "";
        private String productId    = "";
        private String productLabel = "";
        private String description  = "";
        private double quantity     = 1;
        private String unit         = "";
        private double unitPrice    = 0;
        private double discountPct  = 0;
        private double taxPct       = 11;
        private String notes        = "";
    }
}


// ── GoodsReceiptController.java ──────────────────────────────────────────────
package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.*;
import com.artivisi.accountingfinance.enums.GoodsReceiptStatus;
import com.artivisi.accountingfinance.repository.PurchaseOrderRepository;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.GoodsReceiptService;
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
@RequestMapping("/goods-receipts")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.INVOICE_VIEW + "')")
public class GoodsReceiptController {

    private static final String VIEW_LIST   = "goodsreceipt/list";
    private static final String VIEW_FORM   = "goodsreceipt/form";
    private static final String VIEW_DETAIL = "goodsreceipt/detail";
    private static final String REDIRECT    = "redirect:/goods-receipts/";

    private final GoodsReceiptService   grService;
    private final PurchaseOrderRepository poRepository;
    private final ObjectMapper          objectMapper;

    @GetMapping
    public String list(@RequestParam(required = false) GoodsReceiptStatus status,
                       @RequestParam(required = false) String search,
                       @PageableDefault(size = 20) Pageable pageable,
                       Model model) {
        model.addAttribute("page",           grService.findByFilters(status, search, pageable));
        model.addAttribute("statuses",       GoodsReceiptStatus.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("search",         search);
        model.addAttribute("summary",        grService.getSummary());
        return VIEW_LIST;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable UUID id, Model model) {
        model.addAttribute("gr", grService.findById(id));
        return VIEW_DETAIL;
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String newForm(@RequestParam(required = false) UUID poId,
                          Model model) throws JsonProcessingException {
        GrForm form = new GrForm();
        form.setReceiptDate(LocalDate.now());
        List<LineJson> initialLines = new ArrayList<>();

        if (poId != null) {
            poRepository.findByIdWithLines(poId).ifPresent(po -> {
                form.setPoId(po.getId());
                po.getLines().forEach(pol -> {
                    BigDecimal sisa = pol.getQtyRemaining();
                    if (sisa.compareTo(BigDecimal.ZERO) > 0) {
                        LineJson lj = new LineJson();
                        lj.setPoLineId(pol.getId().toString());
                        lj.setDescription(pol.getDescription());
                        lj.setQuantity(sisa.doubleValue());
                        lj.setUnit(pol.getUnit() != null ? pol.getUnit() : "");
                        lj.setUnitPrice(pol.getUnitPrice() != null ? pol.getUnitPrice().doubleValue() : 0.0);
                        initialLines.add(lj);
                    }
                });
            });
        }
        populateModel(model, form, initialLines);
        return VIEW_FORM;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String create(@ModelAttribute("form") GrForm form,
                         RedirectAttributes ra, Model model) throws JsonProcessingException {
        if (form.getPoId() == null) {
            populateModel(model, form, buildLinesJson(form));
            model.addAttribute("errorMessage", "PO wajib dipilih");
            return VIEW_FORM;
        }
        GoodsReceipt gr = formToEntity(form);
        GoodsReceipt saved = grService.create(gr, form.getPoId());
        ra.addFlashAttribute("successMessage", "GR " + saved.getGrNumber() + " berhasil dibuat");
        return REDIRECT + saved.getId();
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String confirm(@PathVariable UUID id, RedirectAttributes ra) {
        grService.confirm(id);
        ra.addFlashAttribute("successMessage", "Goods Receipt dikonfirmasi. Stok persediaan diupdate.");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_EDIT + "')")
    public String cancel(@PathVariable UUID id, RedirectAttributes ra) {
        grService.cancel(id);
        ra.addFlashAttribute("successMessage", "Goods Receipt dibatalkan");
        return REDIRECT + id;
    }

    @PostMapping("/{id}/create-bill")
    @PreAuthorize("hasAuthority('" + Permission.INVOICE_CREATE + "')")
    public String createBill(@PathVariable UUID id) {
        return "redirect:/bills/new?grId=" + id;
    }

    private void populateModel(Model model, GrForm form, List<LineJson> lines) throws JsonProcessingException {
        model.addAttribute("form",         form);
        model.addAttribute("initialLines", objectMapper.writeValueAsString(lines));
        if (form.getPoId() != null)
            poRepository.findByIdWithLines(form.getPoId())
                .ifPresent(po -> model.addAttribute("po", po));
    }

    private List<LineJson> buildLinesJson(GrForm form) {
        if (form.getLines() == null) return List.of();
        return form.getLines().stream().map(lf -> {
            LineJson lj = new LineJson();
            lj.setPoLineId(lf.getPoLineId() != null ? lf.getPoLineId().toString() : "");
            lj.setDescription(lf.getDescription() != null ? lf.getDescription() : "");
            lj.setQuantity(lf.getQuantity() != null ? lf.getQuantity().doubleValue() : 0.0);
            lj.setUnit(lf.getUnit() != null ? lf.getUnit() : "");
            return lj;
        }).toList();
    }

    private GoodsReceipt formToEntity(GrForm form) {
        GoodsReceipt gr = new GoodsReceipt();
        gr.setReceiptDate(form.getReceiptDate());
        gr.setReceivedBy(form.getReceivedBy());
        gr.setDeliveryNote(form.getDeliveryNote());
        gr.setNotes(form.getNotes());
        if (form.getLines() != null) {
            int order = 1;
            for (GrLineForm lf : form.getLines()) {
                if (lf.getDescription() == null || lf.getDescription().isBlank()) continue;
                GoodsReceiptLine line = new GoodsReceiptLine();
                line.setGoodsReceipt(gr);
                line.setLineOrder(order++);
                line.setDescription(lf.getDescription());
                line.setQuantity(lf.getQuantity() != null ? lf.getQuantity() : BigDecimal.ZERO);
                line.setUnit(lf.getUnit());
                line.setSerialNumbers(lf.getSerialNumbers());
                line.setNotes(lf.getNotes());
                if (lf.getPoLineId() != null) {
                    PurchaseOrderLine pol = new PurchaseOrderLine();
                    pol.setId(lf.getPoLineId());
                    line.setPoLine(pol);
                }
                gr.getLines().add(line);
            }
        }
        return gr;
    }

    @Getter @Setter public static class GrForm {
        private UUID      poId;
        private LocalDate receiptDate;
        private String    receivedBy;
        private String    deliveryNote;
        private String    notes;
        private List<GrLineForm> lines = new ArrayList<>();
    }

    @Getter @Setter public static class GrLineForm {
        private UUID       poLineId;
        private String     description;
        private BigDecimal quantity;
        private String     unit;
        private String     serialNumbers;
        private String     notes;
    }

    @Getter @Setter public static class LineJson {
        private String poLineId     = "";
        private String description  = "";
        private double quantity     = 0;
        private String unit         = "";
        private double unitPrice    = 0;
        private String serialNumbers = "";
        private String notes        = "";
    }
}
