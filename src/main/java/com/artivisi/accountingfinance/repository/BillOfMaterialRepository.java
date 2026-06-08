package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.BillOfMaterial;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BillOfMaterialRepository extends JpaRepository<BillOfMaterial, UUID> {

    Optional<BillOfMaterial> findByCode(String code);

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    @Query("SELECT b FROM BillOfMaterial b WHERE b.active = true ORDER BY b.code")
    List<BillOfMaterial> findAllActive();

    @Query("SELECT b FROM BillOfMaterial b LEFT JOIN FETCH b.product WHERE b.active = true ORDER BY b.code")
    List<BillOfMaterial> findAllActiveWithProduct();

    @Query("SELECT b FROM BillOfMaterial b LEFT JOIN FETCH b.product WHERE b.id = :id")
    Optional<BillOfMaterial> findByIdWithProduct(@Param("id") UUID id);

    @Query("SELECT b FROM BillOfMaterial b LEFT JOIN FETCH b.product LEFT JOIN FETCH b.lines l LEFT JOIN FETCH l.component WHERE b.id = :id")
    Optional<BillOfMaterial> findByIdWithLines(@Param("id") UUID id);

    @Query("SELECT b FROM BillOfMaterial b WHERE b.product.id = :productId")
    List<BillOfMaterial> findByProductId(@Param("productId") UUID productId);

    @Query("SELECT b FROM BillOfMaterial b WHERE b.active = true AND " +
           "(LOWER(b.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(b.product.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY b.code")
    List<BillOfMaterial> searchActive(@Param("search") String search);
}
