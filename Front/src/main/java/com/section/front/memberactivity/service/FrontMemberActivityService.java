package com.section.front.memberactivity.service;

import com.section.common.commerce.entity.FrontMemberActivityType;
import com.section.common.commerce.entity.FrontMemberProductActivity;
import com.section.common.commerce.repository.FrontMemberProductActivityRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.repository.AccountRepository;
import com.section.front.memberactivity.dto.FrontMemberActivityProductResponse;
import com.section.front.memberactivity.dto.FrontMemberActivityResponse;
import com.section.front.product.dto.FrontProductResponse;
import com.section.front.product.service.FrontProductCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FrontMemberActivityService {

    private static final int MAX_REQUESTED_IDS = 100;

    private final FrontMemberProductActivityRepository activityRepository;
    private final AccountRepository accountRepository;
    private final FrontProductCatalogService productCatalogService;

    @Transactional(readOnly = true)
    public FrontMemberActivityResponse getActivities(long memberNo) {
        requireAvailableMember(memberNo, false);
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse sync(long memberNo, Map<FrontMemberActivityType, List<Long>> localActivities) {
        requireAvailableMember(memberNo, true);
        Map<FrontMemberActivityType, List<Long>> persisted = persistedIds(memberNo);
        for (FrontMemberActivityType type : FrontMemberActivityType.values()) {
            List<Long> merged = mergeIds(localActivities.get(type), persisted.get(type), type.limit());
            replace(memberNo, type, merged, false);
        }
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse replace(long memberNo, FrontMemberActivityType type, List<Long> productIds) {
        requireAvailableMember(memberNo, true);
        replace(memberNo, type, productIds, true);
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse add(long memberNo, FrontMemberActivityType type, long productNo) {
        requireAvailableMember(memberNo, true);
        List<Long> merged = mergeIds(
                List.of(productNo),
                persistedIds(memberNo).get(type),
                type.limit()
        );
        replace(memberNo, type, merged, true);
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse remove(long memberNo, FrontMemberActivityType type, long productNo) {
        requireAvailableMember(memberNo, true);
        List<Long> remaining = persistedIds(memberNo).get(type).stream()
                .filter(id -> id != productNo)
                .toList();
        replace(memberNo, type, remaining, false);
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse clear(long memberNo, FrontMemberActivityType type) {
        requireAvailableMember(memberNo, true);
        replace(memberNo, type, List.of(), false);
        return response(memberNo);
    }

    @Transactional
    public FrontMemberActivityResponse clearAll(long memberNo) {
        requireAvailableMember(memberNo, true);
        activityRepository.deleteAllByMemberNo(memberNo);
        return emptyResponse();
    }

    private void replace(
            long memberNo,
            FrontMemberActivityType type,
            List<Long> requestedIds,
            boolean rejectMissing
    ) {
        List<Long> normalized = normalizeIds(requestedIds, type.limit());
        Map<Long, FrontProductResponse> products = productCatalogService.findProducts(new LinkedHashSet<>(normalized));
        if (rejectMissing && products.size() != normalized.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "활동에 저장할 상품을 찾을 수 없습니다.");
        }
        List<Long> validIds = normalized.stream().filter(products::containsKey).toList();

        activityRepository.deleteAllByMemberNoAndActivityType(memberNo, type);
        activityRepository.flush();
        LocalDateTime now = LocalDateTime.now();
        List<FrontMemberProductActivity> replacements = new ArrayList<>(validIds.size());
        for (int index = 0; index < validIds.size(); index++) {
            replacements.add(FrontMemberProductActivity.create(
                    memberNo,
                    type,
                    validIds.get(index),
                    now.minus(index, ChronoUnit.MICROS)
            ));
        }
        activityRepository.saveAll(replacements);
    }

    private FrontMemberActivityResponse response(long memberNo) {
        Map<FrontMemberActivityType, List<Long>> groupedIds = persistedIds(memberNo);
        Set<Long> productIds = groupedIds.values().stream()
                .flatMap(List::stream)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        Map<Long, FrontProductResponse> products = productCatalogService.findProducts(productIds);
        Map<String, List<FrontMemberActivityProductResponse>> activities = new java.util.LinkedHashMap<>();
        for (FrontMemberActivityType type : FrontMemberActivityType.values()) {
            activities.put(type.name(), groupedIds.get(type).stream()
                    .map(products::get)
                    .filter(java.util.Objects::nonNull)
                    .map(FrontMemberActivityProductResponse::from)
                    .toList());
        }
        return new FrontMemberActivityResponse(activities, limits());
    }

    private FrontMemberActivityResponse emptyResponse() {
        Map<String, List<FrontMemberActivityProductResponse>> activities = new java.util.LinkedHashMap<>();
        for (FrontMemberActivityType type : FrontMemberActivityType.values()) {
            activities.put(type.name(), List.of());
        }
        return new FrontMemberActivityResponse(activities, limits());
    }

    private Map<String, Integer> limits() {
        Map<String, Integer> limits = new java.util.LinkedHashMap<>();
        for (FrontMemberActivityType type : FrontMemberActivityType.values()) {
            limits.put(type.name(), type.limit());
        }
        return limits;
    }

    private Map<FrontMemberActivityType, List<Long>> persistedIds(long memberNo) {
        Map<FrontMemberActivityType, List<Long>> grouped = new EnumMap<>(FrontMemberActivityType.class);
        for (FrontMemberActivityType type : FrontMemberActivityType.values()) {
            grouped.put(type, new ArrayList<>());
        }
        activityRepository.findAllByMemberNoOrderByLastInteractedAtDescIdDesc(memberNo)
                .forEach(activity -> grouped.get(activity.getActivityType()).add(activity.getProductNo()));
        return grouped;
    }

    private List<Long> mergeIds(List<Long> preferred, List<Long> fallback, int limit) {
        List<Long> merged = new ArrayList<>();
        if (preferred != null) {
            merged.addAll(preferred);
        }
        if (fallback != null) {
            merged.addAll(fallback);
        }
        return normalizeIds(merged, limit);
    }

    private List<Long> normalizeIds(List<Long> productIds, int limit) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        int inspected = 0;
        for (Long productId : productIds) {
            if (inspected++ == MAX_REQUESTED_IDS) {
                break;
            }
            if (productId != null && productId > 0) {
                uniqueIds.add(productId);
            }
            if (uniqueIds.size() == limit) {
                break;
            }
        }
        return List.copyOf(uniqueIds);
    }

    private Account requireAvailableMember(long memberNo, boolean lock) {
        Account account = (lock ? accountRepository.findByIdForUpdate(memberNo) : accountRepository.findById(memberNo))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 정보를 확인할 수 없습니다."));
        if (!account.isAvailableCustomer()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "사용할 수 없는 회원입니다.");
        }
        return account;
    }
}
