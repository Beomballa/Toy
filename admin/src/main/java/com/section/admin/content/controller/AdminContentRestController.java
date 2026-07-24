package com.section.admin.content.controller;

import com.section.admin.content.req.ContentSaveRequest;
import com.section.admin.content.req.ContentBulkOperateRequest;
import com.section.admin.content.req.ContentBulkDeleteRequest;
import com.section.admin.content.req.ContentQuickOperateRequest;
import com.section.admin.content.res.ContentDetailResponse;
import com.section.admin.content.res.ContentListResponse;
import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.content.res.ContentPerformanceTaskResponse;
import com.section.admin.content.res.ContentSaveResponse;
import com.section.admin.content.res.ContentSummaryResponse;
import com.section.admin.content.res.ContentDailyStatsResponse;
import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.admin.content.res.ContentReactionDataQualityResponse;
import com.section.admin.content.res.ContentReactionDetailResponse;
import com.section.admin.content.res.ContentViewAnalyticsResponse;
import com.section.admin.content.res.ContentViewDataQualityResponse;
import com.section.admin.content.service.AdminContentReactionAnalyticsService;
import com.section.admin.content.service.AdminContentPerformanceAnalyticsService;
import com.section.admin.content.service.AdminContentPerformanceTaskService;
import com.section.admin.content.service.AdminContentStatsService;
import com.section.admin.content.service.AdminContentViewAnalyticsService;
import com.section.admin.content.support.ContentReactionAnalyticsCsvWriter;
import com.section.admin.content.support.ContentPerformanceAnalyticsCsvWriter;
import com.section.admin.content.support.ContentExportCsvWriter;
import com.section.admin.content.support.ContentExportSummary;
import com.section.admin.content.support.ContentViewAnalyticsCsvWriter;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import com.section.common.content.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content")
public class AdminContentRestController {
    private static final int CONTENT_EXPORT_MAX_SIZE = 1000;

    private final DocumentService documentService;
    private final AdminOperationPolicyService adminOperationPolicyService;
    private final AdminContentStatsService adminContentStatsService;
    private final AdminContentViewAnalyticsService adminContentViewAnalyticsService;
    private final AdminContentReactionAnalyticsService adminContentReactionAnalyticsService;
    private final AdminContentPerformanceAnalyticsService adminContentPerformanceAnalyticsService;
    private final AdminContentPerformanceTaskService adminContentPerformanceTaskService;

