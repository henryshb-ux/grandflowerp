package com.artivisi.accountingfinance.scheduler;

import com.artivisi.accountingfinance.entity.PayrollSchedule;
import com.artivisi.accountingfinance.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

/**
 * Scheduler for automated monthly payroll run creation.
 * Runs daily and checks if today matches the configured day of month.
 * Also catches up on missed runs at startup.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollScheduler {

    private final PayrollService payrollService;

    /**
     * Run daily at 6:30 AM to check if payroll should be created.
     */
    @Scheduled(cron = "${app.payroll.schedule-cron:0 30 6 * * *}")
    public void checkAndCreatePayroll() {
        Optional<PayrollSchedule> scheduleOpt = payrollService.getSchedule();
        if (scheduleOpt.isEmpty()) {
            return;
        }

        PayrollSchedule schedule = scheduleOpt.get();
        if (!Boolean.TRUE.equals(schedule.getActive())) {
            return;
        }

        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() == schedule.getDayOfMonth()) {
            processPayrollForPeriod(YearMonth.from(today), schedule);
        }
    }

    /**
     * On startup, catch up on missed payroll runs.
     * Check current month (if past configured day) and previous month.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        Optional<PayrollSchedule> scheduleOpt = payrollService.getSchedule();
        if (scheduleOpt.isEmpty()) {
            return;
        }

        PayrollSchedule schedule = scheduleOpt.get();
        if (!Boolean.TRUE.equals(schedule.getActive())) {
            return;
        }

        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.from(today);
        YearMonth previousMonth = currentMonth.minusMonths(1);

        // Check previous month (in case it was missed)
        processPayrollForPeriod(previousMonth, schedule);

        // Check current month if past the configured day
        if (today.getDayOfMonth() >= schedule.getDayOfMonth()) {
            processPayrollForPeriod(currentMonth, schedule);
        }
    }

    private void processPayrollForPeriod(YearMonth period, PayrollSchedule schedule) {
        try {
            payrollService.executeScheduledPayroll(period, schedule)
                    .ifPresent(run -> log.info("Scheduled payroll created: period={}, status={}",
                            run.getPayrollPeriod(), run.getStatus()));
        } catch (Exception e) {
            log.error("Scheduled payroll failed for period {}: {}", period, e.getMessage());
        }
    }
}
