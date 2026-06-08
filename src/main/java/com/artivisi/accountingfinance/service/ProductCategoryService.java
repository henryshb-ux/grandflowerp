package com.artivisi.accountingfinance.service;

import com.artivisi.accountingfinance.entity.ProductCategory;
import com.artivisi.accountingfinance.repository.ProductCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ProductCategoryService {

    private final ProductCategoryRepository categoryRepository;

    public ProductCategory create(ProductCategory category) {
        validateUniqueCode(category.getCode(), null);
        validateParent(category);
        return categoryRepository.save(category);
    }

    public ProductCategory update(UUID id, ProductCategory updated) {
        ProductCategory existing = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori produk tidak ditemukan: " + id));

        validateUniqueCode(updated.getCode(), id);
        validateParent(updated);
        validateNoCircularReference(id, updated.getParent());

        existing.setCode(updated.getCode());
        existing.setName(updated.getName());
        existing.setDescription(updated.getDescription());
        existing.setParent(updated.getParent());
        existing.setActive(updated.isActive());

        return categoryRepository.save(existing);
    }

    public void delete(UUID id) {
        ProductCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Kategori produk tidak ditemukan: " + id));

        long productCount = categoryRepository.countProductsByCategory(id);
        if (productCount > 0) {
            throw new IllegalStateException("Kategori tidak dapat dihapus karena masih memiliki " + productCount + " produk");
        }

        List<ProductCategory> children = categoryRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException("Kategori tidak dapat dihapus karena masih memiliki sub-kategori");
        }

        categoryRepository.delete(category);
        log.info("Deleted product category: {}", category.getCode());
    }

    @Transactional(readOnly = true)
    public Optional<ProductCategory> findById(UUID id) {
        return categoryRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<ProductCategory> findByCode(String code) {
        return categoryRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> findAllActive() {
        return categoryRepository.findAllActive();
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> findRootCategories() {
        return categoryRepository.findRootCategories();
    }

    @Transactional(readOnly = true)
    public List<ProductCategory> findByParentId(UUID parentId) {
        return categoryRepository.findByParentId(parentId);
    }

    @Transactional(readOnly = true)
    public Page<ProductCategory> findBySearch(String search, Pageable pageable) {
        return categoryRepository.findBySearch(search, pageable);
    }

    private void validateUniqueCode(String code, UUID excludeId) {
        if (excludeId == null) {
            if (categoryRepository.existsByCode(code)) {
                throw new IllegalArgumentException("Kode kategori sudah digunakan: " + code);
            }
        } else {
            if (categoryRepository.existsByCodeAndIdNot(code, excludeId)) {
                throw new IllegalArgumentException("Kode kategori sudah digunakan: " + code);
            }
        }
    }

    private void validateParent(ProductCategory category) {
        if (category.getParent() != null && category.getParent().getId() != null) {
            categoryRepository.findById(category.getParent().getId())
                    .orElseThrow(() -> new IllegalArgumentException("Kategori induk tidak ditemukan"));
        }
    }

    private void validateNoCircularReference(UUID categoryId, ProductCategory parent) {
        if (parent == null || parent.getId() == null) {
            return;
        }

        UUID currentId = parent.getId();
        while (currentId != null) {
            if (currentId.equals(categoryId)) {
                throw new IllegalArgumentException("Referensi sirkuler terdeteksi: kategori tidak dapat menjadi anak dari dirinya sendiri");
            }
            Optional<ProductCategory> current = categoryRepository.findById(currentId);
            currentId = current.map(c -> c.getParent() != null ? c.getParent().getId() : null).orElse(null);
        }
    }
}
