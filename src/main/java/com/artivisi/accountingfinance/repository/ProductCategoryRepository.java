package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.ProductCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, UUID> {

    Optional<ProductCategory> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("SELECT c FROM ProductCategory c WHERE c.active = true ORDER BY c.name")
    List<ProductCategory> findAllActive();

    @Query("SELECT c FROM ProductCategory c WHERE c.parent IS NULL ORDER BY c.name")
    List<ProductCategory> findRootCategories();

    @Query("SELECT c FROM ProductCategory c WHERE c.parent.id = :parentId ORDER BY c.name")
    List<ProductCategory> findByParentId(@Param("parentId") UUID parentId);

    @Query("SELECT c FROM ProductCategory c WHERE " +
           "(COALESCE(:search, '') = '' OR LOWER(c.code) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))) " +
           "ORDER BY c.code")
    Page<ProductCategory> findBySearch(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategory(@Param("categoryId") UUID categoryId);
}
