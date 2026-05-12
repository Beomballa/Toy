package com.section.admin.content.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.common.base.entity.type.YN;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.content.dto.DocumentListItemDto;
import com.section.common.content.dto.DocumentListQuery;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminContentRestControllerTest {

    @Mock
    private DocumentService documentService;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminContentRestController(documentService))
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
        item.setCrtDtm(LocalDateTime.of(2026, 5, 12, 10, 0));
        when(documentService.getDocumentList(any(DocumentListQuery.class), any()))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 9), 1));

        mockMvc.perform(get("/api/admin/content/list")
                        .param("boardType", "NOTICE")
                        .param("status", "PUBLISHED")
                        .param("publicYn", "Y")
                        .param("pinnedOnly", "true")
                        .param("keyword", "공지"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$.items[0].publicYn").value("Y"))
                .andExpect(jsonPath("$.items[0].pinnedYn").value("Y"));

        ArgumentCaptor<DocumentListQuery> captor = ArgumentCaptor.forClass(DocumentListQuery.class);
        verify(documentService).getDocumentList(captor.capture(), any());
        assertEquals(Document.BoardType.NOTICE, captor.getValue().boardType());
        assertEquals(Document.PublishStatus.PUBLISHED, captor.getValue().status());
        assertEquals(YN.Y, captor.getValue().publicYn());
        assertEquals(true, captor.getValue().pinnedOnly());
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
    }

    @Test
    @DisplayName("존재하지 않는 게시글 상세 조회는 404 DOCUMENT_NOT_FOUND를 반환한다")
    void getDetailReturnsNotFoundWhenDocumentMissing() throws Exception {
        when(documentService.getDocument(999L)).thenThrow(new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/content/get").param("id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("D001"));
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
}