    @GetMapping("/list")
    public ResponseEntity<ContentListResponse> getList(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "publicYn", required = false) String publicYn,
            @RequestParam(value = "pinnedOnly", required = false) Boolean pinnedOnly,
            @RequestParam(value = "productNo", required = false) Long productNo,
            @RequestParam(value = "productLinked", required = false) String productLinked,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @PageableDefault(size = 9) Pageable pageable
    ) {
        DocumentListQuery query = buildQuery(boardType, keyword, status, publicYn, pinnedOnly, productNo, productLinked, startDate, endDate);
        return ResponseEntity.ok(ContentListResponse.of(
                documentService.getDocumentList(query, pageable)
        ));
    }

    @GetMapping("/summary")
    public ResponseEntity<ContentSummaryResponse> getSummary(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "publicYn", required = false) String publicYn,
            @RequestParam(value = "pinnedOnly", required = false) Boolean pinnedOnly,
            @RequestParam(value = "productNo", required = false) Long productNo,
            @RequestParam(value = "productLinked", required = false) String productLinked,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        DocumentListQuery query = buildQuery(boardType, keyword, status, publicYn, pinnedOnly, productNo, productLinked, startDate, endDate);
        return ResponseEntity.ok(ContentSummaryResponse.from(documentService.getDocumentSummary(query)));
    }

    @GetMapping("/stats/daily")
    public ResponseEntity<ContentDailyStatsResponse> getDailyStats() {
        return ResponseEntity.ok(adminContentStatsService.getLatestStats());
    }

    @GetMapping("/stats/views")
    public ResponseEntity<ContentViewAnalyticsResponse> getViewAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        return ResponseEntity.ok(adminContentViewAnalyticsService.getAnalytics(normalizedBoardType, days));
    }

    @GetMapping("/stats/views/quality")
    public ResponseEntity<ContentViewDataQualityResponse> getViewDataQuality() {
        return ResponseEntity.ok(adminContentViewAnalyticsService.getDataQuality());
    }

    @GetMapping("/stats/views/export")
    public ResponseEntity<byte[]> exportViewAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        ContentViewAnalyticsResponse analytics =
                adminContentViewAnalyticsService.getAnalytics(normalizedBoardType, days);
        String boardLabel = normalizedBoardType == null ? "all" : normalizedBoardType.name().toLowerCase(Locale.ROOT);
        String fileName = "content-view-analytics-" + boardLabel + "-" + days + "d-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(ContentViewAnalyticsCsvWriter.write(analytics));
    }

    @GetMapping("/stats/reactions")
    public ResponseEntity<ContentReactionAnalyticsResponse> getReactionAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        return ResponseEntity.ok(adminContentReactionAnalyticsService.getAnalytics(normalizedBoardType, days));
    }

    @GetMapping("/stats/reactions/export")
    public ResponseEntity<byte[]> exportReactionAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        ContentReactionAnalyticsResponse analytics =
                adminContentReactionAnalyticsService.getAnalytics(normalizedBoardType, days);
        String boardLabel = normalizedBoardType == null ? "all" : normalizedBoardType.name().toLowerCase(Locale.ROOT);
        String fileName = "content-reaction-analytics-" + boardLabel + "-" + days + "d-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(ContentReactionAnalyticsCsvWriter.write(analytics));
    }

    @GetMapping("/stats/reactions/quality")
    public ResponseEntity<ContentReactionDataQualityResponse> getReactionDataQuality() {
        return ResponseEntity.ok(adminContentReactionAnalyticsService.getDataQuality());
    }

    @GetMapping("/{id}/reactions")
    public ResponseEntity<ContentReactionDetailResponse> getDocumentReactionInsight(
            @PathVariable("id") long id,
            @RequestParam(value = "days", defaultValue = "30") int days
    ) {
        documentService.getDocument(id);
        return ResponseEntity.ok(adminContentReactionAnalyticsService.getDocumentInsight(id, days));
    }

    @GetMapping("/stats/performance")
    public ResponseEntity<ContentPerformanceAnalyticsResponse> getPerformanceAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        return ResponseEntity.ok(adminContentPerformanceAnalyticsService.getAnalytics(normalizedBoardType, days));
    }

    @GetMapping("/stats/performance/export")
    public ResponseEntity<byte[]> exportPerformanceAnalytics(
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        ContentPerformanceAnalyticsResponse analytics =
                adminContentPerformanceAnalyticsService.getAnalytics(normalizedBoardType, days);
        String boardLabel = normalizedBoardType == null ? "all" : normalizedBoardType.name().toLowerCase(Locale.ROOT);
        String fileName = "content-performance-" + boardLabel + "-" + days + "d-"
                + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(ContentPerformanceAnalyticsCsvWriter.write(analytics));
    }

    @PostMapping("/{id}/performance-task")
    public ResponseEntity<ContentPerformanceTaskResponse> createPerformanceTask(
            @PathVariable("id") long id,
            @RequestParam(value = "boardType", required = false) String boardType,
            @RequestParam(value = "days", defaultValue = "7") int days
    ) {
        adminOperationPolicyService.assertAdminWriteAllowed();
        Document.BoardType normalizedBoardType = boardType == null || boardType.isBlank()
                ? null
                : parseBoardType(boardType);
        return ResponseEntity.ok(adminContentPerformanceTaskService.createTask(id, normalizedBoardType, days));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "publicYn", required = false) String publicYn,
            @RequestParam(value = "pinnedOnly", required = false) Boolean pinnedOnly,
            @RequestParam(value = "productNo", required = false) Long productNo,
            @RequestParam(value = "productLinked", required = false) String productLinked,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        DocumentListQuery query = buildQuery(boardType, keyword, status, publicYn, pinnedOnly, productNo, productLinked, startDate, endDate);
        String fileName = "contents-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(ContentExportCsvWriter.write(
                        ContentExportSummary.from(query),
                        documentService.getDocumentExportList(query, CONTENT_EXPORT_MAX_SIZE)
                ));
    }

    @GetMapping("/get")
    public ResponseEntity<ContentDetailResponse> getDetail(@RequestParam("id") Long id) {
        return ResponseEntity.ok(ContentDetailResponse.from(documentService.getDocument(id)));
    }

    @GetMapping("/read")
    public ResponseEntity<ContentDetailResponse> read(@RequestParam("id") Long id) {
        // 관리자 상세 조회는 운영 지표를 왜곡하지 않도록 조회수를 증가시키지 않는다.
        return ResponseEntity.ok(ContentDetailResponse.from(documentService.getDocument(id)));
    }

    @PostMapping("/save")
    public ResponseEntity<ContentSaveResponse> save(@Valid @RequestBody ContentSaveRequest req) {
        adminOperationPolicyService.assertCommunityWriteAllowed();
        Document savedDocument = documentService.saveDocument(req.toEntity());
        return ResponseEntity.ok(ContentSaveResponse.from(savedDocument.getId()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("id") Long id) {
        adminOperationPolicyService.assertCommunityWriteAllowed();
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{id}/operate")
    public ResponseEntity<DocumentService.BulkOperateResult> operate(
            @PathVariable("id") Long id,
            @RequestBody ContentQuickOperateRequest request
    ) {
        adminOperationPolicyService.assertCommunityWriteAllowed();
        if (!request.hasOperateField()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ResponseEntity.ok(documentService.operateDocument(
                id,
                request.normalizedStatus(),
                request.normalizedPublicYn(),
                request.normalizedPinnedYn()
        ));
    }

    @PatchMapping("/bulk-operate")
    public ResponseEntity<DocumentService.BulkOperateResult> bulkOperate(@RequestBody ContentBulkOperateRequest request) {
        adminOperationPolicyService.assertCommunityWriteAllowed();
        if (!request.hasOperateField()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ResponseEntity.ok(documentService.bulkOperateDocuments(
                request.normalizedIds(),
                request.normalizedStatus(),
                request.normalizedPublicYn(),
                request.normalizedPinnedYn()
        ));
    }

    @PostMapping("/bulk-delete")
    public ResponseEntity<DocumentService.BulkDeleteResult> bulkDelete(@RequestBody ContentBulkDeleteRequest request) {
        adminOperationPolicyService.assertCommunityWriteAllowed();
        return ResponseEntity.ok(documentService.bulkDeleteDocuments(request.normalizedIds()));
    }

    private Document.BoardType parseBoardType(String boardType) {
        try {
            return Document.BoardType.valueOf(normalizeEnumValue(boardType));
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Document.PublishStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return Document.PublishStatus.valueOf(normalizeEnumValue(status));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private YN parseYn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return YN.valueOf(normalizeEnumValue(value));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private LocalDateTime parseStartDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim()).atStartOfDay();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private LocalDateTime parseEndDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value.trim()).atTime(LocalTime.MAX);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private DocumentListQuery buildQuery(
            String boardType,
            String keyword,
            String status,
            String publicYn,
            Boolean pinnedOnly,
            Long productNo,
            String productLinked,
            String startDate,
            String endDate
    ) {
        LocalDateTime startDateTime = parseStartDate(startDate);
        LocalDateTime endDateTime = parseEndDate(endDate);
        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return new DocumentListQuery(
                parseBoardType(boardType),
                keyword,
                parseStatus(status),
                parseYn(publicYn),
                pinnedOnly,
                productNo,
                parseBooleanFilter(productLinked),
                startDateTime,
                endDateTime
        );
    }

    private Boolean parseBooleanFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = normalizeEnumValue(value);
        return switch (normalized) {
            case "Y", "TRUE" -> true;
            case "N", "FALSE" -> false;
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        };
    }

    private String normalizeEnumValue(String value) {
        return value == null ? null : value.trim().toUpperCase(Locale.ROOT);
    }
}
