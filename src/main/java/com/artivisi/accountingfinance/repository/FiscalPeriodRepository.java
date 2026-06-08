package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.FiscalPeriod;
import com.artivisi.accountingfinance.enums.FiscalPeriodStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, UUID> {

    Optional<FiscalPeriod> findByYearAndMonth(Integer year, Integer month);

    boolean existsByYearAndMonth(Integer year, Integer month);

    List<FiscalPeriod> findByYear(Integer year);

    List<FiscalPeriod> findByStatus(FiscalPeriodStatus status);

    @Query("SELECT fp FROM FiscalPeriod fp ORDER BY fp.year DESC, fp.month DESC")
    Page<FiscalPeriod> findAllOrderByYearMonthDesc(Pageable pageable);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE " +
            "(:year IS NULL OR fp.year = :year) AND " +
            "(:status IS NULL OR fp.status = :status) " +
            "ORDER BY fp.year DESC, fp.month DESC")
    Page<FiscalPeriod> findByFilters(
            @Param("year") Integer year,
            @Param("status") FiscalPeriodStatus status,
            Pageable pageable);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE " +
            "fp.year = :year AND fp.month <= :month " +
            "ORDER BY fp.year DESC, fp.month DESC")
    List<FiscalPeriod> findByYearUpToMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT fp FROM FiscalPeriod fp WHERE " +
            "fp.status = 'OPEN' " +
            "ORDER BY fp.year ASC, fp.month ASC")
    List<FiscalPeriod> findOpenPeriods();

    @Query("SELECT fp FROM FiscalPeriod fp WHERE " +
            "(fp.year > :year OR (fp.year = :year AND fp.month >= :month)) " +
            "ORDER BY fp.year ASC, fp.month ASC")
    List<FiscalPeriod> findPeriodsFromDate(@Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT DISTINCT fp.year FROM FiscalPeriod fp ORDER BY fp.year DESC")
    List<Integer> findDistinctYears();

    long countByStatus(FiscalPeriodStatus status);

    default Optional<FiscalPeriod> findByDate(LocalDate date) {
        return findByYearAndMonth(date.getYear(), date.getMonthValue());
    }
}
