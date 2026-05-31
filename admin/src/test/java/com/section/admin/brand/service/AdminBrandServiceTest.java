package com.section.admin.brand.service;

import com.section.admin.brand.req.BrandListRequest;
import com.section.admin.brand.res.BrandListResponse;
import com.section.common.base.exception.BusinessException;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminBrandServiceTest {

    @Mock
    private BrandRepository brandRepository;
    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminBrandService adminBrandService;

    @Test
    @DisplayName("브랜드 목록은 키워드와 상태로 필터링된다")
    void getBrandListFiltersByKeywordAndActive() {
        BrandListRequest request = new BrandListRequest();
        request.setKeyword("나이키");
        request.setIsActive("Y");

        when(brandRepository.getBrandList("나이키", "Y", PageRequest.of(0, 10))).thenReturn(new PageImpl<>(
                List.of(Brand.builder().brandNo(1L).nameKo("나이키").nameEn("Nike").isActive("Y").build()),
                PageRequest.of(0, 10),
                1
        ));

        BrandListResponse response = adminBrandService.getBrandList(request);

        assertEquals(1, response.items().size());
        assertEquals(0, response.currentPage());
        assertEquals(10, response.pageSize());
        assertEquals(1L, response.totalElements());
        assertEquals("검색 결과 1건", response.resultMeta().resultLabel());
        assertEquals("브랜드명 기준 · 검색=나이키 · 상태=사용", response.resultMeta().querySignature());
    }

    @Test
    @DisplayName("브랜드 삭제는 연결 상품이 있으면 거부한다")
    void deleteBrandRejectsWhenProductsExist() {
        when(productRepository.existsByBrandNo(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> adminBrandService.deleteBrand(1L));
    }

    @Test
    @DisplayName("브랜드 활성 상태 변경은 YN 값만 허용한다")
    void updateActiveChangesStatus() {
        Brand brand = Brand.builder().brandNo(1L).nameKo("나이키").isActive("Y").build();
        when(brandRepository.findById(1L)).thenReturn(Optional.of(brand));

        adminBrandService.updateActive(1L, "N");

        assertEquals("N", brand.getIsActive());
    }

    @Test
    @DisplayName("브랜드 저장은 한글명 중복을 허용하지 않는다")
    void saveBrandRejectsDuplicateKoName() {
        when(brandRepository.existsByNameKoIgnoreCase("나이키")).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                adminBrandService.saveBrand(new com.section.admin.brand.req.BrandSaveRequest(null, " 나이키 ", "NIKE", null, "Y")));

        assertEquals(com.section.common.base.exception.ErrorCode.BRAND_NAME_DUPLICATED, exception.getErrorCode());
    }

    @Test
    @DisplayName("브랜드 저장은 공백 정규화 후 저장한다")
    void saveBrandNormalizesFields() {
        adminBrandService.saveBrand(new com.section.admin.brand.req.BrandSaveRequest(null, " 뉴   발란스 ", " new  balance ", " https://logo ", "y"));

        var captor = forClass(Brand.class);
        verify(brandRepository).save(captor.capture());
        assertEquals("뉴 발란스", captor.getValue().getNameKo());
        assertEquals("new balance", captor.getValue().getNameEn());
        assertEquals("https://logo", captor.getValue().getLogoUrl());
        assertEquals("Y", captor.getValue().getIsActive());
    }

    @Test
    @DisplayName("브랜드 삭제는 존재하는 브랜드 엔티티를 삭제한다")
    void deleteBrandDeletesExistingEntity() {
        Brand brand = Brand.builder().brandNo(3L).nameKo("아식스").isActive("Y").build();
        when(productRepository.existsByBrandNo(3L)).thenReturn(false);
        when(brandRepository.findById(3L)).thenReturn(Optional.of(brand));

        adminBrandService.deleteBrand(3L);

        verify(brandRepository).delete(argThat(item -> item.getBrandNo().equals(3L)));
    }
}
