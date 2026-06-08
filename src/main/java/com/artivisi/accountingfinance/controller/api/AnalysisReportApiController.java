package com.artivisi.accountingfinance.controller.api;

import com.artivisi.accountingfinance.entity.AnalysisReport;
import com.artivisi.accountingfinance.enums.AuditEventType;
import com.artivisi.accountingfinance.repository.AnalysisReportRepository;
import com.artivisi.accountingfinance.service.SecurityAuditService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Analysis API for publishing and listing AI analysis reports.
 */
@RestController
@RequestMapping("/api/analysis")
@Tag(name = "Financial Analysis", description = "Read-only financial data for AI analysis (reports, snapshots, ledgers)")
@PreAuthorize("hasAuthority('SCOPE_analysis:read')")
@RequiredArgsConstructor
public class AnalysisReportApiController {

    private static final String META_DESCRIPTION = "description";

    private final AnalysisReportRepository analysisReportRepository;
    private final SecurityAuditService securityAuditService;

    @PostMapping("/reports")
    @PreAuthorize("hasAuthority('SCOPE_analysis:write')")
    public ResponseEntity<AnalysisResponse<ReportDto>> publishReport(
            @Valid @RequestBody PublishReportRequest request,
            Authentication authentication) {

        AnalysisReport report = new AnalysisReport();
        report.setTitle(request.title());
        report.setReportType(request.reportType());
        report.setIndustry(request.industry());
        report.setExecutiveSummary(request.executiveSummary());
        report.setMetrics(request.metrics());
        report.setFindings(request.findings());
        report.setRecommendations(request.recommendations());
        report.setRisks(request.risks());
        report.setPeriodStart(request.periodStart());
        report.setPeriodEnd(request.periodEnd());
        report.setAiSource(request.aiSource());
        report.setAiModel(request.aiModel());

        String username = authentication != null ? authentication.getName() : "api";
        report.setCreatedBy(username);
        report.setUpdatedBy(username);

        AnalysisReport saved = analysisReportRepository.save(report);

        auditAccess("publish-report", Map.of("reportId", saved.getId().toString(), "title", saved.getTitle()));

        ReportDto dto = toReportDto(saved);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AnalysisResponse<>(
                "analysis-report", LocalDateTime.now(),
                Map.of("reportId", saved.getId().toString()),
                dto,
                Map.of(META_DESCRIPTION, "Published analysis report: " + saved.getTitle())));
    }

    @GetMapping("/reports")
    public ResponseEntity<AnalysisResponse<ReportListDto>> listReports(
            @RequestParam(required = false, defaultValue = "0") int page,
            @RequestParam(required = false, defaultValue = "20") int size) {

        Page<AnalysisReport> reportsPage = analysisReportRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));

        List<ReportDto> reports = reportsPage.getContent().stream()
                .map(this::toReportDto)
                .toList();

        auditAccess("list-reports", Map.of("page", String.valueOf(page), "size", String.valueOf(size)));

        return ResponseEntity.ok(new AnalysisResponse<>(
                "analysis-reports", LocalDateTime.now(),
                Map.of("page", String.valueOf(page), "size", String.valueOf(size)),
                new ReportListDto(reports,
                        reportsPage.getTotalElements(), reportsPage.getTotalPages(), page, size),
                Map.of(META_DESCRIPTION, "Published analysis reports, newest first.")));
    }

    private ReportDto toReportDto(AnalysisReport r) {
        return new ReportDto(
                r.getId(), r.getTitle(), r.getReportType(), r.getIndustry(),
                r.getExecutiveSummary(),
                r.getPeriodStart(), r.getPeriodEnd(),
                r.getAiSource(), r.getAiModel(),
                r.getMetrics(), r.getFindings(),
                r.getRecommendations(), r.getRisks(),
                r.getCreatedBy(), r.getCreatedAt());
    }

    private void auditAccess(String reportType, Map<String, String> params) {
        securityAuditService.logAsync(AuditEventType.API_CALL,
                "Analysis API: " + reportType + " " + params);
    }

    // --- DTOs ---

    public record PublishReportRequest(
            @NotBlank String title,
            @NotBlank String reportType,
            String industry,
            String executiveSummary,
            List<Map<String, String>> metrics,
            List<Map<String, String>> findings,
            List<Map<String, String>> recommendations,
            List<Map<String, String>> risks,
            LocalDate periodStart,
            LocalDate periodEnd,
            String aiSource,
            String aiModel
    ) {}

    public record ReportDto(
            UUID id,
            String title,
            String reportType,
            String industry,
            String executiveSummary,
            LocalDate periodStart,
            LocalDate periodEnd,
            String aiSource,
            String aiModel,
            List<Map<String, String>> metrics,
            List<Map<String, String>> findings,
            List<Map<String, String>> recommendations,
            List<Map<String, String>> risks,
            String createdBy,
            LocalDateTime createdAt
    ) {}

    public record ReportListDto(
            List<ReportDto> reports,
            long totalElements,
            int totalPages,
            int currentPage,
            int pageSize
    ) {}
}
