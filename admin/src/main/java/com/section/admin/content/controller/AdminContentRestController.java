package com.section.admin.content.controller;

import com.section.admin.content.req.ContentSaveRequest;
import com.section.admin.content.req.ContentBulkOperateRequest;
import com.section.admin.content.req.ContentBulkDeleteRequest;
import com.section.admin.content.res.ContentDetailResponse;
import com.section.admin.content.res.ContentListResponse;
import com.section.admin.content.res.ContentSaveResponse;
import com.section.admin.content.support.ContentExportCsvWriter;
import com.section.admin.content.support.ContentExportSummary;
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

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content")
public class AdminContentRestController {
    private static final int CONTENT_EXPORT_MAX_SIZE = 1000;

    private final DocumentService documentService;
    private final AdminOperationPolicyService adminOperationPolicyService;

    @GetMapping("/list")
    public ResponseEntity<ContentListResponse> getList(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "publicYn", required = false) String publicYn,
            @RequestParam(value = "pinnedOnly", required = false) Boolean pinnedOnly,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate,
            @PageableDefault(size = 9) Pageable pageable
    ) {
        DocumentListQuery query = buildQuery(boardType, keyword, status, publicYn, pinnedOnly, startDate, endDate);
        return ResponseEntity.ok(ContentListResponse.of(
                documentService.getDocumentList(query, pageable)
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "publicYn", required = false) String publicYn,
            @RequestParam(value = "pinnedOnly", required = false) Boolean pinnedOnly,
            @RequestParam(value = "startDate", required = false) String startDate,
            @RequestParam(value = "endDate", required = false) String endDate
    ) {
        DocumentListQuery query = buildQuery(boardType, keyword, status, publicYn, pinnedOnly, startDate, endDate);
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
        return ResponseEntity.ok(ContentDetailResponse.from(documentService.readDocument(id)));
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
            return Document.BoardType.valueOf(boardType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private Document.PublishStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        try {
            return Document.PublishStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }

    private YN parseYn(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return YN.valueOf(value);
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
                startDateTime,
                endDateTime
        );
    }
}
