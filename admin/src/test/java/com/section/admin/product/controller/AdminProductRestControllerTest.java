package com.section.admin.product.controller;

import com.section.admin.common.controller.AdminGlobalExceptionHandler;
import com.section.admin.product.req.ProductCreateRequest;
import com.section.admin.product.req.ProductUpdateRequest;
import com.section.admin.product.res.ProductListResponse;
import com.section.admin.product.service.AdminProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.base.entity.type.ProductOrderType;
import com.section.common.commerce.dto.ProductListQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminProductRestControllerTest {

    @Mock
    private AdminProductService adminProductService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AdminProductRestController(adminProductService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new AdminGlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("상품 생성 성공 시 생성된 상품 번호를 반환한다")
    void createProductReturnsCreatedProductNo() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);

        when(adminProductService.createProductInfo(org.mockito.ArgumentMatchers.any(ProductCreateRequest.class)))
                .thenReturn(33L);

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resultCode").value("200"))
                .andExpect(jsonPath("$.productNo").value(33L));
    }

    @Test
    @DisplayName("상품 목록 API는 appliedQuery와 resultMeta를 함께 반환한다")
    void getProductListReturnsAppliedQueryAndResultMeta() throws Exception {
        ProductListResponse response = new ProductListResponse(
                List.of(),
                0,
                1,
                2L,
                new ProductListResponse.ProductStatsItem(2L, 1L, 1L, 0L, 30L),
                ProductListResponse.AppliedQueryItem.from(
                        new ProductListQuery(3L, 7L, null, "뉴발란스 993", ProductOrderType.STOCK_COUNT, true, 30L, false)
                ),
                ProductListResponse.ResultMetaItem.from(
                        new ProductListQuery(3L, 7L, null, "뉴발란스 993", ProductOrderType.STOCK_COUNT, true, 30L, false),
                        2L,
                        1
                )
        );

        when(adminProductService.getProductList(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(response);

        mockMvc.perform(get("/api/admin/product/list?page=0&size=10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedQuery.brandNo").value(7L))
                .andExpect(jsonPath("$.appliedQuery.categoryNo").value(3L))
                .andExpect(jsonPath("$.appliedQuery.orderTypeCode").value("c"))
                .andExpect(jsonPath("$.productStats.lowStockThreshold").value(30L))
                .andExpect(jsonPath("$.resultMeta.resultLabel").value("검색 결과 2개"))
                .andExpect(jsonPath("$.resultMeta.querySignature").value("재고순 · 검색=뉴발란스 993 · 재고<30"));
    }

    @Test
    @DisplayName("상품 CSV 다운로드는 첨부 헤더와 CSV 바디를 반환한다")
    void exportProductListReturnsCsvAttachment() throws Exception {
        byte[] body = "\"내보낸시각\",\"2026.05.06 10:00\"\r\n\"정렬\",\"최신순\"".getBytes();

        when(adminProductService.exportProductListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn(body);

        mockMvc.perform(get("/api/admin/product/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products.csv"))
                .andExpect(content().contentType("text/csv"))
                .andExpect(content().bytes(body));
    }

    @Test
    @DisplayName("잘못된 상품 생성 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void createProductReturnsBadRequestWhenPayloadInvalid() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo(" ");
        request.setReleasePrice(1000);

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 옵션 생성 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void createProductReturnsBadRequestWhenOptionPayloadInvalid() throws Exception {
        ProductCreateRequest.ProductOptionRequest optionRequest = new ProductCreateRequest.ProductOptionRequest();
        optionRequest.setOptionName("  ");
        optionRequest.setStockCnt(-1);
        optionRequest.setAdditionalPrice(0);

        ProductCreateRequest request = new ProductCreateRequest();
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(optionRequest));

        mockMvc.perform(post("/api/admin/product/set")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("상품 목록 CSV 내보내기는 attachment 헤더로 응답한다")
    void exportProductListReturnsAttachmentResponse() throws Exception {
        when(adminProductService.exportProductListCsv(org.mockito.ArgumentMatchers.any()))
                .thenReturn("csv".getBytes());

        mockMvc.perform(get("/api/admin/product/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=products.csv"));
    }

    @Test
    @DisplayName("존재하지 않는 상품 상세 조회는 404 PRODUCT_NOT_FOUND를 반환한다")
    void getProductDetailReturnsNotFoundWhenProductMissing() throws Exception {
        when(adminProductService.getProductDetail(999L))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        mockMvc.perform(get("/api/admin/product/get").param("no", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 상품 삭제는 404 PRODUCT_NOT_FOUND를 반환한다")
    void deleteProductReturnsNotFoundWhenProductMissing() throws Exception {
        doThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND))
                .when(adminProductService)
                .deleteProduct(999L);

        mockMvc.perform(patch("/api/admin/product/delete/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 상품입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 수정 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateProductReturnsBadRequestWhenPayloadInvalid() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo(" ");
        request.setReleasePrice(1000);

        mockMvc.perform(post("/api/admin/product/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }

    @Test
    @DisplayName("잘못된 상품 옵션 수정 요청은 400 INVALID_INPUT_VALUE를 반환한다")
    void updateProductReturnsBadRequestWhenOptionPayloadInvalid() throws Exception {
        ProductUpdateRequest.ProductOptionUpdateRequest optionRequest = new ProductUpdateRequest.ProductOptionUpdateRequest();
        optionRequest.setOptionName("옵션명".repeat(30));
        optionRequest.setStockCnt(1);
        optionRequest.setAdditionalPrice(-1);

        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductNo(1L);
        request.setCategoryNo(2L);
        request.setBrandNo(1L);
        request.setNameKo("테스트 상품");
        request.setReleasePrice(1000);
        request.setOptions(List.of(optionRequest));

        mockMvc.perform(post("/api/admin/product/update")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.message").value("잘못된 입력값입니다."));
    }
}
