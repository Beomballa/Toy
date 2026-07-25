package com.section.front.product.service;

import com.section.common.commerce.dto.FrontCatalogProductRow;
import com.section.common.commerce.dto.FrontCatalogQuery;
import com.section.common.commerce.dto.FrontCatalogSummaryRow;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.front.product.dto.FrontCatalogBootstrapResponse;
import com.section.front.product.dto.FrontCatalogFacetResponse;
import com.section.front.product.dto.FrontCatalogMetricsResponse;
import com.section.front.product.dto.FrontCatalogPageResponse;
import com.section.front.product.dto.FrontProductDetailResponse;
import com.section.front.product.dto.FrontProductOptionResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.dto.FrontProductPageResponse;
import com.section.front.product.dto.FrontRelatedProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FrontProductCatalogService {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;

    public FrontProductPageResponse getCatalog(FrontCatalogQuery query, Pageable pageable) {
        Page<FrontCatalogProductRow> rows = productRepository.getFrontCatalogProducts(query, pageable);
        if (rows.isEmpty() && rows.getTotalElements() > 0 && pageable.getPageNumber() >= rows.getTotalPages()) {
            Pageable lastPage = PageRequest.of(rows.getTotalPages() - 1, pageable.getPageSize());
            rows = productRepository.getFrontCatalogProducts(query, lastPage);
        }
        List<FrontProductResponse> products = toProductResponses(
                rows.getContent(),
                loadOptionMap(rows.stream().map(FrontCatalogProductRow::productNo).toList()),
                query.lowStockThreshold()
        );
        return new FrontProductPageResponse(products, toPageResponse(rows));
    }

    public FrontCatalogBootstrapResponse getBootstrap(FrontCatalogQuery query, Pageable pageable) {
        FrontProductPageResponse catalogPage = getCatalog(query, pageable);
        List<FrontCatalogSummaryRow> summary = productRepository.getFrontCatalogSummary(query);
        String latestCreatedDate = summary.stream()
                .map(FrontCatalogSummaryRow::createdAt)
                .filter(java.util.Objects::nonNull)
                .map(createdAt -> createdAt.toLocalDate().format(DATE_FORMATTER))
                .max(String::compareTo)
                .orElse(null);
        java.util.IntSummaryStatistics priceStats = summary.stream()
                .map(FrontCatalogSummaryRow::releasePrice)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .summaryStatistics();

        return new FrontCatalogBootstrapResponse(
                catalogPage.products(),
                catalogPage.pagination(),
                new FrontCatalogMetricsResponse(
                        summary.size(),
                        (int) summary.stream().filter(product -> safeStock(product.totalStock()) < query.lowStockThreshold()).count(),
                        latestCreatedDate,
                        (int) summary.stream()
                                .filter(product -> latestCreatedDate != null
                                        && product.createdAt() != null
                                        && latestCreatedDate.equals(product.createdAt().toLocalDate().format(DATE_FORMATTER)))
                                .count(),
                        (int) summary.stream().filter(FrontCatalogSummaryRow::featured).count(),
                        saturatedInt(summary.stream().mapToLong(product -> safeStock(product.totalStock())).sum()),
                        priceStats.getCount() == 0 ? 0 : saturatedInt(Math.round(priceStats.getAverage())),
                        priceStats.getCount() == 0 ? 0 : priceStats.getMin(),
                        priceStats.getCount() == 0 ? 0 : priceStats.getMax(),
                        (int) summary.stream().map(FrontCatalogSummaryRow::brandName).filter(java.util.Objects::nonNull).distinct().count(),
                        (int) summary.stream().filter(product -> product.releasePrice() != null
                                && product.releasePrice() < 200000).count(),
                        (int) summary.stream().filter(product -> product.releasePrice() != null
                                && product.releasePrice() >= 200000
                                && product.releasePrice() <= 300000).count(),
                        (int) summary.stream().filter(product -> product.releasePrice() != null
                                && product.releasePrice() > 300000).count()
                ),
                buildFacetResponses(summary, FrontCatalogSummaryRow::brandName),
                buildFacetResponses(summary, FrontCatalogSummaryRow::categoryName)
        );
    }

    public Optional<FrontProductResponse> findProduct(long productId) {
        return productRepository.getFrontCatalogProduct(productId)
                .map(row -> toProductResponse(
                        row,
                        loadOptionMap(List.of(row.productNo())).getOrDefault(row.productNo(), List.of()),
                        20
                ));
    }

    public Optional<FrontProductDetailResponse> findProductDetail(long productId) {
        Optional<FrontCatalogProductRow> detailRow = productRepository.getFrontCatalogProduct(productId);
        if (detailRow.isEmpty()) {
            return Optional.empty();
        }

        FrontCatalogProductRow targetRow = detailRow.get();
        FrontProductResponse target = toProductResponse(
                targetRow,
                loadOptionMap(List.of(targetRow.productNo())).getOrDefault(targetRow.productNo(), List.of()),
                20
        );

        List<FrontCatalogProductRow> relatedRows = productRepository.getRelatedFrontCatalogProducts(
                productId,
                targetRow.brandNo(),
                targetRow.categoryNo(),
                6
        );
        List<FrontProductResponse> relatedCatalog = toProductResponses(
                relatedRows,
                loadOptionMap(relatedRows.stream().map(FrontCatalogProductRow::productNo).toList()),
                20
        );

        return Optional.of(new FrontProductDetailResponse(
                target.id(),
                target.brand(),
                target.category(),
                target.name(),
                target.headline(),
                target.model(),
                target.price(),
                target.stock(),
                target.createdDate(),
                target.description(),
                target.mood(),
                target.featured(),
                target.featuredRank(),
                target.stockStatus(),
                target.priceLabel(),
                target.options(),
                buildRelatedProducts(relatedCatalog, target),
                target.thumbnailUrl()
        ));
    }

    private Map<Long, List<FrontProductOptionResponse>> loadOptionMap(List<Long> productNos) {
        if (productNos.isEmpty()) {
            return Map.of();
        }
        return productOptionRepository.findAllByProductNoInOrderByProductNoAscOptionNameAsc(productNos).stream()
                .collect(Collectors.groupingBy(
                        ProductOption::getProductNo,
                        Collectors.mapping(
                                option -> new FrontProductOptionResponse(option.getOptionName(), option.getStockCnt()),
                                Collectors.toList()
                        )
                ));
    }

    private List<FrontProductResponse> toProductResponses(
            List<FrontCatalogProductRow> rows,
            Map<Long, List<FrontProductOptionResponse>> optionMap,
            int lowStockThreshold
    ) {
        return rows.stream()
                .map(row -> toProductResponse(row, optionMap.getOrDefault(row.productNo(), List.of()), lowStockThreshold))
                .toList();
    }

    private FrontProductResponse toProductResponse(
            FrontCatalogProductRow row,
            List<FrontProductOptionResponse> options,
            int lowStockThreshold
    ) {
        String brandName = defaultText(row.brandName(), "Unknown");
        String categoryName = defaultText(row.categoryName(), "미분류");
        String productName = defaultText(row.productName(), "이름 없는 상품");
        String headline = defaultText(row.headline(), brandName + " curated");
        String mood = defaultText(row.mood(), brandName + " pick");
        String description = defaultText(
                row.description(),
                productName + " 상품 상세 문구가 아직 등록되지 않아 기본 정보만 노출합니다."
        );
        return new FrontProductResponse(
                row.productNo(),
                brandName,
                categoryName,
                productName,
                headline,
                row.modelNum(),
                row.releasePrice(),
                row.totalStock(),
                row.createdAt() == null ? null : row.createdAt().toLocalDate().format(DATE_FORMATTER),
                description,
                mood,
                row.featured(),
                row.featuredRank(),
                toStockStatus(row.totalStock(), lowStockThreshold),
                formatPriceLabel(row.releasePrice()),
                options,
                row.thumbnailUrl()
        );
    }

    private String defaultText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private List<FrontRelatedProductResponse> buildRelatedProducts(
            List<FrontProductResponse> relatedCatalog,
            FrontProductResponse target
    ) {
        Set<Long> relatedIds = new LinkedHashSet<>();
        List<FrontRelatedProductResponse> relatedProducts = new ArrayList<>();

        relatedCatalog.stream()
                .filter(product -> product.id() != target.id())
                .filter(product -> product.brand().equals(target.brand()) && product.category().equals(target.category()))
                .forEach(product -> appendRelatedProduct(relatedIds, relatedProducts, product, "브랜드·카테고리 일치"));

        relatedCatalog.stream()
                .filter(product -> product.id() != target.id())
                .filter(product -> product.brand().equals(target.brand()))
                .filter(product -> !product.category().equals(target.category()))
                .forEach(product -> appendRelatedProduct(relatedIds, relatedProducts, product, "같은 브랜드"));

        relatedCatalog.stream()
                .filter(product -> product.id() != target.id())
                .filter(product -> product.category().equals(target.category()))
                .filter(product -> !product.brand().equals(target.brand()))
                .forEach(product -> appendRelatedProduct(relatedIds, relatedProducts, product, "같은 카테고리"));

        return relatedProducts.stream().limit(3).toList();
    }

    private void appendRelatedProduct(
            Set<Long> relatedIds,
            List<FrontRelatedProductResponse> relatedProducts,
            FrontProductResponse product,
            String reason
    ) {
        if (!relatedIds.add(product.id()) || relatedProducts.size() >= 3) {
            return;
        }
        relatedProducts.add(new FrontRelatedProductResponse(
                product.id(),
                product.brand(),
                product.category(),
                product.name(),
                reason,
                product.model(),
                product.price(),
                product.stock(),
                product.stockStatus(),
                product.priceLabel(),
                product.thumbnailUrl()
        ));
    }

    private String toStockStatus(Integer totalStock, int threshold) {
        int safeStock = totalStock == null ? 0 : totalStock;
        return safeStock < threshold ? "품절 임박" : "재고 안정";
    }

    private String formatPriceLabel(Integer releasePrice) {
        if (releasePrice == null) {
            return "-";
        }
        return String.format("%,d원", releasePrice);
    }

    private FrontCatalogPageResponse toPageResponse(Page<?> page) {
        return new FrontCatalogPageResponse(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private int safeStock(Integer stock) {
        return stock == null ? 0 : stock;
    }

    private int saturatedInt(long value) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(Integer.MIN_VALUE, value));
    }

    private List<FrontCatalogFacetResponse> buildFacetResponses(
            List<FrontCatalogSummaryRow> catalog,
            Function<FrontCatalogSummaryRow, String> classifier
    ) {
        return catalog.stream()
                .filter(product -> classifier.apply(product) != null)
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                .map(entry -> new FrontCatalogFacetResponse(entry.getKey(), entry.getValue().intValue()))
                .toList();
    }
}
