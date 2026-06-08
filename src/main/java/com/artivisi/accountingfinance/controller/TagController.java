package com.artivisi.accountingfinance.controller;

import com.artivisi.accountingfinance.entity.Tag;
import com.artivisi.accountingfinance.entity.TagType;
import com.artivisi.accountingfinance.security.Permission;
import com.artivisi.accountingfinance.service.TagService;
import com.artivisi.accountingfinance.service.TagTypeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.UUID;

import static com.artivisi.accountingfinance.controller.ViewConstants.*;

@Controller
@RequestMapping("/tags/types/{tagTypeId}/tags")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('" + Permission.TAG_VIEW + "')")
public class TagController {

    private static final String ATTR_SUCCESS_MESSAGE = "successMessage";
    private static final String ATTR_TAG_TYPE = "tagType";
    private static final String ERR_TAG_TYPE_NOT_FOUND = "Tipe label tidak ditemukan: ";
    private static final String VIEW_FORM = "tags/form";
    private static final String REDIRECT_TAGS_PREFIX = "redirect:/tags/types/";
    private static final String REDIRECT_TAGS_SUFFIX = "/tags";

    private final TagService tagService;
    private final TagTypeService tagTypeService;

    @Getter
    @Setter
    static class TagForm {
        private UUID id;

        @NotBlank(message = "Kode label wajib diisi")
        @Size(max = 20, message = "Kode label maksimal 20 karakter")
        private String code;

        @NotBlank(message = "Nama label wajib diisi")
        @Size(max = 100, message = "Nama label maksimal 100 karakter")
        private String name;

        @Size(max = 255, message = "Deskripsi maksimal 255 karakter")
        private String description;

        private boolean active;
    }

    @GetMapping
    public String list(
            @PathVariable UUID tagTypeId,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "HX-Request", required = false) String hxRequest,
            @PageableDefault(size = 20) Pageable pageable,
            Model model) {

        TagType tagType = tagTypeService.findById(tagTypeId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_TAG_TYPE_NOT_FOUND + tagTypeId));

        Page<Tag> tags = tagService.findByTagTypeAndSearch(tagTypeId, search, pageable);

        model.addAttribute(ATTR_TAG_TYPE, tagType);
        model.addAttribute("tags", tags);
        model.addAttribute("search", search);
        model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);

        if ("true".equals(hxRequest)) {
            return "tags/fragments/tag-table :: table";
        }

        return "tags/list";
    }

    @GetMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.TAG_CREATE + "')")
    public String newForm(@PathVariable UUID tagTypeId, Model model) {
        TagType tagType = tagTypeService.findById(tagTypeId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_TAG_TYPE_NOT_FOUND + tagTypeId));

        TagForm form = new TagForm();
        form.setActive(true);

        model.addAttribute(ATTR_TAG_TYPE, tagType);
        model.addAttribute("tag", form);
        model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
        return VIEW_FORM;
    }

    @PostMapping("/new")
    @PreAuthorize("hasAuthority('" + Permission.TAG_CREATE + "')")
    public String create(
            @PathVariable UUID tagTypeId,
            @Valid @ModelAttribute("tag") TagForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        TagType tagType = tagTypeService.findById(tagTypeId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_TAG_TYPE_NOT_FOUND + tagTypeId));

        if (bindingResult.hasErrors()) {
            model.addAttribute(ATTR_TAG_TYPE, tagType);
            model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
            return VIEW_FORM;
        }

        try {
            Tag tag = new Tag();
            BeanUtils.copyProperties(form, tag, "id");
            tag.setTagType(tagType);
            tagService.create(tag);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS_MESSAGE, "Label berhasil ditambahkan");
            return REDIRECT_TAGS_PREFIX + tagTypeId + REDIRECT_TAGS_SUFFIX;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Kode")) {
                bindingResult.rejectValue("code", "duplicate", e.getMessage());
            } else {
                bindingResult.reject("error", e.getMessage());
            }
            model.addAttribute(ATTR_TAG_TYPE, tagType);
            model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
            return VIEW_FORM;
        }
    }

    @GetMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.TAG_EDIT + "')")
    public String editForm(@PathVariable UUID tagTypeId, @PathVariable UUID id, Model model) {
        TagType tagType = tagTypeService.findById(tagTypeId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_TAG_TYPE_NOT_FOUND + tagTypeId));

        Tag tag = tagService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Label tidak ditemukan: " + id));

        TagForm form = new TagForm();
        BeanUtils.copyProperties(tag, form);

        model.addAttribute(ATTR_TAG_TYPE, tagType);
        model.addAttribute("tag", form);
        model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
        return VIEW_FORM;
    }

    @PostMapping("/{id}/edit")
    @PreAuthorize("hasAuthority('" + Permission.TAG_EDIT + "')")
    public String update(
            @PathVariable UUID tagTypeId,
            @PathVariable UUID id,
            @Valid @ModelAttribute("tag") TagForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        TagType tagType = tagTypeService.findById(tagTypeId)
                .orElseThrow(() -> new IllegalArgumentException(ERR_TAG_TYPE_NOT_FOUND + tagTypeId));

        if (bindingResult.hasErrors()) {
            form.setId(id);
            model.addAttribute(ATTR_TAG_TYPE, tagType);
            model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
            return VIEW_FORM;
        }

        try {
            Tag tag = new Tag();
            BeanUtils.copyProperties(form, tag, "id");
            tag.setTagType(tagType);
            tagService.update(id, tag);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS_MESSAGE, "Label berhasil diubah");
            return REDIRECT_TAGS_PREFIX + tagTypeId + REDIRECT_TAGS_SUFFIX;
        } catch (IllegalArgumentException e) {
            if (e.getMessage().contains("Kode")) {
                bindingResult.rejectValue("code", "duplicate", e.getMessage());
            } else {
                bindingResult.reject("error", e.getMessage());
            }
            form.setId(id);
            model.addAttribute(ATTR_TAG_TYPE, tagType);
            model.addAttribute(ATTR_CURRENT_PAGE, PAGE_TAGS);
            return VIEW_FORM;
        }
    }

    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAuthority('" + Permission.TAG_DELETE + "')")
    public String delete(@PathVariable UUID tagTypeId, @PathVariable UUID id, RedirectAttributes redirectAttributes) {
        try {
            tagService.delete(id);
            redirectAttributes.addFlashAttribute(ATTR_SUCCESS_MESSAGE, "Label berhasil dihapus");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return REDIRECT_TAGS_PREFIX + tagTypeId + REDIRECT_TAGS_SUFFIX;
    }
}
