package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.MerchantMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MerchantMappingRepository extends JpaRepository<MerchantMapping, UUID> {

    List<MerchantMapping> findAllByOrderByMatchCountDesc();

    @Query("SELECT m FROM MerchantMapping m WHERE " +
           "m.matchType = 'EXACT' AND LOWER(m.merchantPattern) = LOWER(:merchantName)")
    List<MerchantMapping> findExactMatches(@Param("merchantName") String merchantName);

    @Query("SELECT m FROM MerchantMapping m WHERE " +
           "m.matchType = 'CONTAINS' AND LOWER(:merchantName) LIKE LOWER(CONCAT('%', m.merchantPattern, '%'))")
    List<MerchantMapping> findContainsMatches(@Param("merchantName") String merchantName);

    List<MerchantMapping> findByMatchType(MerchantMapping.MatchType matchType);
}
