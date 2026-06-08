package com.artivisi.accountingfinance.repository;

import com.artivisi.accountingfinance.entity.PayrollSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PayrollScheduleRepository extends JpaRepository<PayrollSchedule, UUID> {

    default Optional<PayrollSchedule> findActive() {
        return findAll().stream()
                .filter(PayrollSchedule::getActive)
                .findFirst();
    }

    default Optional<PayrollSchedule> findCurrent() {
        return findAll().stream().findFirst();
    }
}
