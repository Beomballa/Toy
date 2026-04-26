package com.section.admin.content.controller;

import com.section.admin.content.req.ContentSaveRequest;
import com.section.admin.content.res.ContentListResponse;
import com.section.admin.content.res.ContentSaveResponse;
import com.section.common.content.entity.Document;
import com.section.common.content.service.DocumentService;
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
            @org.springframework.data.web.PageableDefault(size = 9) org.springframework.data.domain.Pageable pageable
    ) {
        try {
            return ResponseEntity.ok(ContentListResponse.of(
                    documentService.getDocumentList(Document.BoardType.valueOf(boardType), pageable)
            ));
        } catch (Exception e) {
            e.printStackTrace(); // 서버 로그에서 확인 가능하도록
            throw e;
        }
    }

    @GetMapping("/get")
    public ResponseEntity<Document> getDetail(@RequestParam("id") Long id) {
        return ResponseEntity.ok(documentService.getDocument(id));
    }

    @PostMapping("/save")
    public ResponseEntity<ContentSaveResponse> save(@RequestBody ContentSaveRequest req) {
        Document savedDocument = documentService.saveDocument(req.toEntity());
        return ResponseEntity.ok(ContentSaveResponse.from(savedDocument.getId()));
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> delete(@RequestParam("id") Long id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok().build();
    }
}
