package com.section.admin.brand.service;

import com.section.admin.brand.req.BrandBulkDeleteRequest;
import com.section.admin.brand.req.BrandBulkOperateRequest;
import com.section.admin.brand.req.BrandListRequest;
import com.section.admin.brand.req.BrandSaveRequest;
import com.section.admin.brand.res.BrandListResponse;
import com.section.admin.brand.res.BrandResponse;
import com.section.admin.brand.support.BrandExportCsvWriter;
import com.section.admin.brand.support.BrandExportSummary;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminBrandService {
    private static final int BRAND_EXPORT_MAX_SIZE = 1000;

    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;

    public BrandListResponse getBrandList(BrandListRequest req) {
        Page<Brand> brandPage = brandRepository.getBrandList(
                req.normalizedKeyword(),
                req.normalizedIsActive(),
                PageRequest.of(req.normalizedPage(), req.normalizedSize())
        );
        Page<BrandResponse> responsePage = brandPage.map(BrandResponse::from);
        return BrandListResponse.of(responsePage, req);
    }

    public byte[] exportBrandListCsv(BrandListRequest req) {
        Page<Brand> brandPage = brandRepository.getBrandList(
                req.normalizedKeyword(),
                req.normalizedIsActive(),
                PageRequest.of(0, BRAND_EXPORT_MAX_SIZE)
        );
        return BrandExportCsvWriter.write(
                BrandExportSummary.of(req, java.time.LocalDateTime.now()),
                brandPage.getContent().stream().map(BrandResponse::from).toList()
        );
    }

    public BrandResponse getBrand(Long brandNo) {
        return BrandResponse.from(getBrandEntity(brandNo));
    }

    public Brand getBrandEntity(Long brandNo) {
        return brandRepository.findById(brandNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.BRAND_NOT_FOUND));
    }

    @Transactional
    public void saveBrand(BrandSaveRequest req) {
        String normalizedNameKo = normalizeRequiredText(req.nameKo());
        String normalizedNameEn = normalizeOptionalText(req.nameEn());
        String normalizedLogoUrl = normalizeOptionalText(req.logoUrl());
        String normalizedIsActive = normalizeYnStatus(req.isActive());

        validateDuplicateBrand(req.brandNo(), normalizedNameKo, normalizedNameEn);

        if (req.brandNo() != null) {
            Brand brand = getBrandEntity(req.brandNo());
            brand.update(normalizedNameKo, normalizedNameEn, normalizedLogoUrl, normalizedIsActive);
        } else {
            brandRepository.save(Brand.builder()
                    .nameKo(normalizedNameKo)
                    .nameEn(normalizedNameEn)
                    .logoUrl(normalizedLogoUrl)
                    .isActive(normalizedIsActive)
                    .build());
        }
    }

    @Transactional
    public void deleteBrand(Long brandNo) {
        if (productRepository.existsByBrandNo(brandNo)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Brand brand = getBrandEntity(brandNo);
        brandRepository.delete(brand);
    }

    @Transactional
    public void updateActive(Long brandNo, String isActive) {
        String normalized = isActive == null ? null : isActive.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        Brand brand = getBrandEntity(brandNo);
        brand.update(brand.getNameKo(), brand.getNameEn(), brand.getLogoUrl(), normalized);
    }

    @Transactional
    public BulkOperateResult bulkOperate(BrandBulkOperateRequest req) {
        req.validateOperation();
        List<Long> targetBrandNos = req.normalizedBrandNos();
        String normalizedIsActive = req.normalizedIsActive();

        List<Brand> brands = brandRepository.findAllById(targetBrandNos);
        if (brands.isEmpty()) {
            throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
        }

        int updatedCount = 0;
        int unchangedCount = 0;
        for (Brand brand : brands) {
            if (normalizedIsActive.equals(brand.getIsActive())) {
                unchangedCount += 1;
                continue;
            }
            brand.update(brand.getNameKo(), brand.getNameEn(), brand.getLogoUrl(), normalizedIsActive);
            updatedCount += 1;
        }

        return new BulkOperateResult(targetBrandNos.size(), updatedCount, unchangedCount);
    }

    private void validateDuplicateBrand(Long brandNo, String nameKo, String nameEn) {
        boolean duplicatedKo = brandNo == null
                ? brandRepository.existsByNameKoIgnoreCase(nameKo)
                : brandRepository.existsByNameKoIgnoreCaseAndBrandNoNot(nameKo, brandNo);
        if (duplicatedKo) {
            throw new BusinessException(ErrorCode.BRAND_NAME_DUPLICATED);
        }

        if (nameEn == null) {
            return;
        }

        boolean duplicatedEn = brandNo == null
                ? brandRepository.existsByNameEnIgnoreCase(nameEn)
                : brandRepository.existsByNameEnIgnoreCaseAndBrandNoNot(nameEn, brandNo);
        if (duplicatedEn) {
            throw new BusinessException(ErrorCode.BRAND_NAME_DUPLICATED);
        }
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeYnStatus(String value) {
        String normalized = value == null ? "Y" : value.trim().toUpperCase();
        if (!"Y".equals(normalized) && !"N".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized;
    }

    @Transactional
    public BulkDeleteResult bulkDelete(BrandBulkDeleteRequest req) {
        List<Long> targetBrandNos = req.normalizedBrandNos();
        List<Brand> brands = brandRepository.findAllById(targetBrandNos);
        if (brands.isEmpty()) {
            throw new BusinessException(ErrorCode.BRAND_NOT_FOUND);
        }
        Set<Long> referencedBrandNos = new HashSet<>(productRepository.getReferencedBrandNos(
                brands.stream().map(Brand::getBrandNo).toList()
        ));

        int deletedCount = 0;
        int blockedCount = 0;
        for (Brand brand : brands) {
            if (referencedBrandNos.contains(brand.getBrandNo())) {
                blockedCount += 1;
                continue;
            }
            brandRepository.delete(brand);
            deletedCount += 1;
        }

        HashSet<Long> existingBrandNoSet = new HashSet<>(brands.stream()
                .map(Brand::getBrandNo)
                .toList());
        long missingCount = targetBrandNos.stream()
                .filter(no -> !existingBrandNoSet.contains(no))
                .count();
        return new BulkDeleteResult(targetBrandNos.size(), deletedCount, blockedCount, (int) missingCount);
    }

    public record BulkOperateResult(
            int requestedCount,
            int updatedCount,
            int unchangedCount
    ) {
    }

    public record BulkDeleteResult(
            int requestedCount,
            int deletedCount,
            int blockedCount,
            int missingCount
    ) {
    }
}
