package com.section.front.product.service;

import com.section.front.product.dto.FrontProductOptionResponse;
import com.section.front.product.dto.FrontCatalogBootstrapResponse;
import com.section.front.product.dto.FrontCatalogFacetResponse;
import com.section.front.product.dto.FrontCatalogMetricsResponse;
import com.section.front.product.dto.FrontProductDetailResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.dto.FrontRelatedProductResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class FrontProductCatalogService {

    public List<FrontProductResponse> getCatalog() {
        return List.of(
                new FrontProductResponse(
                        101L,
                        "New Balance",
                        "러닝화",
                        "990v6 Grey Day",
                        "M990GL6",
                        289000,
                        18,
                        "2026-06-04",
                        "브랜드 시그니처 그레이 팔레트에 퍼포먼스 러닝 실루엣을 더한 대표 드롭입니다.",
                        "Grey precision",
                        true,
                        List.of(
                                new FrontProductOptionResponse("260", 4),
                                new FrontProductOptionResponse("270", 6),
                                new FrontProductOptionResponse("280", 8)
                        )
                ),
                new FrontProductResponse(
                        102L,
                        "Nike",
                        "라이프스타일",
                        "Air Max DN Ember",
                        "DV3337-800",
                        219000,
                        54,
                        "2026-06-03",
                        "강한 주황빛 텐션과 둥근 에어 볼륨이 전면에 드러나는 에너지 중심 모델입니다.",
                        "Ember energy",
                        true,
                        List.of(
                                new FrontProductOptionResponse("255", 10),
                                new FrontProductOptionResponse("265", 24),
                                new FrontProductOptionResponse("275", 20)
                        )
                ),
                new FrontProductResponse(
                        103L,
                        "ASICS",
                        "러닝화",
                        "Gel-Kayano 14 Oyster",
                        "1201A019-200",
                        179000,
                        12,
                        "2026-06-04",
                        "실버 러닝 무드와 베이지 톤이 섞인 안정적인 실루엣으로 여성 고객 반응이 빠른 편입니다.",
                        "Metal calm",
                        true,
                        List.of(
                                new FrontProductOptionResponse("240", 2),
                                new FrontProductOptionResponse("245", 4),
                                new FrontProductOptionResponse("250", 6)
                        )
                ),
                new FrontProductResponse(
                        104L,
                        "Salomon",
                        "아웃도어",
                        "XT-6 Skyline",
                        "L47739100",
                        248000,
                        32,
                        "2026-06-02",
                        "아웃도어 기반 기술 실루엣이지만 도심 착장용 수요가 높은 스테디 라인입니다.",
                        "Trail machine",
                        false,
                        List.of(
                                new FrontProductOptionResponse("260", 7),
                                new FrontProductOptionResponse("270", 12),
                                new FrontProductOptionResponse("280", 13)
                        )
                ),
                new FrontProductResponse(
                        105L,
                        "Adidas",
                        "축구화",
                        "Predator Fold-Over Core",
                        "IG5432",
                        329000,
                        8,
                        "2026-06-01",
                        "폴드오버 텅 디테일이 강하고, 콘텐츠용 주목도는 높지만 사이즈별 편차가 큰 모델입니다.",
                        "Pitch statement",
                        false,
                        List.of(
                                new FrontProductOptionResponse("255", 1),
                                new FrontProductOptionResponse("265", 3),
                                new FrontProductOptionResponse("275", 4)
                        )
                ),
                new FrontProductResponse(
                        106L,
                        "Hoka",
                        "러닝화",
                        "Mach X Voltage",
                        "HM1123",
                        239000,
                        65,
                        "2026-05-29",
                        "쿠셔닝과 반응성을 동시에 묶은 하이브리드 러닝 카테고리에서 리텐션이 좋은 상품입니다.",
                        "Fast cushion",
                        false,
                        List.of(
                                new FrontProductOptionResponse("260", 14),
                                new FrontProductOptionResponse("270", 25),
                                new FrontProductOptionResponse("280", 26)
                        )
                )
        );
    }

    public FrontCatalogBootstrapResponse getBootstrap() {
        List<FrontProductResponse> catalog = getCatalog();
        String latestCreatedDate = catalog.stream()
                .map(FrontProductResponse::createdDate)
                .max(String::compareTo)
                .orElse(null);

        return new FrontCatalogBootstrapResponse(
                catalog,
                new FrontCatalogMetricsResponse(
                        catalog.size(),
                        (int) catalog.stream().filter(product -> product.stock() < 20).count(),
                        latestCreatedDate,
                        (int) catalog.stream()
                                .filter(product -> latestCreatedDate != null && latestCreatedDate.equals(product.createdDate()))
                                .count(),
                        (int) catalog.stream().filter(FrontProductResponse::featured).count(),
                        catalog.stream().mapToInt(FrontProductResponse::stock).sum()
                ),
                buildFacetResponses(catalog, FrontProductResponse::brand),
                buildFacetResponses(catalog, FrontProductResponse::category)
        );
    }

    public Optional<FrontProductResponse> findProduct(long productId) {
        return getCatalog().stream()
                .filter(product -> product.id() == productId)
                .findFirst();
    }

    public Optional<FrontProductDetailResponse> findProductDetail(long productId) {
        List<FrontProductResponse> catalog = getCatalog();
        return catalog.stream()
                .filter(product -> product.id() == productId)
                .findFirst()
                .map(product -> new FrontProductDetailResponse(
                        product.id(),
                        product.brand(),
                        product.category(),
                        product.name(),
                        product.model(),
                        product.price(),
                        product.stock(),
                        product.createdDate(),
                        product.description(),
                        product.mood(),
                        product.featured(),
                        product.options(),
                        buildRelatedProducts(catalog, product)
                ));
    }

    private List<FrontRelatedProductResponse> buildRelatedProducts(List<FrontProductResponse> catalog, FrontProductResponse target) {
        Set<Long> relatedIds = new LinkedHashSet<>();
        List<FrontRelatedProductResponse> relatedProducts = new java.util.ArrayList<>();

        catalog.stream()
                .filter(product -> product.id() != target.id())
                .filter(product -> product.brand().equals(target.brand()))
                .sorted(java.util.Comparator.comparingInt(FrontProductResponse::stock))
                .forEach(product -> appendRelatedProduct(relatedIds, relatedProducts, product));

        catalog.stream()
                .filter(product -> product.id() != target.id())
                .filter(product -> product.category().equals(target.category()))
                .sorted(java.util.Comparator.comparingInt(FrontProductResponse::stock))
                .forEach(product -> appendRelatedProduct(relatedIds, relatedProducts, product));

        return relatedProducts.stream().limit(3).toList();
    }

    private void appendRelatedProduct(
            Set<Long> relatedIds,
            List<FrontRelatedProductResponse> relatedProducts,
            FrontProductResponse product
    ) {
        if (!relatedIds.add(product.id()) || relatedProducts.size() >= 3) {
            return;
        }
        relatedProducts.add(new FrontRelatedProductResponse(
                product.id(),
                product.brand(),
                product.name(),
                product.model(),
                product.price(),
                product.stock()
        ));
    }

    private List<FrontCatalogFacetResponse> buildFacetResponses(
            List<FrontProductResponse> catalog,
            Function<FrontProductResponse, String> classifier
    ) {
        Map<String, Long> grouped = catalog.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()));

        return grouped.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                .map(entry -> new FrontCatalogFacetResponse(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }
}
