package com.section.admin.content.controller;

import com.section.admin.content.req.ContentSaveRequest;
import com.section.admin.content.res.ContentDetailResponse;
import com.section.admin.content.res.ContentListResponse;
import com.section.admin.content.res.ContentSaveResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.entity.Document;
import com.section.common.content.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/content")
public class AdminContentRestController {

    private final DocumentService documentService;

    @GetMapping("/list")
    public ResponseEntity<ContentListResponse> getList(
            @RequestParam(value = "boardType", defaultValue = "NOTICE") String boardType,
            @RequestParam(value = "keyword", required = false) String keyword,
            @org.springframework.data.web.PageableDefault(size = 9) org.springframework.data.domain.Pageable pageable
    ) {
        return ResponseEntity.ok(ContentListResponse.of(
                documentService.getDocumentList(new DocumentListQuery(parseBoardType(boardType), keyword), pageable)
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
        Document savedDocument = documentService.saveDocument(req.toEntity());
        return ResponseEntity.ok(ContentSaveResponse.from(savedDocument.getId()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }

    private Document.BoardType parseBoardType(String boardType) {
        try {
            return Document.BoardType.valueOf(boardType);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
    }
}
