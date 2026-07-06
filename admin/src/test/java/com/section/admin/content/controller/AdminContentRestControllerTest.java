package com.section.admin.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
import com.section.common.content.dto.DocumentSummaryDto;
import com.section.common.content.entity.Document;
import com.section.common.content.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminContentRestControllerTest {

    @Mock
    private DocumentService documentService;

    @Mock
    private AdminOperationPolicyService adminOperationPolicyService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContentRestController(documentService, adminOperationPolicyService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("콘텐츠 목록은 상태/공개/고정 필터를 QueryDSL 조회 경계로 전달한다")
    void getListPassesExtendedFilters() throws Exception {
        DocumentListItemDto item = new DocumentListItemDto();
        item.setId(1L);
        item.setBoardType("NOTICE");
        item.setStatus("PUBLISHED");
        item.setPublicYn("Y");
        item.setPinnedYn("Y");
        item.setTitle("공지");
        item.setContentPreview("내용");
        item.setViewCnt(10);
        item.setProductNo(41L);
        item.setCrtDtm(LocalDateTime.of(2026, 5, 12, 10, 0));
        when(documentService.getDocumentList(any(DocumentListQuery.class), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 9), 1));

        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", "NOTICE")
                        .param("status", "PUBLISHED")
                        .param("publicYn", "Y")
                        .param("pinnedOnly", "true")
                        .param("productLinked", "Y")
                        .param("productNo", "41")
                        .param("startDate", "2026-05-01")
                        .param("endDate", "2026-05-31")
                        .param("keyword", "공지"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].publicYn").value("Y"))
                .andExpect(jsonPath("$.items[0].pinnedYn").value("Y"))
                .andExpect(jsonPath("$.items[0].productNo").value(41L));

        ArgumentCaptor<DocumentListQuery> captor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).getDocumentList(captor.capture(), any());
        assertEquals(Document.BoardType.NOTICE, captor.getValue().boardType());
        assertEquals(Document.PublishStatus.PUBLISHED, captor.getValue().status());
        assertEquals(YN.Y, captor.getValue().publicYn());
        assertEquals(true, captor.getValue().pinnedOnly());
        assertEquals(true, captor.getValue().productLinked());
        assertEquals(41L, captor.getValue().productNo());
        assertEquals("2026-05-01T00:00", captor.getValue().startDateTime().toString());
        assertEquals("2026-05-31T23:59:59.999999999", captor.getValue().endDateTime().toString());
    }

    @Test
    @DisplayName("콘텐츠 목록은 소문자와 여분 공백으로 들어온 필터도 정규화해서 처리한다")
    void getListNormalizesEnumStyleFilters() throws Exception {
        when(documentService.getDocumentList(any(DocumentListQuery.class), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", " notice ")
                        .param("status", " published ")
                        .param("publicYn", " y ")
                        .param("productLinked", " n "))
                .andExpect(status().isOk());

        ArgumentCaptor<DocumentListQuery> captor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).getDocumentList(captor.capture(), any());
        assertEquals(Document.BoardType.NOTICE, captor.getValue().boardType());
        assertEquals(Document.PublishStatus.PUBLISHED, captor.getValue().status());
        assertEquals(YN.Y, captor.getValue().publicYn());
        assertEquals(false, captor.getValue().productLinked());
    }

    @Test
    @DisplayName("콘텐츠 CSV 내보내기는 동일한 QueryDSL 필터와 다운로드 헤더를 사용한다")
    void exportPassesFiltersAndReturnsAttachmentHeaders() throws Exception {
        DocumentListItemDto item = new DocumentListItemDto();
        item.setId(8L);
        item.setBoardType("NOTICE");
        item.setStatus("PUBLISHED");
        item.setPublicYn("Y");
        item.setPinnedYn("N");
        item.setTitle("배포 공지");
        item.setContentPreview("<p>점검 안내</p>");
        item.setProductNo(77L);
        item.setCrtDtm(LocalDateTime.of(2026, 6, 1, 12, 0));
        when(documentService.getDocumentExportList(any(DocumentListQuery.class), org.mockito.ArgumentMatchers.eq(1000)))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/admin/content/export")
                        .param("boardType", "NOTICE")
                        .param("publicYn", "Y")
                        .param("productLinked", "Y")
                        .param("productNo", "77")
                        .param("keyword", "공지"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment; filename=\"contents-")));

        ArgumentCaptor<DocumentListQuery> captor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).getDocumentExportList(captor.capture(), org.mockito.ArgumentMatchers.eq(1000));
        assertEquals(Document.BoardType.NOTICE, captor.getValue().boardType());
        assertEquals(YN.Y, captor.getValue().publicYn());
        assertEquals("공지", captor.getValue().keyword());
        assertEquals(true, captor.getValue().productLinked());
        assertEquals(77L, captor.getValue().productNo());
    }

    @Test
    @DisplayName("콘텐츠 요약은 동일한 검색 조건으로 집계 응답을 반환한다")
    void getSummaryReturnsAggregatedSnapshot() throws Exception {
        when(documentService.getDocumentSummary(any(DocumentListQuery.class)))
                .thenReturn(new DocumentSummaryDto(9, 6, 3, 7, 2, 2, 4, 321));

        mockMvc.perform(get("/api/admin/content/summary")
                        .param("boardType", "STYLE")
                        .param("productLinked", "Y")
                        .param("productNo", "101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(9))
                .andExpect(jsonPath("$.publishedCount").value(6))
                .andExpect(jsonPath("$.linkedCount").value(4))
                .andExpect(jsonPath("$.totalViewCount").value(321));

        ArgumentCaptor<DocumentListQuery> captor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).getDocumentSummary(captor.capture());
        assertEquals(Document.BoardType.STYLE, captor.getValue().boardType());
        assertEquals(true, captor.getValue().productLinked());
        assertEquals(101L, captor.getValue().productNo());
    }

    @Test
    @DisplayName("잘못된 게시 상태 필터는 400 INVALID_INPUT_VALUE를 반환한다")
    void getListReturnsBadRequestWhenStatusInvalid() throws Exception {
        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", "NOTICE")
                        .param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("콘텐츠 목록은 잘못된 생성일 범위를 400 INVALID_INPUT_VALUE로 반환한다")
    void getListReturnsBadRequestWhenDateRangeInvalid() throws Exception {
        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", "NOTICE")
                        .param("startDate", "2026-05-31")
                        .param("endDate", "2026-05-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("잘못된 상품 연결 필터는 400 INVALID_INPUT_VALUE를 반환한다")
    void getListReturnsBadRequestWhenProductLinkedInvalid() throws Exception {
        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", "NOTICE")
                        .param("productLinked", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("콘텐츠 저장은 게시 상태와 공개/고정 여부를 그대로 응답 계약에 반영한다")
    void saveContentAcceptsExtendedFields() throws Exception {
        Document savedDocument = new Document();
        savedDocument.setId(9L);
        when(documentService.saveDocument(any(Document.class))).thenReturn(savedDocument);

        mockMvc.perform(post("/api/admin/content/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SavePayload(
                                null, "DISCUSS", "제목", "본문", 7L, "PUBLISHED", "N", "Y"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(9L));

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentService).saveDocument(documentCaptor.capture());
        assertEquals(Document.PublishStatus.PUBLISHED, documentCaptor.getValue().getStatus());
        assertEquals(YN.N, documentCaptor.getValue().getPublicYn());
        assertEquals(YN.Y, documentCaptor.getValue().getPinnedYn());
        assertEquals(7L, documentCaptor.getValue().getProductNo());
    }

    @Test
    @DisplayName("콘텐츠 저장은 소문자와 여분 공백으로 들어온 enum 스타일 필드도 정규화한다")
    void saveContentNormalizesEnumStyleFields() throws Exception {
        Document savedDocument = new Document();
        savedDocument.setId(10L);
        when(documentService.saveDocument(any(Document.class))).thenReturn(savedDocument);

        mockMvc.perform(post("/api/admin/content/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SavePayload(
                                null, " discuss ", "제목", "본문", 7L, " published ", " n ", " y "
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10L));

        ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
        verify(documentService, atLeastOnce()).saveDocument(documentCaptor.capture());
        Document captured = documentCaptor.getValue();
        assertEquals(Document.BoardType.DISCUSS, captured.getBoardType());
        assertEquals(Document.PublishStatus.PUBLISHED, captured.getStatus());
        assertEquals(YN.N, captured.getPublicYn());
        assertEquals(YN.Y, captured.getPinnedYn());
        assertEquals(7L, captured.getProductNo());
    }

    @Test
    @DisplayName("존재하지 않는 게시글 상세 조회는 404 DOCUMENT_NOT_FOUND를 반환한다")
    void getDetailReturnsNotFoundWhenDocumentMissing() throws Exception {
        when(documentService.getDocument(999L)).thenThrow(new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/content/get").param("id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("D001"));
    }

    @Test
    @DisplayName("관리자 상세 조회 전용 read API는 조회수를 증가시키지 않는다")
    void readDoesNotIncreaseViewCount() throws Exception {
        Document document = new Document();
        document.setId(11L);
        document.setBoardType(Document.BoardType.NOTICE);
        document.setStatus(Document.PublishStatus.PUBLISHED);
        document.setPublicYn(YN.Y);
        document.setPinnedYn(YN.N);
        document.setTitle("운영 공지");
        document.setContent("본문");
        when(documentService.getDocument(11L)).thenReturn(document);

        mockMvc.perform(get("/api/admin/content/read").param("id", "11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11L));

        verify(documentService).getDocument(11L);
        verify(documentService, never()).readDocument(11L);
    }

    @Test
    @DisplayName("커뮤니티 작성이 비활성화되면 저장은 400 ADMIN_FEATURE_DISABLED를 반환한다")
    void saveReturnsBadRequestWhenCommunityWriteDisabled() throws Exception {
        doThrow(new BusinessException(ErrorCode.ADMIN_FEATURE_DISABLED))
                .when(adminOperationPolicyService)
                .assertCommunityWriteAllowed();

        mockMvc.perform(post("/api/admin/content/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SavePayload(
                                null, "DISCUSS", "제목", "본문", 7L, "PUBLISHED", "N", "Y"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("A002"));
    }

    @Test
    @DisplayName("콘텐츠 저장은 존재하지 않는 상품번호를 전달하면 404 PRODUCT_NOT_FOUND를 반환한다")
    void saveReturnsNotFoundWhenProductMissing() throws Exception {
        when(documentService.saveDocument(any(Document.class)))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(post("/api/admin/content/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SavePayload(
                                null, "STYLE", "제목", "본문", 404L, "PUBLISHED", "Y", "N"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    @DisplayName("콘텐츠 일괄 운영은 선택한 게시글 수를 반환한다")
    void bulkOperateReturnsUpdatedCount() throws Exception {
        when(documentService.bulkOperateDocuments(any(), any(), any(), any()))
                .thenReturn(DocumentService.BulkOperateResult.of(2, 1, 0));

        mockMvc.perform(patch("/api/admin/content/bulk-operate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkOperatePayload(
                                List.of(1L, 2L), "PUBLISHED", "N", "Y"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(2))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(1))
                .andExpect(jsonPath("$.missingCount").value(0));
    }

    @Test
    @DisplayName("콘텐츠 빠른 운영 액션은 단건 상태 변경 결과를 반환한다")
    void operateReturnsSingleUpdateResult() throws Exception {
        when(documentService.operateDocument(any(), any(), any(), any()))
                .thenReturn(DocumentService.BulkOperateResult.of(1, 1, 0));

        mockMvc.perform(patch("/api/admin/content/21/operate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkOperatePayload(
                                null, "DRAFT", "N", null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(1))
                .andExpect(jsonPath("$.updatedCount").value(1))
                .andExpect(jsonPath("$.unchangedCount").value(0))
                .andExpect(jsonPath("$.missingCount").value(0));
    }

    @Test
    @DisplayName("콘텐츠 빠른 운영 액션은 변경 값이 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void operateReturnsBadRequestWhenOperateFieldMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/content/21/operate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkOperatePayload(
                                null, null, null, null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("콘텐츠 일괄 운영은 변경 항목이 없으면 400 INVALID_INPUT_VALUE를 반환한다")
    void bulkOperateReturnsBadRequestWhenOperateFieldMissing() throws Exception {
        mockMvc.perform(patch("/api/admin/content/bulk-operate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkOperatePayload(
                                List.of(1L, 2L), null, null, null
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("콘텐츠 일괄 삭제는 삭제 결과 집계를 반환한다")
    void bulkDeleteReturnsDeletedCounts() throws Exception {
        when(documentService.bulkDeleteDocuments(any()))
                .thenReturn(new DocumentService.BulkDeleteResult(3, 2, 1));

        mockMvc.perform(post("/api/admin/content/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BulkDeletePayload(List.of(1L, 2L, 3L)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestedCount").value(3))
                .andExpect(jsonPath("$.deletedCount").value(2))
                .andExpect(jsonPath("$.missingCount").value(1));
    }

    private record SavePayload(
            Long id,
            String boardType,
            String title,
            String content,
            Long productNo,
            String status,
            String publicYn,
            String pinnedYn
    ) {
    }

    private record BulkOperatePayload(
            List<Long> ids,
            String status,
            String publicYn,
            String pinnedYn
    ) {
    }

    private record BulkDeletePayload(
            List<Long> ids
    ) {
    }
}
