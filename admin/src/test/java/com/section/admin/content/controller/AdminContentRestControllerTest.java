package com.section.admin.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.settings.service.AdminOperationPolicyService;
import com.section.admin.content.service.AdminContentStatsService;
import com.section.admin.content.service.AdminContentViewAnalyticsService;
import com.section.admin.content.service.AdminContentReactionAnalyticsService;
import com.section.admin.content.service.AdminContentPerformanceAnalyticsService;
import com.section.admin.content.res.ContentDailyStatsResponse;
import com.section.admin.content.res.ContentPerformanceAnalyticsResponse;
import com.section.admin.content.res.ContentReactionAnalyticsResponse;
import com.section.admin.content.res.ContentReactionDataQualityResponse;
import com.section.admin.content.res.ContentReactionDetailResponse;
import com.section.admin.content.res.ContentViewAnalyticsResponse;
import com.section.admin.content.res.ContentViewDataQualityResponse;
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

    @Mock
    private AdminContentStatsService adminContentStatsService;

    @Mock
    private AdminContentViewAnalyticsService adminContentViewAnalyticsService;

    @Mock
    private AdminContentReactionAnalyticsService adminContentReactionAnalyticsService;

    @Mock
    private AdminContentPerformanceAnalyticsService adminContentPerformanceAnalyticsService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContentRestController(
                        documentService,
                        adminOperationPolicyService,
                        adminContentStatsService,
                        adminContentViewAnalyticsService,
                        adminContentReactionAnalyticsService,
                        adminContentPerformanceAnalyticsService
                ))
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
    @DisplayName("콘텐츠 일일 통계 API는 최근 배치 스냅샷을 반환한다")
    void getDailyStatsReturnsLatestBatchSnapshot() throws Exception {
        when(adminContentStatsService.getLatestStats()).thenReturn(new ContentDailyStatsResponse(
                "2026-07-21",
                "2026-07-21 14:30:00",
                List.of(new ContentDailyStatsResponse.Item(
                        "TOTAL", 8, 7, 1, 7, 1, 2, 4, 120
                ))
        ));

        mockMvc.perform(get("/api/admin/content/stats/daily"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.snapshotDate").value("2026-07-21"))
                .andExpect(jsonPath("$.items[0].scope").value("TOTAL"))
                .andExpect(jsonPath("$.items[0].totalCount").value(8))
                .andExpect(jsonPath("$.items[0].totalViewCount").value(120));
    }

    @Test
    @DisplayName("콘텐츠 조회 분석 API는 게시판과 기간을 정규화해 분석 결과를 반환한다")
    void getViewAnalyticsReturnsBoardAnalytics() throws Exception {
        when(adminContentViewAnalyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(new ContentViewAnalyticsResponse(
                        "STYLE",
                        14,
                        "2026-07-10",
                        "2026-07-23",
                        "2026-07-23 12:00:00",
                        new ContentViewAnalyticsResponse.Summary(120, 72, 8, 15.0, 100, 20),
                        List.of(new ContentViewAnalyticsResponse.Trend("2026-07-23", 20, 12)),
                        List.of(new ContentViewAnalyticsResponse.TopContent(
                                31L, "STYLE", "여름 스타일", 25, 18
                        ))
                ));

        mockMvc.perform(get("/api/admin/content/stats/views")
                        .param("boardType", " style ")
                        .param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardType").value("STYLE"))
                .andExpect(jsonPath("$.rangeDays").value(14))
                .andExpect(jsonPath("$.summary.totalViews").value(120))
                .andExpect(jsonPath("$.summary.viewChangeRate").value(20))
                .andExpect(jsonPath("$.trend[0].uniqueVisitors").value(12))
                .andExpect(jsonPath("$.topContents[0].documentId").value(31L));

        verify(adminContentViewAnalyticsService).getAnalytics(Document.BoardType.STYLE, 14);
    }

    @Test
    @DisplayName("콘텐츠 조회 데이터 품질 API는 정상·고아 이벤트 현황을 반환한다")
    void getViewDataQualityReturnsEventIntegritySnapshot() throws Exception {
        when(adminContentViewAnalyticsService.getDataQuality())
                .thenReturn(new ContentViewDataQualityResponse(
                        100, 98, 2, "2026-01-01", "2026-07-23",
                        "CLEANUP_REQUIRED", "2026-07-23 15:00:00"
                ));

        mockMvc.perform(get("/api/admin/content/stats/views/quality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEventCount").value(100))
                .andExpect(jsonPath("$.validEventCount").value(98))
                .andExpect(jsonPath("$.orphanEventCount").value(2))
                .andExpect(jsonPath("$.status").value("CLEANUP_REQUIRED"));
    }

    @Test
    @DisplayName("콘텐츠 조회 분석 CSV는 동일한 게시판·기간과 다운로드 헤더를 사용한다")
    void exportViewAnalyticsReturnsCsvAttachment() throws Exception {
        when(adminContentViewAnalyticsService.getAnalytics(Document.BoardType.NOTICE, 30))
                .thenReturn(viewAnalyticsResponse("NOTICE", 30));

        mockMvc.perform(get("/api/admin/content/stats/views/export")
                        .param("boardType", " notice ")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("content-view-analytics-notice-30d-")
                ))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(com.section.admin.content.support.ContentViewAnalyticsCsvWriter.write(
                                viewAnalyticsResponse("NOTICE", 30)
                        )));

        verify(adminContentViewAnalyticsService).getAnalytics(Document.BoardType.NOTICE, 30);
    }

    @Test
    @DisplayName("콘텐츠 조회 분석 API는 게시판이 비어 있으면 전체 분석을 요청한다")
    void getViewAnalyticsUsesAllBoardsWhenBoardTypeBlank() throws Exception {
        when(adminContentViewAnalyticsService.getAnalytics(null, 7))
                .thenReturn(new ContentViewAnalyticsResponse(
                        "ALL",
                        7,
                        "2026-07-17",
                        "2026-07-23",
                        "2026-07-23 12:00:00",
                        new ContentViewAnalyticsResponse.Summary(0, 0, 0, 0, 0, 0),
                        List.of(),
                        List.of()
                ));

        mockMvc.perform(get("/api/admin/content/stats/views")
                        .param("boardType", " ")
                        .param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardType").value("ALL"));

        verify(adminContentViewAnalyticsService).getAnalytics(null, 7);
    }

    @Test
    @DisplayName("콘텐츠 조회 분석 API는 지원하지 않는 기간의 오류 응답을 전달한다")
    void getViewAnalyticsRejectsUnsupportedRange() throws Exception {
        when(adminContentViewAnalyticsService.getAnalytics(Document.BoardType.NOTICE, 10))
                .thenThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE));

        mockMvc.perform(get("/api/admin/content/stats/views")
                        .param("boardType", "NOTICE")
                        .param("days", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    @DisplayName("콘텐츠 반응 분석 API는 게시판과 기간을 정규화한다")
    void getReactionAnalyticsReturnsBoardAnalytics() throws Exception {
        ContentReactionAnalyticsResponse response = reactionAnalyticsResponse("STYLE", 14);
        when(adminContentReactionAnalyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/content/stats/reactions")
                        .param("boardType", " style ")
                        .param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardType").value("STYLE"))
                .andExpect(jsonPath("$.summary.helpfulRate").value(75))
                .andExpect(jsonPath("$.improvementContents[0].documentId").value(31L));

        verify(adminContentReactionAnalyticsService).getAnalytics(Document.BoardType.STYLE, 14);
    }

    @Test
    @DisplayName("콘텐츠 반응 분석 CSV는 필터 정보와 다운로드 헤더를 유지한다")
    void exportReactionAnalyticsReturnsCsvAttachment() throws Exception {
        ContentReactionAnalyticsResponse response = reactionAnalyticsResponse("NOTICE", 30);
        when(adminContentReactionAnalyticsService.getAnalytics(Document.BoardType.NOTICE, 30))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/content/stats/reactions/export")
                        .param("boardType", "NOTICE")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("content-reaction-analytics-notice-30d-")
                ))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(com.section.admin.content.support.ContentReactionAnalyticsCsvWriter.write(response)));
    }

    @Test
    @DisplayName("콘텐츠 효과 분석 API는 게시판과 기간을 정규화해 조치 우선순위를 반환한다")
    void getPerformanceAnalyticsReturnsPriorities() throws Exception {
        ContentPerformanceAnalyticsResponse response = performanceAnalyticsResponse("STYLE", 14);
        when(adminContentPerformanceAnalyticsService.getAnalytics(Document.BoardType.STYLE, 14))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/content/stats/performance")
                        .param("boardType", " style ")
                        .param("days", "14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardType").value("STYLE"))
                .andExpect(jsonPath("$.rangeDays").value(14))
                .andExpect(jsonPath("$.summary.reactionCoverageRate").value(8))
                .andExpect(jsonPath("$.summary.actionRequiredCount").value(1))
                .andExpect(jsonPath("$.priorityContents[0].priorityScore").value(84))
                .andExpect(jsonPath("$.priorityContents[0].status").value("IMPROVEMENT_REQUIRED"));

        verify(adminContentPerformanceAnalyticsService).getAnalytics(Document.BoardType.STYLE, 14);
    }

    @Test
    @DisplayName("콘텐츠 효과 분석 CSV는 동일 조건과 다운로드 헤더를 유지한다")
    void exportPerformanceAnalyticsReturnsCsvAttachment() throws Exception {
        ContentPerformanceAnalyticsResponse response = performanceAnalyticsResponse("NOTICE", 30);
        when(adminContentPerformanceAnalyticsService.getAnalytics(Document.BoardType.NOTICE, 30))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/content/stats/performance/export")
                        .param("boardType", " notice ")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("text/csv")))
                .andExpect(header().string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("content-performance-notice-30d-")
                ))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes(com.section.admin.content.support.ContentPerformanceAnalyticsCsvWriter.write(response)));

        verify(adminContentPerformanceAnalyticsService).getAnalytics(Document.BoardType.NOTICE, 30);
    }

    @Test
    @DisplayName("콘텐츠 반응 데이터 품질 API는 정상·고아 반응 현황을 반환한다")
    void getReactionDataQualityReturnsIntegrityStatus() throws Exception {
        when(adminContentReactionAnalyticsService.getDataQuality())
                .thenReturn(new ContentReactionDataQualityResponse(
                        10, 9, 1, "2026-07-01 09:00:00", "2026-07-24 12:00:00",
                        "CLEANUP_REQUIRED", "2026-07-24 12:01:00"
                ));

        mockMvc.perform(get("/api/admin/content/stats/reactions/quality"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validCount").value(9))
                .andExpect(jsonPath("$.orphanCount").value(1))
                .andExpect(jsonPath("$.status").value("CLEANUP_REQUIRED"));
    }

    @Test
    @DisplayName("문서 반응 인사이트 API는 문서 존재를 확인하고 기간별 결과를 반환한다")
    void getDocumentReactionInsightReturnsDetail() throws Exception {
        when(documentService.getDocument(31L)).thenReturn(new Document());
        when(adminContentReactionAnalyticsService.getDocumentInsight(31L, 90))
                .thenReturn(new ContentReactionDetailResponse(
                        31, 90, "2026-04-26", "2026-07-24",
                        5, 2, 3, 40, 3,
                        "IMPROVEMENT_REQUIRED", "본문 보완을 검토해 주세요.",
                        List.of(new ContentReactionDetailResponse.Trend("2026-07-24", 1, 0, 1))
                ));

        mockMvc.perform(get("/api/admin/content/31/reactions").param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(31))
                .andExpect(jsonPath("$.helpfulRate").value(40))
                .andExpect(jsonPath("$.status").value("IMPROVEMENT_REQUIRED"));

        verify(documentService).getDocument(31L);
        verify(adminContentReactionAnalyticsService).getDocumentInsight(31L, 90);
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

    private ContentViewAnalyticsResponse viewAnalyticsResponse(String boardType, int rangeDays) {
        return new ContentViewAnalyticsResponse(
                boardType,
                rangeDays,
                "2026-06-24",
                "2026-07-23",
                "2026-07-23 12:00:00",
                new ContentViewAnalyticsResponse.Summary(120, 72, 8, 15.0, 100, 20),
                List.of(new ContentViewAnalyticsResponse.Trend("2026-07-23", 20, 12)),
                List.of(new ContentViewAnalyticsResponse.TopContent(
                        31L, boardType, "여름 스타일", 25, 18
                ))
        );
    }

    private ContentReactionAnalyticsResponse reactionAnalyticsResponse(String boardType, int rangeDays) {
        return new ContentReactionAnalyticsResponse(
                boardType,
                rangeDays,
                "2026-07-11",
                "2026-07-24",
                "2026-07-24 12:00:00",
                "기간 내 마지막 선택 시각 기준 현재 반응",
                new ContentReactionAnalyticsResponse.Summary(4, 3, 1, 75, 4, 1),
                List.of(new ContentReactionAnalyticsResponse.Trend("2026-07-24", 4, 3, 1, 75)),
                List.of(new ContentReactionAnalyticsResponse.Content(31, boardType, "스타일", 4, 3, 1, 75)),
                List.of(new ContentReactionAnalyticsResponse.Content(31, boardType, "스타일", 4, 3, 1, 75))
        );
    }

    private ContentPerformanceAnalyticsResponse performanceAnalyticsResponse(String boardType, int rangeDays) {
        return new ContentPerformanceAnalyticsResponse(
                boardType,
                rangeDays,
                "2026-07-11",
                "2026-07-24",
                "2026-07-24 12:00:00",
                new ContentPerformanceAnalyticsResponse.Summary(50, 4, 25, 8, 1, 1),
                List.of(new ContentPerformanceAnalyticsResponse.Content(
                        31, boardType, "스타일", 50, 20, 4, 1, 3,
                        25, 8, 84, "IMPROVEMENT_REQUIRED", "본문 보완이 필요합니다."
                ))
        );
    }
}
