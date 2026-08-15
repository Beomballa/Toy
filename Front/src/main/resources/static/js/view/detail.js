(function () {
    const productId = Number(document.body.dataset.productId || 0);
    const BOOKMARK_PRODUCTS_KEY = "front-bookmark-products";
    const COMPARE_PRODUCTS_KEY = "front-compare-products";
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const SELECTED_OPTION_KEY = "front-detail-selected-options";
    const DETAIL_QUANTITY_KEY = "front-detail-option-quantities";
    const PRODUCT_IMAGE_FALLBACK_URL = "/images/product-placeholder.svg";
    const RECENT_VIEWED_LIMIT = 6;
    const optionSortState = {
        mode: "STOCK_ASC",
        lowStockOnly: false,
        stableOnly: false,
        availableOnly: false
    };
    const relatedSortState = {
        mode: "DEFAULT",
        lowStockOnly: false,
        sameBrandOnly: false,
        sameCategoryOnly: false,
        availableOnly: false,
        cheaperOnly: false,
        stockAdvantageOnly: false,
        soldOutOnly: false
    };
    const detailRecentState = {
        sort: "RECENT",
        availableOnly: false
    };
    let currentProduct = null;
    let selectedOptionName = "";
    let selectedQuantity = 1;
    const optionSelectionHistory = [];
    let toastTimerSeed = 0;
    let detailModalReturnFocus = null;
    let detailReportModalReturnFocus = null;
    let reportingReviewId = null;
    let memoryCartToken = null;
    let cartSubmitting = false;
    const detailReviewState = { page: 0, hasNext: false, loading: false, sort: "RECENT" };
    const reviewOrderNumber = new URLSearchParams(window.location.search).get("reviewOrder") || "";

    const elements = {
        detailTitle: document.getElementById("detailTitle"),
        detailProductVisual: document.getElementById("detailProductVisual"),
        detailVisualBrand: document.getElementById("detailVisualBrand"),
        detailVisualModel: document.getElementById("detailVisualModel"),
        detailDescription: document.getElementById("detailDescription"),
        detailGuideDescription: document.getElementById("detailGuideDescription"),
        detailGuideModel: document.getElementById("detailGuideModel"),
        detailGuideCategory: document.getElementById("detailGuideCategory"),
        detailPrice: document.getElementById("detailPrice"),
        detailStockText: document.getElementById("detailStockText"),
        detailMetaRow: document.getElementById("detailMetaRow"),
        detailSignalList: document.getElementById("detailSignalList"),
        detailOverviewGrid: document.getElementById("detailOverviewGrid"),
        detailOptionGrid: document.getElementById("detailOptionGrid"),
        detailOptionCount: document.getElementById("detailOptionCount"),
        detailRelatedGrid: document.getElementById("detailRelatedGrid"),
        detailRelatedCount: document.getElementById("detailRelatedCount"),
        detailReviewCount: document.getElementById("detailReviewCount"),
        detailReviewAverage: document.getElementById("detailReviewAverage"),
        detailReviewAverageText: document.getElementById("detailReviewAverageText"),
        detailReviewDistribution: document.getElementById("detailReviewDistribution"),
        detailReviewList: document.getElementById("detailReviewList"),
        detailReviewMoreButton: document.getElementById("detailReviewMoreButton"),
        detailReviewSort: document.getElementById("detailReviewSort"),
        detailReviewWriteButton: document.getElementById("detailReviewWriteButton"),
        detailReviewForm: document.getElementById("detailReviewForm"),
        detailReviewFormCloseButton: document.getElementById("detailReviewFormCloseButton"),
        detailReviewSubmitButton: document.getElementById("detailReviewSubmitButton"),
        detailReviewContent: document.getElementById("detailReviewContent"),
        detailReviewContentCount: document.getElementById("detailReviewContentCount"),
        detailRecentSection: document.getElementById("detailRecentSection"),
        detailRecentGrid: document.getElementById("detailRecentGrid"),
        detailRecentCount: document.getElementById("detailRecentCount"),
        detailRecentAveragePrice: document.getElementById("detailRecentAveragePrice"),
        detailRecentPriceRange: document.getElementById("detailRecentPriceRange"),
        detailRecentTotalStock: document.getElementById("detailRecentTotalStock"),
        detailRecentLowStockCount: document.getElementById("detailRecentLowStockCount"),
        detailRecentBrandCount: document.getElementById("detailRecentBrandCount"),
        detailRecentSortPriceButton: document.getElementById("detailRecentSortPriceButton"),
        detailRecentSortStockButton: document.getElementById("detailRecentSortStockButton"),
        detailRecentAvailableOnlyButton: document.getElementById("detailRecentAvailableOnlyButton"),
        detailRecentCompareAllButton: document.getElementById("detailRecentCompareAllButton"),
        detailRecentBookmarkAllButton: document.getElementById("detailRecentBookmarkAllButton"),
        detailFocusRelated: document.getElementById("detailFocusRelated"),
        detailPrimaryAction: document.getElementById("detailPrimaryAction"),
        detailMobileBookmarkButton: document.getElementById("detailMobileBookmarkButton"),
        detailMobileCompareButton: document.getElementById("detailMobileCompareButton"),
        detailMobilePrimaryButton: document.getElementById("detailMobilePrimaryButton"),
        detailMobilePrice: document.getElementById("detailMobilePrice"),
        detailShareButton: document.getElementById("detailShareButton"),
        detailCopySummaryButton: document.getElementById("detailCopySummaryButton"),
        detailBookmarkButton: document.getElementById("detailBookmarkButton"),
        detailCompareButton: document.getElementById("detailCompareButton"),
        detailSectionNav: document.getElementById("detailSectionNav"),
        detailOptionSortStockButton: document.getElementById("detailOptionSortStockButton"),
        detailOptionSortNameButton: document.getElementById("detailOptionSortNameButton"),
        detailOptionLowStockOnlyButton: document.getElementById("detailOptionLowStockOnlyButton"),
        detailOptionAvailableOnlyButton: document.getElementById("detailOptionAvailableOnlyButton"),
        detailCopyOptionSummaryButton: document.getElementById("detailCopyOptionSummaryButton"),
        detailOptionSortStockHighButton: document.getElementById("detailOptionSortStockHighButton"),
        detailOptionStableOnlyButton: document.getElementById("detailOptionStableOnlyButton"),
        detailCopyAvailableOptionsButton: document.getElementById("detailCopyAvailableOptionsButton"),
        detailRelatedSortStockButton: document.getElementById("detailRelatedSortStockButton"),
        detailRelatedSortPriceButton: document.getElementById("detailRelatedSortPriceButton"),
        detailRelatedLowStockOnlyButton: document.getElementById("detailRelatedLowStockOnlyButton"),
        detailCopyRelatedSummaryButton: document.getElementById("detailCopyRelatedSummaryButton"),
        detailCopyPriceComparisonButton: document.getElementById("detailCopyPriceComparisonButton"),
        detailRelatedSortPriceLowButton: document.getElementById("detailRelatedSortPriceLowButton"),
        detailRelatedSameBrandButton: document.getElementById("detailRelatedSameBrandButton"),
        detailRelatedSameCategoryButton: document.getElementById("detailRelatedSameCategoryButton"),
        detailRelatedAvailableOnlyButton: document.getElementById("detailRelatedAvailableOnlyButton"),
        detailRelatedCheaperOnlyButton: document.getElementById("detailRelatedCheaperOnlyButton"),
        detailRelatedStockAdvantageOnlyButton: document.getElementById("detailRelatedStockAdvantageOnlyButton"),
        detailRelatedSoldOutOnlyButton: document.getElementById("detailRelatedSoldOutOnlyButton"),
        detailBookmarkAllRelatedButton: document.getElementById("detailBookmarkAllRelatedButton"),
        detailCopyRelatedLinksButton: document.getElementById("detailCopyRelatedLinksButton"),
        detailResetRelatedFiltersButton: document.getElementById("detailResetRelatedFiltersButton"),
        detailRandomRelatedButton: document.getElementById("detailRandomRelatedButton"),
        clearDetailRecentButton: document.getElementById("clearDetailRecentButton"),
        copyDetailRecentSummaryButton: document.getElementById("copyDetailRecentSummaryButton"),
        copyDetailRecentLinksButton: document.getElementById("copyDetailRecentLinksButton"),
        detailPreviousRecentButton: document.getElementById("detailPreviousRecentButton"),
        detailNextRecentButton: document.getElementById("detailNextRecentButton"),
        backToCatalogLink: document.getElementById("backToCatalogLink"),
        detailCatalogLink: document.getElementById("detailCatalogLink"),
        detailScrollProgress: document.getElementById("detailScrollProgress"),
        detailStatus: document.getElementById("detailStatus"),
        detailBreadcrumbCategory: document.getElementById("detailBreadcrumbCategory"),
        detailBreadcrumbProduct: document.getElementById("detailBreadcrumbProduct"),
        detailCopyBreadcrumbButton: document.getElementById("detailCopyBreadcrumbButton"),
        detailOptionSelection: document.getElementById("detailOptionSelection"),
        detailOptionSelectionText: document.getElementById("detailOptionSelectionText"),
        detailOptionHistory: document.getElementById("detailOptionHistory"),
        detailOptionHistoryCount: document.getElementById("detailOptionHistoryCount"),
        detailOptionHistoryList: document.getElementById("detailOptionHistoryList"),
        detailPreviousOptionButton: document.getElementById("detailPreviousOptionButton"),
        detailNextOptionButton: document.getElementById("detailNextOptionButton"),
        detailCopyOptionHistoryButton: document.getElementById("detailCopyOptionHistoryButton"),
        detailClearOptionHistoryButton: document.getElementById("detailClearOptionHistoryButton"),
        detailClearOptionButton: document.getElementById("detailClearOptionButton"),
        detailCopySelectedOptionButton: document.getElementById("detailCopySelectedOptionButton"),
        detailShareSelectedOptionButton: document.getElementById("detailShareSelectedOptionButton"),
        detailPurchaseEstimate: document.getElementById("detailPurchaseEstimate"),
        detailQuantityDecreaseButton: document.getElementById("detailQuantityDecreaseButton"),
        detailQuantityIncreaseButton: document.getElementById("detailQuantityIncreaseButton"),
        detailQuantityInput: document.getElementById("detailQuantityInput"),
        detailQuantityPresetTwoButton: document.getElementById("detailQuantityPresetTwoButton"),
        detailQuantityPresetThreeButton: document.getElementById("detailQuantityPresetThreeButton"),
        detailQuantityPresetFiveButton: document.getElementById("detailQuantityPresetFiveButton"),
        detailEstimatedTotal: document.getElementById("detailEstimatedTotal"),
        detailUnitPrice: document.getElementById("detailUnitPrice"),
        detailRemainingStock: document.getElementById("detailRemainingStock"),
        detailOptionRemainingRate: document.getElementById("detailOptionRemainingRate"),
        detailProductStockUsageRate: document.getElementById("detailProductStockUsageRate"),
        detailSafeQuantity: document.getElementById("detailSafeQuantity"),
        detailPurchaseUrgency: document.getElementById("detailPurchaseUrgency"),
        detailQuantityStatus: document.getElementById("detailQuantityStatus"),
        detailStockUsageRate: document.getElementById("detailStockUsageRate"),
        detailStockUsageBar: document.getElementById("detailStockUsageBar"),
        detailQuantityNotice: document.getElementById("detailQuantityNotice"),
        detailQuantityMaxButton: document.getElementById("detailQuantityMaxButton"),
        detailQuantityResetButton: document.getElementById("detailQuantityResetButton"),
        detailCopyOrderSummaryButton: document.getElementById("detailCopyOrderSummaryButton"),
        detailAddCartButton: document.getElementById("detailAddCartButton"),
        detailBuyNowButton: document.getElementById("detailBuyNowButton"),
        detailZoomButton: document.getElementById("detailZoomButton"),
        detailImageModal: document.getElementById("detailImageModal"),
        detailImageModalCloseButton: document.getElementById("detailImageModalCloseButton"),
        detailImageModalImage: document.getElementById("detailImageModalImage"),
        detailReportModal: document.getElementById("detailReportModal"),
        detailReportForm: document.getElementById("detailReportForm"),
        detailReportReason: document.getElementById("detailReportReason"),
        detailReportDetail: document.getElementById("detailReportDetail"),
        detailReportModalCloseButton: document.getElementById("detailReportModalCloseButton"),
        detailReportCancelButton: document.getElementById("detailReportCancelButton"),
        detailReportSubmitButton: document.getElementById("detailReportSubmitButton"),
        detailMobileActions: document.getElementById("detailMobileActions"),
        detailRetryButton: document.getElementById("detailRetryButton"),
        detailScrollTopButton: document.getElementById("detailScrollTopButton"),
        detailAvailableOptionCount: document.getElementById("detailAvailableOptionCount"),
        detailLowOptionCount: document.getElementById("detailLowOptionCount"),
        detailSoldOutOptionCount: document.getElementById("detailSoldOutOptionCount"),
        detailTotalOptionStock: document.getElementById("detailTotalOptionStock"),
        detailMinOptionStock: document.getElementById("detailMinOptionStock"),
        detailMaxOptionStock: document.getElementById("detailMaxOptionStock"),
        detailMedianOptionStock: document.getElementById("detailMedianOptionStock"),
        detailSelectedOptionRank: document.getElementById("detailSelectedOptionRank"),
        detailOptionStockRateText: document.getElementById("detailOptionStockRateText"),
        detailOptionStockRateBar: document.getElementById("detailOptionStockRateBar"),
        detailRecommendOptionButton: document.getElementById("detailRecommendOptionButton"),
        detailCopyOptionMatrixButton: document.getElementById("detailCopyOptionMatrixButton"),
        detailOptionSoldOutRate: document.getElementById("detailOptionSoldOutRate"),
        detailOptionCriticalRate: document.getElementById("detailOptionCriticalRate"),
        detailOptionLowRate: document.getElementById("detailOptionLowRate"),
        detailOptionStableRate: document.getElementById("detailOptionStableRate"),
        detailOptionConcentrationRate: document.getElementById("detailOptionConcentrationRate"),
        detailOptionSoldOutBar: document.getElementById("detailOptionSoldOutBar"),
        detailOptionCriticalBar: document.getElementById("detailOptionCriticalBar"),
        detailOptionLowBar: document.getElementById("detailOptionLowBar"),
        detailOptionStableBar: document.getElementById("detailOptionStableBar"),
        detailOptionConcentrationBar: document.getElementById("detailOptionConcentrationBar"),
        detailPreviousRelatedButton: document.getElementById("detailPreviousRelatedButton"),
        detailNextRelatedButton: document.getElementById("detailNextRelatedButton"),
        detailRelatedAveragePrice: document.getElementById("detailRelatedAveragePrice"),
        detailRelatedMinPrice: document.getElementById("detailRelatedMinPrice"),
        detailRelatedMaxPrice: document.getElementById("detailRelatedMaxPrice"),
        detailRelatedPriceSpread: document.getElementById("detailRelatedPriceSpread"),
        detailRelatedTotalStock: document.getElementById("detailRelatedTotalStock"),
        detailRelatedCheaperCount: document.getElementById("detailRelatedCheaperCount"),
        detailRelatedHigherStockCount: document.getElementById("detailRelatedHigherStockCount"),
        detailRelatedSoldOutCount: document.getElementById("detailRelatedSoldOutCount"),
        detailRelatedFilterStatus: document.getElementById("detailRelatedFilterStatus"),
        detailRelatedMaxSaving: document.getElementById("detailRelatedMaxSaving"),
        detailRelatedMaxStockGain: document.getElementById("detailRelatedMaxStockGain"),
        detailRelatedAvailableRate: document.getElementById("detailRelatedAvailableRate"),
        detailRelatedSameBrandCount: document.getElementById("detailRelatedSameBrandCount"),
        detailCopyValueAnalysisButton: document.getElementById("detailCopyValueAnalysisButton"),
        detailRelatedCheaperRate: document.getElementById("detailRelatedCheaperRate"),
        detailRelatedStockAdvantageRate: document.getElementById("detailRelatedStockAdvantageRate"),
        detailRelatedSameBrandRate: document.getElementById("detailRelatedSameBrandRate"),
        detailRelatedSameCategoryRate: document.getElementById("detailRelatedSameCategoryRate"),
        detailRelatedAverageSavingRate: document.getElementById("detailRelatedAverageSavingRate"),
        detailRelatedCheaperBar: document.getElementById("detailRelatedCheaperBar"),
        detailRelatedStockAdvantageBar: document.getElementById("detailRelatedStockAdvantageBar"),
        detailRelatedSameBrandBar: document.getElementById("detailRelatedSameBrandBar"),
        detailRelatedSameCategoryBar: document.getElementById("detailRelatedSameCategoryBar"),
        detailRelatedAverageSavingBar: document.getElementById("detailRelatedAverageSavingBar"),
        detailCompareAllRelatedButton: document.getElementById("detailCompareAllRelatedButton"),
        detailCheapestRelatedButton: document.getElementById("detailCheapestRelatedButton"),
        detailHighestStockRelatedButton: document.getElementById("detailHighestStockRelatedButton"),
        detailBalancedRelatedButton: document.getElementById("detailBalancedRelatedButton")
    };

    function formatPrice(price) {
        return `${Number(price || 0).toLocaleString("ko-KR")}원`;
    }

    function lowStockThreshold() {
        return 20;
    }

    function stockLabel(stock) {
        return Number(stock || 0) < lowStockThreshold() ? "품절 임박" : "재고 안정";
    }

    function stockClassName(stock) {
        return Number(stock || 0) < lowStockThreshold() ? "is-low-stock" : "is-stable-stock";
    }

    function stockPressureDetail(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return `재고 ${quantity}개로 즉시 확인이 필요합니다.`;
        }
        if (quantity < lowStockThreshold()) {
            return `재고 ${quantity}개로 긴장 구간에 들어가 있습니다.`;
        }
        return `재고 ${quantity}개로 안정적으로 유지되고 있습니다.`;
    }

    function brandInitials(brand) {
        if (!brand) {
            return "GS";
        }
        return brand
            .split(/\s+/)
            .filter(Boolean)
            .slice(0, 2)
            .map((token) => token.charAt(0).toUpperCase())
            .join("");
    }

    function productVisualMarkup(product, className) {
        product = markupSafeObject(product);
        const thumbnail = String(product.thumbnailUrl || "").trim();
        const usesFallback = !thumbnail || thumbnail === PRODUCT_IMAGE_FALLBACK_URL;
        const imageSource = thumbnail || PRODUCT_IMAGE_FALLBACK_URL;
        const imageLabel = usesFallback ? `${product.name || "상품"} 대체 이미지` : (product.name || "상품 이미지");
        return `
            <div class="${className} product-visual--has-image${usesFallback ? " is-image-fallback" : ""}">
                <img class="product-visual__image" src="${escapeAttribute(imageSource)}" alt="${escapeAttribute(imageLabel)}" loading="lazy" decoding="async" data-product-image${usesFallback ? " data-image-fallback=\"true\"" : ""}>
                <span class="${className}__badge">${brandInitials(product.brand)}</span>
                <div class="${className}__copy">
                    <strong>${product.brand || "NOREN"}</strong>
                    <span>${product.category || product.model || product.reason || "Curated pick"}</span>
                </div>
            </div>
        `;
    }

    function handleProductImageError(event) {
        if (!event.target.matches?.("[data-product-image]")) {
            return;
        }
        const image = event.target;
        const visual = image.closest(".product-visual--has-image");
        const isDetailImage = visual === elements.detailProductVisual;
        if (image.dataset.imageFallback === "true") {
            visual?.classList.add("is-image-error");
            visual?.removeAttribute("aria-busy");
            image.remove();
            if (isDetailImage) {
                clearDetailImageModalSource();
                elements.detailZoomButton.hidden = true;
            }
            return;
        }
        image.dataset.imageFallback = "true";
        image.src = PRODUCT_IMAGE_FALLBACK_URL;
        image.alt = `${image.alt || "상품"} 대체 이미지`;
        visual?.classList.add("is-image-fallback");
        if (isDetailImage) {
            clearDetailImageModalSource();
            elements.detailZoomButton.hidden = true;
        }
    }

    function clearDetailImageModalSource() {
        elements.detailImageModalImage?.removeAttribute("src");
        elements.detailImageModalImage?.removeAttribute("alt");
    }

    function escapeAttribute(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }

    function escapeMarkup(value) {
        return String(value || "")
            .replaceAll("&", "&amp;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;")
            .replaceAll('"', "&quot;")
            .replaceAll("'", "&#39;");
    }

    function markupSafeObject(value) {
        if (!value || typeof value !== "object") {
            return value;
        }
        return new Proxy(value, {
            get(target, property) {
                const result = target[property];
                if (typeof result === "string") {
                    return escapeMarkup(result);
                }
                if (Array.isArray(result)) {
                    return result.map(markupSafeObject);
                }
                return result;
            }
        });
    }

    function renderMeta(product) {
        if (!elements.detailMetaRow) {
            return;
        }
        product = markupSafeObject(product);
        elements.detailMetaRow.innerHTML = `
            <span class="product-drawer__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
            <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
            <span class="product-drawer__pill is-stable-stock">${product.category}</span>
            ${product.featured ? `<span class="product-drawer__pill">Featured${product.featuredRank ? ` #${product.featuredRank}` : ""}</span>` : ""}
        `;
        if (elements.detailVisualBrand) {
            elements.detailVisualBrand.textContent = brandInitials(product.brand);
        }
        if (elements.detailVisualModel) {
            elements.detailVisualModel.textContent = product.model || product.name || "Product";
        }
        if (elements.detailBreadcrumbCategory) {
            elements.detailBreadcrumbCategory.textContent = product.category || "상품";
            elements.detailBreadcrumbCategory.href = `/front?category=${encodeURIComponent(product.category || "")}`;
        }
        if (elements.detailBreadcrumbProduct) {
            elements.detailBreadcrumbProduct.textContent = product.name || product.headline || "상세";
        }
        renderDetailThumbnail(product);
    }

    function renderDetailThumbnail(product) {
        if (!elements.detailProductVisual) {
            return;
        }
        elements.detailProductVisual.querySelector("[data-product-image]")?.remove();
        elements.detailProductVisual.classList.remove("product-visual--has-image", "is-image-error", "is-image-fallback");
        const thumbnail = String(product.thumbnailUrl || "").trim();
        const usesFallback = !thumbnail || thumbnail === PRODUCT_IMAGE_FALLBACK_URL;
        const imageSource = thumbnail || PRODUCT_IMAGE_FALLBACK_URL;
        clearDetailImageModalSource();
        elements.detailZoomButton.hidden = true;
        elements.detailProductVisual.setAttribute("aria-busy", "true");
        const image = document.createElement("img");
        image.className = "product-visual__image";
        image.src = imageSource;
        image.alt = usesFallback ? `${product.name || "상품"} 대체 이미지` : (product.name || "상품 이미지");
        image.decoding = "async";
        image.fetchPriority = "high";
        image.dataset.productImage = "";
        if (usesFallback) {
            image.dataset.imageFallback = "true";
            elements.detailProductVisual.classList.add("is-image-fallback");
        }
        image.addEventListener("load", () => {
            elements.detailProductVisual.removeAttribute("aria-busy");
            if (image.dataset.imageFallback === "true") {
                return;
            }
            elements.detailZoomButton.hidden = false;
            if (elements.detailImageModalImage) {
                elements.detailImageModalImage.src = image.currentSrc || image.src;
                elements.detailImageModalImage.alt = `${product.name || "상품"} 확대 이미지`;
            }
        }, { once: true });
        elements.detailProductVisual.classList.add("product-visual--has-image");
        elements.detailProductVisual.prepend(image);
    }

    function renderSignals(product) {
        if (!elements.detailSignalList) {
            return;
        }
        const signals = [
            stockPressureDetail(product.stock),
            `${product.model} 모델 기준으로 ${product.category} 라인에 포함되며 무드 키워드는 ${product.mood || "Curated"}입니다.`,
            `${product.relatedProducts?.length || 0}개의 연관 상품과 ${product.options?.length || 0}개의 옵션 구성을 함께 확인할 수 있습니다.`
        ];
        elements.detailSignalList.innerHTML = signals.map((message, index) => `
            <article class="signal-card">
                <strong>Signal 0${index + 1}</strong>
                <span>${escapeMarkup(message)}</span>
            </article>
        `).join("");
    }

    function reviewStars(rating) {
        return "★".repeat(rating) + "☆".repeat(5 - rating);
    }

    function reviewMarkup(review) {
        return `
            <article class="detail-review-item">
                <div><strong>${escapeMarkup(review.reviewerName)}</strong><span>${reviewStars(review.rating)}</span></div>
                <p>${escapeMarkup(review.content)}</p>
                <footer><time>${escapeMarkup(review.createdDate)}</time>${review.reportedByMe ? '<span class="detail-review-reported">신고 완료</span>' : `<button type="button" data-review-report-id="${review.id}">신고</button>`}</footer>
            </article>
        `;
    }

    function normalizeReview(review) {
        const rating = detailInteger(review?.rating, "후기 평점", 1);
        if (rating > 5) throw new Error("후기 평점 정보가 올바르지 않습니다.");
        return {
            id: detailInteger(review?.id, "후기 번호", 1),
            reviewerName: detailText(review?.reviewerName, 40, true),
            rating,
            content: detailText(review?.content, 1000, true),
            createdDate: detailText(review?.createdDate, 30, true),
            reportedByMe: review?.reportedByMe === true
        };
    }

    function normalizeRatingDistribution(distribution) {
        if (!Array.isArray(distribution) || distribution.length !== 5) {
            throw new Error("후기 별점 분포 정보가 올바르지 않습니다.");
        }
        return distribution.map((count, index) => detailInteger(count, `${5 - index}점 후기 수`, 0));
    }

    function renderRatingDistribution(distribution, totalCount) {
        if (!elements.detailReviewDistribution) return;
        const maxCount = Math.max(1, ...distribution);
        elements.detailReviewDistribution.innerHTML = distribution.map((count, index) => {
            const rating = 5 - index;
            const percent = totalCount > 0 ? Math.round((count / maxCount) * 100) : 0;
            return `<div><span>${rating}점</span><i><b style="width:${percent}%"></b></i><strong>${count}</strong></div>`;
        }).join("");
    }

    async function loadReviews(page = 0, append = false) {
        if (!elements.detailReviewList || detailReviewState.loading) return;
        detailReviewState.loading = true;
        if (elements.detailReviewMoreButton) elements.detailReviewMoreButton.disabled = true;
        try {
            const response = await fetch(`/api/front/products/${productId}/reviews?page=${page}&sort=${encodeURIComponent(detailReviewState.sort)}`);
            if (!response.ok) throw new Error("구매 후기를 불러오지 못했습니다.");
            const payload = await response.json();
            if (!Array.isArray(payload?.reviews) || !Number.isSafeInteger(payload?.totalCount)
                    || !Number.isFinite(payload?.averageRating)) {
                throw new Error("구매 후기 응답이 올바르지 않습니다.");
            }
            const reviews = payload.reviews.map(normalizeReview);
            const ratingDistribution = normalizeRatingDistribution(payload.ratingDistribution);
            detailReviewState.page = Number.isSafeInteger(payload.page) ? payload.page : page;
            detailReviewState.hasNext = payload.hasNext === true;
            if (elements.detailReviewCount) elements.detailReviewCount.textContent = String(payload.totalCount);
            if (elements.detailReviewAverage) elements.detailReviewAverage.textContent = payload.totalCount ? `${payload.averageRating.toFixed(1)} / 5` : "-";
            if (elements.detailReviewAverageText) {
                elements.detailReviewAverageText.textContent = payload.totalCount
                    ? `${payload.totalCount}개의 배송 완료 구매 후기`
                    : "아직 등록된 후기가 없습니다.";
            }
            renderRatingDistribution(ratingDistribution, payload.totalCount);
            if (append) {
                elements.detailReviewList.insertAdjacentHTML("beforeend", reviews.map(reviewMarkup).join(""));
            } else {
                elements.detailReviewList.innerHTML = reviews.length
                    ? reviews.map(reviewMarkup).join("")
                    : '<article class="detail-review-empty"><strong>첫 구매 후기를 남겨주세요.</strong><p>배송 완료 주문 상품만 작성할 수 있습니다.</p></article>';
            }
            if (elements.detailReviewMoreButton) elements.detailReviewMoreButton.hidden = !detailReviewState.hasNext;
        } catch (error) {
            if (!append) {
                elements.detailReviewList.innerHTML = '<article class="detail-review-empty"><strong>구매 후기를 불러오지 못했습니다.</strong><p>잠시 후 다시 시도해주세요.</p></article>';
            }
        } finally {
            detailReviewState.loading = false;
            if (elements.detailReviewMoreButton) elements.detailReviewMoreButton.disabled = false;
        }
    }

    function setReviewFormOpen(open) {
        if (!elements.detailReviewForm) return;
        elements.detailReviewForm.hidden = !open;
        if (open) {
            updateReviewContentCount();
            loadEligibleReviewOrders();
        }
    }

    function updateReviewContentCount() {
        if (!elements.detailReviewContent || !elements.detailReviewContentCount) return;
        const length = elements.detailReviewContent.value.length;
        elements.detailReviewContentCount.textContent = `${length.toLocaleString("ko-KR")} / 1,000자`;
        elements.detailReviewContentCount.classList.toggle("is-limit", length >= 1000);
    }

    async function loadEligibleReviewOrders() {
        const orderNumberInput = document.getElementById("detailReviewOrderNumber");
        if (!orderNumberInput) return;
        orderNumberInput.disabled = true;
        orderNumberInput.replaceChildren(new Option("작성 가능한 주문을 확인 중입니다.", ""));
        try {
            const response = await fetch(`/api/front/products/${productId}/reviews/eligible-orders`, { headers: { Accept: "application/json" } });
            if (response.status === 401) {
                orderNumberInput.replaceChildren(new Option("로그인 후 주문을 선택할 수 있습니다.", ""));
                return;
            }
            if (!response.ok) throw new Error("작성 가능한 주문을 불러오지 못했습니다.");
            const orders = await response.json();
            if (!Array.isArray(orders)) throw new Error("작성 가능한 주문 응답이 올바르지 않습니다.");
            const options = orders.map((order) => {
                const orderNumber = detailText(order?.orderNumber, 50, true);
                return new Option(orderNumber, orderNumber, false, orderNumber === reviewOrderNumber);
            });
            orderNumberInput.replaceChildren(
                new Option(options.length ? "배송 완료 주문을 선택하세요." : "작성 가능한 주문이 없습니다.", ""),
                ...options
            );
            orderNumberInput.disabled = !options.length;
            if (options.length) orderNumberInput.focus();
        } catch (error) {
            orderNumberInput.replaceChildren(new Option("주문을 불러오지 못했습니다.", ""));
            showToast("작성 가능한 주문을 불러오지 못했습니다.", "잠시 후 다시 시도해주세요.", true);
        }
    }

    async function submitReview(event) {
        event.preventDefault();
        if (!elements.detailReviewForm || !elements.detailReviewSubmitButton) return;
        const form = new FormData(elements.detailReviewForm);
        const orderNumber = String(form.get("orderNumber") || "").trim();
        const rating = Number(form.get("rating"));
        const content = String(form.get("content") || "").trim();
        if (!orderNumber || orderNumber.length > 50 || !Number.isSafeInteger(rating) || rating < 1 || rating > 5 || !content) {
            showToast("후기 내용을 확인해주세요.", "주문 번호, 평점, 내용을 모두 입력해야 합니다.", true);
            return;
        }
        elements.detailReviewSubmitButton.disabled = true;
        try {
            const response = await fetch(`/api/front/products/${productId}/reviews`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ orderNumber, rating, content })
            });
            if (!response.ok) {
                const message = await response.text();
                throw new Error(message || "후기 등록을 처리하지 못했습니다.");
            }
            elements.detailReviewForm.reset();
            setReviewFormOpen(false);
            await loadReviews(0);
            showToast("구매 후기를 등록했습니다.", "배송 완료 주문 기준으로 후기 목록에 반영했습니다.");
        } catch (error) {
            showToast("후기를 등록하지 못했습니다.", error.message.replace(/<[^>]*>/g, "") || "로그인과 주문 상태를 확인해주세요.", true);
        } finally {
            elements.detailReviewSubmitButton.disabled = false;
        }
    }

    function openReportModal(reviewId) {
        if (!Number.isSafeInteger(reviewId) || reviewId < 1 || !elements.detailReportModal || !elements.detailReportForm) return;
        closeDetailImageModal();
        reportingReviewId = reviewId;
        detailReportModalReturnFocus = document.activeElement;
        elements.detailReportForm.reset();
        elements.detailReportModal.classList.add("is-open");
        elements.detailReportModal.setAttribute("aria-hidden", "false");
        setDetailModalBackgroundInert(true);
        document.body.classList.add("has-open-modal");
        elements.detailReportReason?.focus();
    }

    function closeReportModal() {
        if (!elements.detailReportModal?.classList.contains("is-open")) return;
        elements.detailReportModal.classList.remove("is-open");
        elements.detailReportModal.setAttribute("aria-hidden", "true");
        setDetailModalBackgroundInert(false);
        document.body.classList.remove("has-open-modal");
        reportingReviewId = null;
        if (detailReportModalReturnFocus?.isConnected) detailReportModalReturnFocus.focus();
        detailReportModalReturnFocus = null;
    }

    async function submitReviewReport(event) {
        event.preventDefault();
        const reviewId = reportingReviewId;
        const reason = elements.detailReportReason?.value.trim() || "";
        const detail = elements.detailReportDetail?.value.trim() || "";
        if (!Number.isSafeInteger(reviewId) || reviewId < 1 || !reason) {
            showToast("신고 사유를 선택해주세요.", "신고 이유를 선택한 뒤 다시 시도해주세요.", true);
            return;
        }
        elements.detailReportSubmitButton.disabled = true;
        try {
            const response = await fetch(`/api/front/products/${productId}/reviews/${reviewId}/reports`, {
                method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ reason, detail })
            });
            if (!response.ok) throw new Error((await response.text()) || "후기를 신고하지 못했습니다.");
            closeReportModal();
            await loadReviews(0);
            showToast("후기를 신고했습니다.", "운영자가 신고 내용을 확인합니다.");
        } catch (error) {
            showToast("후기를 신고하지 못했습니다.", error.message.replace(/<[^>]*>/g, ""), true);
        } finally {
            elements.detailReportSubmitButton.disabled = false;
        }
    }

    function renderOverview(product) {
        if (!elements.detailOverviewGrid) {
            return;
        }
        const items = [
            ["브랜드", product.brand],
            ["카테고리", product.category],
            ["모델", product.model],
            ["등록일", product.createdDate],
            ["무드", product.mood],
            ["대표 노출", product.featured ? (product.featuredRank ? `Featured #${product.featuredRank}` : "Featured") : "일반"]
        ];
        elements.detailOverviewGrid.innerHTML = items.map(([label, value]) => `
            <article class="detail-info-card">
                <span>${label}</span>
                <strong>${escapeMarkup(value || "-")}</strong>
            </article>
        `).join("");
    }

    function renderOptions(product) {
        if (!elements.detailOptionGrid) {
            return;
        }
        syncOptionSortButtons();
        const options = sortedOptions(product);
        const allOptions = Array.isArray(product.options) ? product.options : [];
        const availableOptions = allOptions.filter((option) => Number(option.stock || 0) > 0);
        const availableRate = allOptions.length ? Math.round((availableOptions.length / allOptions.length) * 100) : 0;
        setElementText(elements.detailAvailableOptionCount, String(availableOptions.length));
        setElementText(elements.detailLowOptionCount, String(allOptions.filter((option) => Number(option.stock || 0) > 0 && Number(option.stock || 0) < lowStockThreshold()).length));
        setElementText(elements.detailSoldOutOptionCount, String(allOptions.filter((option) => Number(option.stock || 0) <= 0).length));
        setElementText(elements.detailTotalOptionStock, String(allOptions.reduce((sum, option) => sum + Number(option.stock || 0), 0)));
        const optionStocks = allOptions.map((option) => Number(option.stock || 0)).sort((left, right) => left - right);
        const medianStock = optionStocks.length
            ? optionStocks.length % 2
                ? optionStocks[Math.floor(optionStocks.length / 2)]
                : Math.round((optionStocks[(optionStocks.length / 2) - 1] + optionStocks[optionStocks.length / 2]) / 2)
            : 0;
        const rankedOptions = allOptions.slice().sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0));
        const selectedRank = selectedOptionName
            ? rankedOptions.findIndex((option) => option.name === selectedOptionName) + 1
            : 0;
        setElementText(elements.detailMinOptionStock, String(optionStocks[0] || 0));
        setElementText(elements.detailMaxOptionStock, String(optionStocks[optionStocks.length - 1] || 0));
        setElementText(elements.detailMedianOptionStock, String(medianStock));
        setElementText(elements.detailSelectedOptionRank, selectedRank ? `${selectedRank} / ${allOptions.length}` : "-");
        const totalOptionStock = optionStocks.reduce((sum, stock) => sum + stock, 0);
        const optionDistribution = [
            [elements.detailOptionSoldOutRate, elements.detailOptionSoldOutBar, allOptions.filter((option) => Number(option.stock || 0) <= 0).length, allOptions.length],
            [elements.detailOptionCriticalRate, elements.detailOptionCriticalBar, allOptions.filter((option) => Number(option.stock || 0) > 0 && Number(option.stock || 0) <= 5).length, allOptions.length],
            [elements.detailOptionLowRate, elements.detailOptionLowBar, allOptions.filter((option) => Number(option.stock || 0) > 5 && Number(option.stock || 0) < lowStockThreshold()).length, allOptions.length],
            [elements.detailOptionStableRate, elements.detailOptionStableBar, allOptions.filter((option) => Number(option.stock || 0) >= lowStockThreshold()).length, allOptions.length],
            [elements.detailOptionConcentrationRate, elements.detailOptionConcentrationBar, optionStocks[optionStocks.length - 1] || 0, totalOptionStock]
        ];
        optionDistribution.forEach(([label, bar, value, total]) => {
            const rate = total ? Math.round((value / total) * 100) : 0;
            setElementText(label, `${rate}%`);
            if (bar) {
                bar.style.width = `${rate}%`;
            }
        });
        setElementText(elements.detailOptionStockRateText, `${availableRate}%`);
        if (elements.detailOptionStockRateBar) {
            elements.detailOptionStockRateBar.style.width = `${availableRate}%`;
        }
        if (elements.detailRecommendOptionButton) {
            elements.detailRecommendOptionButton.disabled = !allOptions.some((option) => Number(option.stock || 0) > 0);
        }
        if (elements.detailCopyOptionMatrixButton) {
            elements.detailCopyOptionMatrixButton.disabled = allOptions.length === 0;
        }
        if (elements.detailOptionCount) {
            elements.detailOptionCount.textContent = String(options.length);
        }
        renderOptionHistory();
        if (selectedOptionName && !options.some((option) => option.name === selectedOptionName)) {
            selectedOptionName = "";
            syncSelectedOptionActions({});
        }
        if (!options.length) {
            elements.detailOptionGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>등록된 옵션이 없습니다.</strong>
                    <p>현재 상품에는 사이즈별 재고 정보가 없습니다.</p>
                </article>
            `;
            return;
        }
        const firstAvailableName = options.find((option) => Number(option.stock || 0) > 0)?.name;
        elements.detailOptionGrid.innerHTML = options.map((option) => `
            <button class="detail-option-card ${selectedOptionName === option.name ? "is-selected" : ""}" type="button" role="radio" data-detail-option="${escapeAttribute(option.name)}" aria-checked="${selectedOptionName === option.name}" tabindex="${selectedOptionName === option.name || (!selectedOptionName && firstAvailableName === option.name) ? "0" : "-1"}" ${Number(option.stock || 0) <= 0 ? "disabled" : ""}>
                <span>${escapeMarkup(option.name)}</span>
                <strong>${option.stock}개</strong>
                <em class="${stockClassName(option.stock)}">${stockLabel(option.stock)}</em>
            </button>
        `).join("");
    }

    async function copyOptionStockMatrix() {
        const options = currentProduct?.options || [];
        const text = options.length
            ? [`${currentProduct.name} 옵션 재고`, ...options.map((option, index) => `${index + 1}. ${option.name} · ${option.stock}개 · ${stockLabel(option.stock)}`)].join("\n")
            : "등록된 옵션 재고가 없습니다.";
        await copyText(text, "옵션 재고 행렬을 복사했습니다.");
    }

    function selectDetailOption(optionName) {
        const option = currentProduct?.options?.find((item) => item.name === optionName);
        if (!option || Number(option.stock || 0) <= 0) {
            return;
        }
        selectedOptionName = selectedOptionName === optionName ? "" : optionName;
        if (selectedOptionName) {
            recordOptionSelection(option);
        }
        rememberSelectedOption(productId, selectedOptionName);
        syncSelectedOptionUrl();
        selectedQuantity = selectedOptionName ? readSelectedQuantity() : 1;
        renderOptions(currentProduct);
        syncSelectedOptionActions(option);
    }

    function availableDetailOptions() {
        return (currentProduct?.options || []).filter((option) => Number(option.stock || 0) > 0);
    }

    function moveDetailOption(direction) {
        const available = availableDetailOptions();
        if (!available.length) {
            return;
        }
        const currentIndex = available.findIndex((option) => option.name === selectedOptionName);
        const nextIndex = currentIndex < 0
            ? (direction > 0 ? 0 : available.length - 1)
            : (currentIndex + direction + available.length) % available.length;
        selectDetailOption(available[nextIndex].name);
    }

    function recordOptionSelection(option) {
        const previousIndex = optionSelectionHistory.findIndex((item) => item.name === option.name);
        if (previousIndex >= 0) {
            optionSelectionHistory.splice(previousIndex, 1);
        }
        optionSelectionHistory.push({ name: option.name, stock: Number(option.stock || 0) });
        if (optionSelectionHistory.length > 5) {
            optionSelectionHistory.shift();
        }
        renderOptionHistory();
    }

    function renderOptionHistory() {
        const available = availableDetailOptions();
        setElementText(elements.detailOptionHistoryCount, String(optionSelectionHistory.length));
        [elements.detailPreviousOptionButton, elements.detailNextOptionButton]
            .forEach((button) => button?.toggleAttribute("disabled", available.length === 0));
        elements.detailCopyOptionHistoryButton?.toggleAttribute("disabled", optionSelectionHistory.length === 0);
        elements.detailClearOptionHistoryButton?.toggleAttribute("disabled", optionSelectionHistory.length === 0);
        if (!elements.detailOptionHistoryList) {
            return;
        }
        elements.detailOptionHistoryList.innerHTML = optionSelectionHistory.length
            ? optionSelectionHistory.slice().reverse().map((item) => `
                <button type="button" data-option-history-name="${escapeAttribute(item.name)}" aria-pressed="${selectedOptionName === item.name}">
                    <strong>${escapeAttribute(item.name)}</strong><span>${item.stock}개</span>
                </button>
            `).join("")
            : "<span>선택한 옵션이 여기에 표시됩니다.</span>";
    }

    async function copyOptionHistory() {
        const lines = optionSelectionHistory.slice().reverse().map((item, index) => `${index + 1}. ${item.name} · 재고 ${item.stock}개`);
        await copyText([`${currentProduct?.name || "상품"} 옵션 탐색`, ...lines].join("\n"), "옵션 탐색 이력을 복사했습니다.");
    }

    function handleDetailOptionNavigation(event) {
        if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Home", "End"].includes(event.key)) {
            return;
        }
        const buttons = Array.from(elements.detailOptionGrid.querySelectorAll("[data-detail-option]:not(:disabled)"));
        const currentIndex = buttons.indexOf(event.target.closest("[data-detail-option]"));
        if (currentIndex < 0 || !buttons.length) {
            return;
        }
        const nextIndex = event.key === "Home"
            ? 0
            : event.key === "End"
                ? buttons.length - 1
                : (currentIndex + (["ArrowRight", "ArrowDown"].includes(event.key) ? 1 : -1) + buttons.length) % buttons.length;
        event.preventDefault();
        const nextButton = buttons[nextIndex];
        nextButton.focus();
        if (nextButton.dataset.detailOption !== selectedOptionName) {
            selectDetailOption(nextButton.dataset.detailOption);
            Array.from(elements.detailOptionGrid.querySelectorAll("[data-detail-option]"))
                .find((button) => button.dataset.detailOption === nextButton.dataset.detailOption)?.focus();
        }
    }

    function selectedOptionSummary() {
        const option = currentProduct?.options?.find((item) => item.name === selectedOptionName);
        if (!currentProduct || !option) {
            return "";
        }
        return `${currentProduct.name} · ${option.name} · 재고 ${option.stock}개 · ${formatPrice(optionUnitPrice(option))}`;
    }

    function optionUnitPrice(option) {
        return Number(currentProduct?.price || 0) + Number(option?.additionalPrice || 0);
    }

    function readQuantityMemory() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(DETAIL_QUANTITY_KEY) || "{}");
            if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
            return Object.fromEntries(Object.entries(parsed).filter(([key, value]) =>
                /^\d+:.{1,100}$/.test(key) && Number.isSafeInteger(value) && value >= 1 && value <= 20
            ).slice(-100));
        } catch (error) {
            return {};
        }
    }

    function readSelectedQuantity() {
        try {
            const quantities = readQuantityMemory();
            return Math.max(1, Number(quantities?.[`${productId}:${selectedOptionName}`]) || 1);
        } catch (error) {
            return 1;
        }
    }

    function setSelectedQuantity(nextQuantity, announce = true) {
        const option = currentProduct?.options?.find((item) => item.name === selectedOptionName);
        if (!option) {
            return;
        }
        const maxQuantity = Math.min(20, Math.max(1, Number(option.stock || 0)));
        selectedQuantity = Math.min(maxQuantity, Math.max(1, Math.trunc(Number(nextQuantity) || 1)));
        try {
            const quantities = readQuantityMemory();
            quantities[`${productId}:${selectedOptionName}`] = selectedQuantity;
            window.localStorage.setItem(DETAIL_QUANTITY_KEY, JSON.stringify(quantities));
        } catch (error) {
            // 저장 공간이 제한된 환경에서도 현재 화면의 수량 계산은 유지한다.
        }
        syncPurchaseEstimate(option);
        if (announce) {
            setElementText(elements.detailStatus, `수량 ${selectedQuantity}개, 예상 상품 금액 ${formatPrice(optionUnitPrice(option) * selectedQuantity)}`);
        }
    }

    function syncPurchaseEstimate(option) {
        const selected = Boolean(selectedOptionName && option?.name);
        if (elements.detailPurchaseEstimate) {
            elements.detailPurchaseEstimate.hidden = !selected;
        }
        if (!selected) {
            return;
        }
        const maxQuantity = Math.min(20, Math.max(1, Number(option.stock || 0)));
        selectedQuantity = Math.min(maxQuantity, Math.max(1, selectedQuantity));
        if (elements.detailQuantityInput) {
            elements.detailQuantityInput.max = String(maxQuantity);
            elements.detailQuantityInput.value = String(selectedQuantity);
        }
        if (elements.detailQuantityDecreaseButton) {
            elements.detailQuantityDecreaseButton.disabled = selectedQuantity <= 1;
        }
        if (elements.detailQuantityIncreaseButton) {
            elements.detailQuantityIncreaseButton.disabled = selectedQuantity >= maxQuantity;
        }
        setElementText(elements.detailEstimatedTotal, formatPrice(optionUnitPrice(option) * selectedQuantity));
        setElementText(elements.detailUnitPrice, formatPrice(optionUnitPrice(option)));
        setElementText(elements.detailRemainingStock, `${Math.max(0, maxQuantity - selectedQuantity)}개`);
        const stockUsageRate = Math.min(100, Math.round((selectedQuantity / maxQuantity) * 100));
        const remainingRate = Math.max(0, 100 - stockUsageRate);
        const productStockUsageRate = Math.min(100, Math.round((selectedQuantity / Math.max(1, Number(currentProduct.stock || 0))) * 100));
        const safeQuantity = Math.max(1, Math.floor(maxQuantity * 0.2));
        const purchaseUrgency = maxQuantity <= 5 ? "즉시 확인" : maxQuantity < lowStockThreshold() ? "재고 긴장" : "재고 안정";
        setElementText(elements.detailOptionRemainingRate, `${remainingRate}%`);
        setElementText(elements.detailProductStockUsageRate, `${productStockUsageRate}%`);
        setElementText(elements.detailSafeQuantity, `${safeQuantity}개`);
        setElementText(elements.detailPurchaseUrgency, purchaseUrgency);
        setElementText(elements.detailQuantityStatus, selectedQuantity > safeQuantity ? "대량 선택" : "권장 범위");
        setElementText(elements.detailStockUsageRate, `${stockUsageRate}%`);
        if (elements.detailStockUsageBar) {
            elements.detailStockUsageBar.style.width = `${stockUsageRate}%`;
        }
        setElementText(
            elements.detailQuantityNotice,
            stockUsageRate >= 50
                ? `현재 옵션 재고의 ${stockUsageRate}%를 선택했습니다. 수량을 다시 확인해주세요.`
                : `선택 후 ${Math.max(0, maxQuantity - selectedQuantity)}개가 남습니다.`
        );
        [
            [elements.detailQuantityPresetTwoButton, 2],
            [elements.detailQuantityPresetThreeButton, 3],
            [elements.detailQuantityPresetFiveButton, 5]
        ].forEach(([button, quantity]) => {
            if (button) {
                button.disabled = maxQuantity < quantity;
                button.classList.toggle("is-active", selectedQuantity === quantity);
                button.setAttribute("aria-pressed", String(selectedQuantity === quantity));
            }
        });
        if (elements.detailQuantityMaxButton) {
            elements.detailQuantityMaxButton.disabled = selectedQuantity >= maxQuantity;
        }
        if (elements.detailQuantityResetButton) {
            elements.detailQuantityResetButton.disabled = selectedQuantity <= 1;
        }
    }

    async function copyOrderSummary() {
        const optionSummary = selectedOptionSummary();
        if (!optionSummary) {
            return;
        }
        const option = currentProduct?.options?.find((item) => item.name === selectedOptionName);
        const total = formatPrice(optionUnitPrice(option) * selectedQuantity);
        await copyText(`${optionSummary}\n수량 ${selectedQuantity}개 · 예상 상품 금액 ${total}`, "주문 요약을 복사했습니다.");
    }

    function cartToken() {
        const key = "grade-stock-cart-token";
        try {
            let token = window.localStorage.getItem(key);
            if (!/^[A-Za-z0-9-]{16,80}$/.test(String(token || ""))) {
                token = createCartToken();
                window.localStorage.setItem(key, token);
            }
            return token;
        } catch (error) {
            memoryCartToken ||= createCartToken();
            return memoryCartToken;
        }
    }

    function createCartToken() {
        return window.crypto?.randomUUID?.() || `cart-${Date.now()}-${Math.random().toString(16).slice(2)}`;
    }

    function cartInteger(value, fieldName, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }

    function normalizeCartResponse(payload, option) {
        if (!payload || !Array.isArray(payload.items) || payload.items.length === 0) throw new Error("장바구니 응답이 올바르지 않습니다.");
        const items = payload.items.map((item) => {
            const quantity = cartInteger(item?.quantity, "장바구니 수량", 1);
            const unitPrice = cartInteger(item?.unitPrice, "장바구니 단가");
            const lineAmount = cartInteger(item?.lineAmount, "장바구니 합계");
            if (quantity > 20 || lineAmount !== unitPrice * quantity) throw new Error("장바구니 상품 합계가 올바르지 않습니다.");
            return {
                productId: cartInteger(item.productId, "상품 번호", 1),
                optionId: cartInteger(item.optionId, "옵션 번호", 1),
                quantity, unitPrice, lineAmount
            };
        });
        const totalQuantity = cartInteger(payload.totalQuantity, "장바구니 총수량");
        const totalAmount = cartInteger(payload.totalAmount, "장바구니 총액");
        const target = items.find((item) => item.productId === currentProduct.id && item.optionId === option.id);
        if (!target || target.quantity < selectedQuantity || target.unitPrice !== optionUnitPrice(option)
            || totalQuantity !== items.reduce((sum, item) => sum + item.quantity, 0)
            || totalAmount !== items.reduce((sum, item) => sum + item.lineAmount, 0)) {
            throw new Error("장바구니 합계가 요청한 상품과 일치하지 않습니다.");
        }
        return { totalQuantity, totalAmount };
    }

    function setCartSubmitting(submitting, targetButton) {
        cartSubmitting = submitting;
        [elements.detailAddCartButton, elements.detailBuyNowButton].forEach((button) => {
            button?.toggleAttribute("disabled", submitting);
            button?.removeAttribute("aria-busy");
        });
        targetButton?.toggleAttribute("aria-busy", submitting);
    }

    async function addSelectedOptionToCart(moveToCheckout = false) {
        if (cartSubmitting) {
            return;
        }
        const option = currentProduct?.options?.find((item) => item.name === selectedOptionName);
        if (!currentProduct || !option?.id) {
            focusDetailOptions();
            showToast("구매 옵션을 선택해주세요.", "재고가 있는 옵션을 선택한 뒤 다시 시도해주세요.", true);
            return;
        }
        const targetButton = moveToCheckout ? elements.detailBuyNowButton : elements.detailAddCartButton;
        setCartSubmitting(true, targetButton);
        try {
            const response = await fetch("/api/front/cart/items", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "X-Cart-Token": cartToken()
                },
                body: JSON.stringify({
                    productId: currentProduct.id,
                    optionId: option.id,
                    quantity: selectedQuantity
                })
            });
            const payload = await response.json().catch(() => ({}));
            if (!response.ok) {
                throw new Error(payload.message || "장바구니에 담지 못했습니다.");
            }
            const cart = normalizeCartResponse(payload, option);
            if (moveToCheckout) {
                window.location.href = "/front/checkout";
                return;
            }
            showToast("장바구니에 담았습니다.", `${selectedOptionName} · ${selectedQuantity}개`);
            setElementText(elements.detailStatus, `장바구니 ${cart.totalQuantity}개, 총 ${formatPrice(cart.totalAmount)}`);
        } catch (error) {
            showToast("장바구니 요청을 처리하지 못했습니다.", error.message, true);
        } finally {
            setCartSubmitting(false, targetButton);
        }
    }

    async function shareSelectedOption() {
        const text = selectedOptionSummary();
        if (!text) {
            return;
        }
        const url = `${window.location.origin}${window.location.pathname}${window.location.search}`;
        try {
            if (navigator.share) {
                await navigator.share({ title: `${currentProduct.name} ${selectedOptionName}`, text, url });
                showToast("선택 옵션을 공유했습니다.", `${selectedOptionName} 옵션 정보를 전달했습니다.`);
                return;
            }
            await copyText(`${text}\n${url}`, "선택 옵션과 URL을 복사했습니다.");
        } catch (error) {
            if (error?.name !== "AbortError") {
                window.prompt("선택 옵션을 복사하세요.", `${text}\n${url}`);
            }
        }
    }

    function syncSelectedOptionActions(option) {
        const selected = Boolean(selectedOptionName);
        if (elements.detailPrimaryAction) {
            elements.detailPrimaryAction.textContent = selected ? `${selectedOptionName} 옵션 선택됨` : "구매 옵션 확인";
        }
        const mobileLabel = elements.detailMobilePrimaryButton?.querySelector("strong");
        if (mobileLabel) {
            mobileLabel.textContent = selected ? `${selectedOptionName} 선택됨` : "옵션 확인";
        }
        elements.detailMobilePrimaryButton?.classList.toggle("has-option", selected);
        if (elements.detailOptionSelection) {
            elements.detailOptionSelection.hidden = !selected;
        }
        if (elements.detailOptionSelectionText) {
            elements.detailOptionSelectionText.textContent = selected ? `${selectedOptionName} · 재고 ${option.stock}개` : "";
        }
        if (selected) {
            showToast("옵션을 선택했습니다.", `${selectedOptionName} · 재고 ${option.stock}개`);
        }
        syncPurchaseEstimate(option);
    }

    function renderRelated(product) {
        if (!elements.detailRelatedGrid) {
            return;
        }
        const related = sortedRelatedProducts(product);
        if (elements.detailPreviousRelatedButton) {
            elements.detailPreviousRelatedButton.disabled = !related.length;
        }
        if (elements.detailNextRelatedButton) {
            elements.detailNextRelatedButton.disabled = !related.length;
        }
        if (elements.detailRelatedCount) {
            elements.detailRelatedCount.textContent = String(related.length);
        }
        setElementText(
            elements.detailRelatedAveragePrice,
            related.length ? formatPrice(Math.round(related.reduce((sum, item) => sum + Number(item.price || 0), 0) / related.length)) : "-"
        );
        const relatedPrices = related.map((item) => Number(item.price || 0));
        setElementText(elements.detailRelatedMinPrice, relatedPrices.length ? formatPrice(Math.min(...relatedPrices)) : "-");
        setElementText(elements.detailRelatedMaxPrice, relatedPrices.length ? formatPrice(Math.max(...relatedPrices)) : "-");
        setElementText(elements.detailRelatedPriceSpread, relatedPrices.length ? formatPrice(Math.max(...relatedPrices) - Math.min(...relatedPrices)) : "-");
        setElementText(elements.detailRelatedTotalStock, `${related.reduce((sum, item) => sum + Number(item.stock || 0), 0)}개`);
        setElementText(elements.detailRelatedCheaperCount, `${related.filter((item) => Number(item.price || 0) < Number(product.price || 0)).length}개`);
        setElementText(elements.detailRelatedHigherStockCount, `${related.filter((item) => Number(item.stock || 0) > Number(product.stock || 0)).length}개`);
        setElementText(elements.detailRelatedSoldOutCount, `${related.filter((item) => Number(item.stock || 0) <= 0).length}개`);
        const maxSaving = related.reduce((max, item) => Math.max(max, Number(product.price || 0) - Number(item.price || 0)), 0);
        const maxStockGain = related.reduce((max, item) => Math.max(max, Number(item.stock || 0) - Number(product.stock || 0)), 0);
        const availableRate = related.length
            ? Math.round((related.filter((item) => Number(item.stock || 0) > 0).length / related.length) * 100)
            : 0;
        setElementText(elements.detailRelatedMaxSaving, formatPrice(maxSaving));
        setElementText(elements.detailRelatedMaxStockGain, `${maxStockGain}개`);
        setElementText(elements.detailRelatedAvailableRate, `${availableRate}%`);
        setElementText(elements.detailRelatedSameBrandCount, `${related.filter((item) => item.brand === product.brand).length}개`);
        const cheaperProducts = related.filter((item) => Number(item.price || 0) < Number(product.price || 0));
        const averageSavingRate = cheaperProducts.length && Number(product.price || 0)
            ? Math.round((cheaperProducts.reduce((sum, item) => sum + (Number(product.price || 0) - Number(item.price || 0)), 0) / cheaperProducts.length / Number(product.price || 0)) * 100)
            : 0;
        const relatedDistribution = [
            [elements.detailRelatedCheaperRate, elements.detailRelatedCheaperBar, cheaperProducts.length],
            [elements.detailRelatedStockAdvantageRate, elements.detailRelatedStockAdvantageBar, related.filter((item) => Number(item.stock || 0) > Number(product.stock || 0)).length],
            [elements.detailRelatedSameBrandRate, elements.detailRelatedSameBrandBar, related.filter((item) => item.brand === product.brand).length],
            [elements.detailRelatedSameCategoryRate, elements.detailRelatedSameCategoryBar, related.filter((item) => item.category === product.category).length]
        ];
        relatedDistribution.forEach(([label, bar, count]) => {
            const rate = related.length ? Math.round((count / related.length) * 100) : 0;
            setElementText(label, `${rate}%`);
            if (bar) {
                bar.style.width = `${rate}%`;
            }
        });
        setElementText(elements.detailRelatedAverageSavingRate, `${averageSavingRate}%`);
        if (elements.detailRelatedAverageSavingBar) {
            elements.detailRelatedAverageSavingBar.style.width = `${Math.min(100, averageSavingRate)}%`;
        }
        if (elements.detailCompareAllRelatedButton) {
            elements.detailCompareAllRelatedButton.disabled = !related.length;
        }
        [elements.detailCheapestRelatedButton, elements.detailHighestStockRelatedButton, elements.detailBalancedRelatedButton, elements.detailCopyPriceComparisonButton, elements.detailCopyValueAnalysisButton, elements.detailBookmarkAllRelatedButton, elements.detailCopyRelatedLinksButton]
            .forEach((button) => {
                if (button) {
                    button.disabled = !related.length;
                }
            });
        if (!related.length) {
            elements.detailRelatedGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>연관 상품이 없습니다.</strong>
                    <p>현재 상품 기준으로 추천 가능한 비교 상품이 없습니다.</p>
                </article>
            `;
            return;
        }
        elements.detailRelatedGrid.innerHTML = related.map((item, index) => {
            item = markupSafeObject(item);
            return `
            <article class="detail-related-card saved-product-card" role="listitem" data-related-product-id="${item.id}" tabindex="${index === 0 ? "0" : "-1"}" aria-label="${escapeAttribute(`${item.name}, ${item.priceLabel || formatPrice(item.price)}, 재고 ${item.stock}개`)}">
                ${productVisualMarkup(item, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${item.brand}</span>
                <strong>${item.name}</strong>
                <p>${item.reason} · ${stockPressureDetail(item.stock)}</p>
                <div class="detail-related-card__comparison" aria-label="현재 상품 비교 정보">
                    <span>${item.category || "카테고리 미정"}</span>
                    <span>${relatedPriceDeltaRateLabel(item.price, product.price)}</span>
                    <span>${relatedStockDeltaLabel(item.stock, product.stock)}</span>
                    <strong>가치 ${relatedValueScore(item, product, related)}점</strong>
                    <em>${index + 1} / ${related.length}</em>
                </div>
                <div class="detail-related-card__meta">
                    <span>모델 ${item.model}</span>
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span>${relatedPriceDeltaLabel(item.price, product.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                </div>
                <div class="detail-related-card__actions saved-product-card__actions">
                    <a href="${buildProductUrl(item.id)}">상세 보기</a>
                    <details class="saved-product-card__menu">
                        <summary aria-label="연관 상품 추가 작업">•••</summary>
                        <div>
                            <button class="${isComparedProduct(item.id) ? "is-active" : ""}" type="button" data-related-compare-id="${item.id}">${isComparedProduct(item.id) ? "비교 해제" : "비교 담기"}</button>
                            <button class="${isBookmarkedProduct(item.id) ? "is-active" : ""}" type="button" data-related-bookmark-id="${item.id}">${isBookmarkedProduct(item.id) ? "관심 해제" : "관심 상품 추가"}</button>
                            <button type="button" data-related-copy-id="${item.id}">요약 복사</button>
                        </div>
                    </details>
                </div>
            </article>
        `;
        }).join("");
        bindRelatedCardActions(related);
        syncRelatedButtons();
    }

    function syncCatalogLinks() {
        const catalogUrl = buildCatalogUrl();
        if (elements.backToCatalogLink) {
            elements.backToCatalogLink.href = catalogUrl;
        }
        if (elements.detailCatalogLink) {
            elements.detailCatalogLink.href = `${catalogUrl}#catalog`;
        }
    }

    function relatedPriceDeltaLabel(relatedPrice, basePrice) {
        const delta = Number(relatedPrice || 0) - Number(basePrice || 0);
        if (!delta) {
            return "현재 상품과 동일가";
        }
        return `현재 상품보다 ${formatPrice(Math.abs(delta))} ${delta > 0 ? "높음" : "낮음"}`;
    }

    function relatedPriceDeltaRateLabel(relatedPrice, basePrice) {
        const base = Number(basePrice || 0);
        const delta = Number(relatedPrice || 0) - base;
        if (!base || !delta) {
            return "가격차 0%";
        }
        return `가격차 ${delta > 0 ? "+" : ""}${Math.round((delta / base) * 100)}%`;
    }

    function relatedStockDeltaLabel(relatedStock, baseStock) {
        const delta = Number(relatedStock || 0) - Number(baseStock || 0);
        return `재고 ${delta > 0 ? "+" : ""}${delta}개`;
    }

    function relatedValueScore(item, baseProduct, related) {
        const basePrice = Number(baseProduct.price || 0);
        const price = Number(item.price || 0);
        const maxStock = Math.max(1, ...related.map((relatedItem) => Number(relatedItem.stock || 0)));
        const priceScore = basePrice
            ? Math.max(0, Math.min(100, 50 + (((basePrice - price) / basePrice) * 50)))
            : 50;
        const stockScore = Math.max(0, Number(item.stock || 0)) / maxStock * 100;
        return Math.round((priceScore * 0.6) + (stockScore * 0.4));
    }

    function handleRelatedCardNavigation(event) {
        const card = event.target.closest("#detailRelatedGrid [role=\"listitem\"]");
        if (!card || event.target !== card) {
            return;
        }
        if (event.key === "Enter") {
            const link = card.querySelector("a[href]");
            if (link) {
                event.preventDefault();
                window.location.href = link.href;
            }
            return;
        }
        if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Home", "End"].includes(event.key)) {
            return;
        }
        const cards = Array.from(elements.detailRelatedGrid.querySelectorAll("[role=\"listitem\"]"));
        const currentIndex = cards.indexOf(card);
        const columnCount = Math.max(1, window.getComputedStyle(elements.detailRelatedGrid).gridTemplateColumns.split(" ").length);
        const offsets = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -columnCount, ArrowDown: columnCount };
        const nextIndex = event.key === "Home"
            ? 0
            : event.key === "End"
                ? cards.length - 1
                : Math.min(cards.length - 1, Math.max(0, currentIndex + offsets[event.key]));
        event.preventDefault();
        cards.forEach((item, index) => item.tabIndex = index === nextIndex ? 0 : -1);
        cards[nextIndex]?.focus();
        const item = sortedRelatedProducts(currentProduct).find((product) => Number(product.id) === Number(cards[nextIndex]?.dataset.relatedProductId));
        setElementText(elements.detailStatus, `${nextIndex + 1} / ${cards.length}번째 ${item?.name || "연관 상품"}입니다.`);
    }

    async function copyRelatedPriceComparison() {
        const related = sortedRelatedProducts(currentProduct);
        if (!currentProduct || !related.length) {
            return;
        }
        const cheapest = related.slice().sort((left, right) => Number(left.price || 0) - Number(right.price || 0))[0];
        const highest = related.slice().sort((left, right) => Number(right.price || 0) - Number(left.price || 0))[0];
        const text = [
            `현재 상품 ${currentProduct.name} · ${currentProduct.priceLabel || formatPrice(currentProduct.price)}`,
            `연관 최저가 ${cheapest.name} · ${cheapest.priceLabel || formatPrice(cheapest.price)} · ${relatedPriceDeltaLabel(cheapest.price, currentProduct.price)}`,
            `연관 최고가 ${highest.name} · ${highest.priceLabel || formatPrice(highest.price)} · ${relatedPriceDeltaLabel(highest.price, currentProduct.price)}`
        ].join("\n");
        await copyText(text, "연관 가격 비교를 복사했습니다.");
    }

    async function copyRelatedValueAnalysis() {
        const related = sortedRelatedProducts(currentProduct);
        if (!currentProduct || !related.length) {
            return;
        }
        const maxSaving = related.reduce((max, item) => Math.max(max, Number(currentProduct.price || 0) - Number(item.price || 0)), 0);
        const maxStockGain = related.reduce((max, item) => Math.max(max, Number(item.stock || 0) - Number(currentProduct.stock || 0)), 0);
        const availableRate = Math.round((related.filter((item) => Number(item.stock || 0) > 0).length / related.length) * 100);
        const text = [
            `${currentProduct.name} 연관 가치 분석`,
            `비교 상품 ${related.length}개 · 구매 가능률 ${availableRate}%`,
            `최대 절감 ${formatPrice(maxSaving)} · 최대 재고 증가 ${maxStockGain}개`,
            `동일 브랜드 ${related.filter((item) => item.brand === currentProduct.brand).length}개`
        ].join("\n");
        await copyText(text, "연관 상품 가치 분석을 복사했습니다.");
    }

    function openRelatedByMetric(metric) {
        const related = sortedRelatedProducts(currentProduct);
        if (!related.length) {
            return;
        }
        const target = related.slice().sort(metric === "PRICE_LOW"
            ? (left, right) => Number(left.price || 0) - Number(right.price || 0)
            : (left, right) => Number(right.stock || 0) - Number(left.stock || 0))[0];
        window.location.href = buildProductUrl(target.id);
    }

    function openBalancedRelatedProduct() {
        const available = sortedRelatedProducts(currentProduct).filter((item) => Number(item.stock || 0) > 0);
        if (!available.length) {
            showToast("균형 추천 상품이 없습니다.", "구매 가능한 연관 상품 조건을 확인해주세요.", true);
            return;
        }
        const target = available.slice().sort((left, right) => {
            const leftScore = Number(left.price || 0) / Math.max(1, Number(left.stock || 0));
            const rightScore = Number(right.price || 0) / Math.max(1, Number(right.stock || 0));
            return leftScore - rightScore;
        })[0];
        window.location.href = buildProductUrl(target.id);
    }

    function readRememberedOptions() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(SELECTED_OPTION_KEY) || "{}");
            return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? parsed : {};
        } catch (error) {
            return {};
        }
    }

    function rememberSelectedOption(currentProductId, optionName) {
        const remembered = readRememberedOptions();
        if (optionName) {
            remembered[currentProductId] = optionName;
        } else {
            delete remembered[currentProductId];
        }
        try {
            window.localStorage.setItem(SELECTED_OPTION_KEY, JSON.stringify(remembered));
        } catch (error) {
            // 저장소가 제한돼도 현재 선택 상태는 유지한다.
        }
    }

    function restoreRememberedOption(product) {
        const urlOptionName = new URLSearchParams(window.location.search).get("option");
        const rememberedName = urlOptionName || readRememberedOptions()[product.id];
        selectedOptionName = (product.options || []).some((option) => option.name === rememberedName && Number(option.stock || 0) > 0)
            ? rememberedName
            : "";
        selectedQuantity = selectedOptionName ? readSelectedQuantity() : 1;
    }

    function syncSelectedOptionUrl() {
        const url = new URL(window.location.href);
        if (selectedOptionName) {
            url.searchParams.set("option", selectedOptionName);
        } else {
            url.searchParams.delete("option");
        }
        window.history.replaceState({}, "", `${url.pathname}${url.search}${url.hash}`);
    }

    function openRelatedByDirection(direction) {
        if (!currentProduct) {
            return;
        }
        const related = sortedRelatedProducts(currentProduct);
        if (!related.length) {
            showToast("이동할 연관 상품이 없습니다.", "연관 상품 조건을 넓혀 다시 확인해주세요.", true);
            return;
        }
        const target = direction < 0 ? related[related.length - 1] : related[0];
        window.location.href = buildProductUrl(target.id);
    }

    function setElementText(element, text) {
        if (element) {
            element.textContent = text;
        }
    }

    function buildCatalogUrl() {
        const returnTo = new URLSearchParams(window.location.search).get("returnTo");
        if (returnTo?.startsWith("/front/collections/")) {
            return returnTo;
        }
        return `/front${detailNavigationSearch()}`;
    }

    function buildProductUrl(nextProductId) {
        return `/front/products/${nextProductId}${detailNavigationSearch()}`;
    }

    function detailNavigationSearch() {
        const params = new URLSearchParams(window.location.search);
        params.delete("option");
        const query = params.toString();
        return query ? `?${query}` : "";
    }

    function saveRecentProduct(product) {
        if (!product?.id) {
            return;
        }
        const previous = readRecentProducts().filter((item) => Number(item.id) !== Number(product.id));
        const current = {
            id: product.id,
            brand: product.brand,
            name: product.name,
            headline: product.headline,
            model: product.model,
            price: product.price,
            priceLabel: product.priceLabel,
            stock: product.stock,
            stockStatus: product.stockStatus
        };
        const next = [current].concat(previous).slice(0, RECENT_VIEWED_LIMIT);
        if (window.StorefrontState) {
            window.StorefrontState.write("recent", next);
            return;
        }
        try {
            window.localStorage.setItem(RECENT_VIEWED_KEY, JSON.stringify(next));
        } catch (error) {
            // 저장소가 제한돼도 상세 화면 렌더링은 유지한다.
        }
    }

    function readRecentProducts() {
        try {
            const parsed = window.StorefrontState
                ? window.StorefrontState.read("recent")
                : JSON.parse(window.localStorage.getItem(RECENT_VIEWED_KEY) || "[]");
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            return [];
        }
    }

    function renderRecentProducts(currentProductId) {
        if (!elements.detailRecentSection || !elements.detailRecentGrid) {
            return;
        }
        const recentProducts = visibleDetailRecentProducts(currentProductId);
        setElementText(elements.detailRecentCount, String(recentProducts.length));
        const recentPrices = recentProducts.map((item) => Number(item.price || 0));
        setElementText(elements.detailRecentAveragePrice, formatPrice(recentPrices.length ? Math.round(recentPrices.reduce((sum, price) => sum + price, 0) / recentPrices.length) : 0));
        setElementText(elements.detailRecentPriceRange, recentPrices.length ? `${formatPrice(Math.min(...recentPrices))} - ${formatPrice(Math.max(...recentPrices))}` : "0원");
        setElementText(elements.detailRecentTotalStock, `${recentProducts.reduce((sum, item) => sum + Number(item.stock || 0), 0)}개`);
        setElementText(elements.detailRecentLowStockCount, `${recentProducts.filter((item) => Number(item.stock || 0) < lowStockThreshold()).length}개`);
        setElementText(elements.detailRecentBrandCount, `${new Set(recentProducts.map((item) => item.brand).filter(Boolean)).size}개`);
        elements.detailRecentSortPriceButton?.classList.toggle("is-active", detailRecentState.sort === "PRICE_LOW");
        elements.detailRecentSortStockButton?.classList.toggle("is-active", detailRecentState.sort === "STOCK_ASC");
        elements.detailRecentAvailableOnlyButton?.classList.toggle("is-active", detailRecentState.availableOnly);
        elements.detailRecentAvailableOnlyButton?.setAttribute("aria-pressed", String(detailRecentState.availableOnly));
        [elements.detailRecentCompareAllButton, elements.detailRecentBookmarkAllButton]
            .forEach((button) => button?.toggleAttribute("disabled", recentProducts.length === 0));
        if (elements.detailPreviousRecentButton) {
            elements.detailPreviousRecentButton.disabled = !recentProducts.length;
        }
        if (elements.detailNextRecentButton) {
            elements.detailNextRecentButton.disabled = !recentProducts.length;
        }
        if (!recentProducts.length) {
            elements.detailRecentSection.hidden = true;
            return;
        }
        elements.detailRecentSection.hidden = false;
        elements.detailRecentGrid.innerHTML = recentProducts.map((item) => {
            item = markupSafeObject(item);
            return `
            <article class="detail-related-card saved-product-card">
                ${productVisualMarkup(item, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${item.brand || "-"}</span>
                <strong>${item.headline || item.name || "-"}</strong>
                <p>${item.name || "-"} · ${item.model || "-"} · ${stockPressureDetail(item.stock)}</p>
                <div class="detail-related-card__meta">
                    <span>최근 본 흐름</span>
                    <span>${item.priceLabel || formatPrice(item.price)}</span>
                    <span class="${stockClassName(item.stock)}">${item.stockStatus || stockLabel(item.stock)}</span>
                    <span>다시 보기</span>
                </div>
                <div class="saved-product-card__actions">
                    <a href="${buildProductUrl(item.id)}">상세 보기</a>
                    <button class="saved-product-card__danger" type="button" data-remove-detail-recent-id="${item.id}">삭제</button>
                </div>
            </article>
        `;
        }).join("");
    }

    function visibleDetailRecentProducts(currentProductId) {
        let recentProducts = readRecentProducts().filter((item) => Number(item.id) !== Number(currentProductId));
        if (detailRecentState.availableOnly) {
            recentProducts = recentProducts.filter((item) => Number(item.stock || 0) > 0);
        }
        if (detailRecentState.sort === "PRICE_LOW") {
            recentProducts.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        if (detailRecentState.sort === "STOCK_ASC") {
            recentProducts.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        return recentProducts.slice(0, 3);
    }

    function addDetailRecentToBoard(target) {
        const recentProducts = visibleDetailRecentProducts(productId);
        const current = target === "COMPARE" ? readCompareProducts() : readBookmarkProducts();
        const limit = target === "COMPARE" ? 3 : 6;
        const merged = recentProducts.concat(current).filter((item, index, items) =>
            items.findIndex((candidate) => Number(candidate.id) === Number(item.id)) === index
        ).slice(0, limit);
        if (target === "COMPARE") {
            writeCompareProducts(merged);
        } else {
            writeBookmarkProducts(merged);
        }
        syncActionButtons();
        showToast(target === "COMPARE" ? "최근 상품을 비교 보드에 담았습니다." : "최근 상품을 관심 보드에 담았습니다.", `${merged.length}개 상품을 유지합니다.`);
    }

    function removeRecentProduct(productIdValue) {
        const next = readRecentProducts().filter((item) => Number(item.id) !== Number(productIdValue));
        if (window.StorefrontState) {
            window.StorefrontState.write("recent", next);
        } else {
            try {
                window.localStorage.setItem(RECENT_VIEWED_KEY, JSON.stringify(next));
            } catch (error) {
                showToast("최근 본 상품을 수정하지 못했습니다.", "브라우저 저장소를 확인해주세요.", true);
                return;
            }
        }
        renderRecentProducts(productId);
        showToast("최근 본 상품에서 삭제했습니다.", "선택한 상품만 최근 흐름에서 제외했습니다.");
    }

    function openRecentProductByDirection(direction) {
        const recentProducts = visibleDetailRecentProducts(productId);
        if (!recentProducts.length) {
            showToast("이동할 최근 상품이 없습니다.", "다른 상품을 확인하면 최근 흐름이 생성됩니다.", true);
            return;
        }
        const target = direction < 0 ? recentProducts[recentProducts.length - 1] : recentProducts[0];
        window.location.href = buildProductUrl(target.id);
    }

    async function copyRecentProductLinks() {
        const recentProducts = visibleDetailRecentProducts(productId);
        const text = recentProducts.length
            ? recentProducts.map((item) => new URL(buildProductUrl(item.id), window.location.origin).href).join("\n")
            : "최근 본 상품이 없습니다.";
        await copyText(text, "최근 상품 링크를 복사했습니다.");
    }

    async function copyDetailBreadcrumb() {
        if (!currentProduct) {
            return;
        }
        await copyText(`홈 / ${currentProduct.category || "상품"} / ${currentProduct.name}`, "상품 경로를 복사했습니다.");
    }

    function addAllRelatedToCompare() {
        if (!currentProduct) {
            return;
        }
        const current = readCompareProducts();
        const merged = current.slice();
        sortedRelatedProducts(currentProduct).forEach((item) => {
            if (!merged.some((saved) => Number(saved.id) === Number(item.id))) {
                merged.push(productStorageSummary(item));
            }
        });
        const next = merged.slice(0, 3);
        const addedCount = Math.max(0, next.length - current.length);
        writeCompareProducts(next);
        syncActionButtons();
        renderRelated(currentProduct);
        showToast(
            addedCount ? `연관 상품 ${addedCount}개를 비교에 담았습니다.` : "추가할 비교 상품이 없습니다.",
            "비교 보드는 최대 3개 상품을 유지합니다."
        );
    }

    function addAllRelatedToBookmark() {
        if (!currentProduct) {
            return;
        }
        const current = readBookmarkProducts();
        const merged = sortedRelatedProducts(currentProduct).map(productStorageSummary).concat(current)
            .filter((item, index, items) => items.findIndex((candidate) => Number(candidate.id) === Number(item.id)) === index)
            .slice(0, 6);
        writeBookmarkProducts(merged);
        syncActionButtons();
        renderRelated(currentProduct);
        showToast("연관 상품을 관심 보드에 담았습니다.", `${merged.length}개 관심 상품을 유지합니다.`);
    }

    async function copyRelatedProductLinks() {
        const related = currentProduct ? sortedRelatedProducts(currentProduct) : [];
        const links = related.map((item) => new URL(buildProductUrl(item.id), window.location.origin).href);
        await copyText(links.join("\n") || "표시 가능한 연관 상품이 없습니다.", "연관 상품 링크를 복사했습니다.");
    }

    function productStorageSummary(product) {
        return {
            id: product.id,
            brand: product.brand,
            name: product.name,
            headline: product.headline,
            model: product.model,
            category: product.category,
            price: product.price,
            priceLabel: product.priceLabel,
            stock: product.stock,
            stockStatus: product.stockStatus,
            thumbnailUrl: product.thumbnailUrl
        };
    }

    function syncDetailStateFromStorage(event) {
        if (!currentProduct) {
            return;
        }
        if ([BOOKMARK_PRODUCTS_KEY, COMPARE_PRODUCTS_KEY].includes(event.key)) {
            syncActionButtons();
            renderRelated(currentProduct);
        }
        if (event.key === RECENT_VIEWED_KEY) {
            renderRecentProducts(productId);
        }
        if (event.key === SELECTED_OPTION_KEY) {
            restoreRememberedOption(currentProduct);
            renderOptions(currentProduct);
            const selected = currentProduct.options?.find((option) => option.name === selectedOptionName);
            syncSelectedOptionActions(selected || {});
        }
        if (event.key === DETAIL_QUANTITY_KEY && selectedOptionName) {
            selectedQuantity = readSelectedQuantity();
            const selected = currentProduct.options?.find((option) => option.name === selectedOptionName);
            syncPurchaseEstimate(selected || {});
        }
    }

    function sortedOptions(product) {
        const options = Array.isArray(product?.options) ? product.options.slice() : [];
        const visibleOptions = options.filter((option) => {
            if (optionSortState.availableOnly && Number(option.stock || 0) <= 0) {
                return false;
            }
            if (optionSortState.lowStockOnly) {
                return Number(option.stock || 0) < lowStockThreshold();
            }
            if (optionSortState.stableOnly) {
                return Number(option.stock || 0) >= lowStockThreshold();
            }
            return true;
        });
        if (optionSortState.mode === "NAME_ASC") {
            return visibleOptions.sort((left, right) => String(left.name || "").localeCompare(String(right.name || ""), "ko"));
        }
        if (optionSortState.mode === "STOCK_DESC") {
            return visibleOptions.sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0));
        }
        return visibleOptions.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
    }

    function syncOptionSortButtons() {
        elements.detailOptionSortStockButton?.classList.toggle("is-active", optionSortState.mode === "STOCK_ASC");
        elements.detailOptionSortNameButton?.classList.toggle("is-active", optionSortState.mode === "NAME_ASC");
        elements.detailOptionLowStockOnlyButton?.classList.toggle("is-active", optionSortState.lowStockOnly);
        elements.detailOptionSortStockHighButton?.classList.toggle("is-active", optionSortState.mode === "STOCK_DESC");
        elements.detailOptionStableOnlyButton?.classList.toggle("is-active", optionSortState.stableOnly);
        elements.detailOptionAvailableOnlyButton?.classList.toggle("is-active", optionSortState.availableOnly);
        [
            [elements.detailOptionSortStockButton, optionSortState.mode === "STOCK_ASC"],
            [elements.detailOptionSortNameButton, optionSortState.mode === "NAME_ASC"],
            [elements.detailOptionSortStockHighButton, optionSortState.mode === "STOCK_DESC"],
            [elements.detailOptionLowStockOnlyButton, optionSortState.lowStockOnly],
            [elements.detailOptionStableOnlyButton, optionSortState.stableOnly],
            [elements.detailOptionAvailableOnlyButton, optionSortState.availableOnly]
        ].forEach(([button, isPressed]) => button?.setAttribute("aria-pressed", String(isPressed)));
    }

    function sortedRelatedProducts(product) {
        const related = Array.isArray(product?.relatedProducts) ? product.relatedProducts.slice() : [];
        const visibleRelated = related.filter((item) => {
            if (relatedSortState.availableOnly && Number(item.stock || 0) <= 0) {
                return false;
            }
            if (relatedSortState.lowStockOnly && Number(item.stock || 0) >= lowStockThreshold()) {
                return false;
            }
            if (relatedSortState.sameBrandOnly && item.brand !== product.brand) {
                return false;
            }
            if (relatedSortState.sameCategoryOnly && item.category !== product.category) {
                return false;
            }
            if (relatedSortState.cheaperOnly && Number(item.price || 0) >= Number(product.price || 0)) {
                return false;
            }
            if (relatedSortState.stockAdvantageOnly && Number(item.stock || 0) <= Number(product.stock || 0)) {
                return false;
            }
            return !relatedSortState.soldOutOnly || Number(item.stock || 0) <= 0;
        });
        if (relatedSortState.mode === "STOCK_ASC") {
            return visibleRelated.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (relatedSortState.mode === "PRICE_HIGH") {
            return visibleRelated.sort((left, right) => Number(right.price || 0) - Number(left.price || 0));
        }
        if (relatedSortState.mode === "PRICE_LOW") {
            return visibleRelated.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        return visibleRelated;
    }

    function syncRelatedButtons() {
        elements.detailRelatedSortStockButton?.classList.toggle("is-active", relatedSortState.mode === "STOCK_ASC");
        elements.detailRelatedSortPriceButton?.classList.toggle("is-active", relatedSortState.mode === "PRICE_HIGH");
        elements.detailRelatedLowStockOnlyButton?.classList.toggle("is-active", relatedSortState.lowStockOnly);
        elements.detailRelatedSortPriceLowButton?.classList.toggle("is-active", relatedSortState.mode === "PRICE_LOW");
        elements.detailRelatedSameBrandButton?.classList.toggle("is-active", relatedSortState.sameBrandOnly);
        elements.detailRelatedSameCategoryButton?.classList.toggle("is-active", relatedSortState.sameCategoryOnly);
        elements.detailRelatedAvailableOnlyButton?.classList.toggle("is-active", relatedSortState.availableOnly);
        elements.detailRelatedCheaperOnlyButton?.classList.toggle("is-active", relatedSortState.cheaperOnly);
        elements.detailRelatedStockAdvantageOnlyButton?.classList.toggle("is-active", relatedSortState.stockAdvantageOnly);
        elements.detailRelatedSoldOutOnlyButton?.classList.toggle("is-active", relatedSortState.soldOutOnly);
        [
            [elements.detailRelatedSortStockButton, relatedSortState.mode === "STOCK_ASC"],
            [elements.detailRelatedSortPriceButton, relatedSortState.mode === "PRICE_HIGH"],
            [elements.detailRelatedSortPriceLowButton, relatedSortState.mode === "PRICE_LOW"],
            [elements.detailRelatedLowStockOnlyButton, relatedSortState.lowStockOnly],
            [elements.detailRelatedSameBrandButton, relatedSortState.sameBrandOnly],
            [elements.detailRelatedSameCategoryButton, relatedSortState.sameCategoryOnly],
            [elements.detailRelatedAvailableOnlyButton, relatedSortState.availableOnly],
            [elements.detailRelatedCheaperOnlyButton, relatedSortState.cheaperOnly],
            [elements.detailRelatedStockAdvantageOnlyButton, relatedSortState.stockAdvantageOnly],
            [elements.detailRelatedSoldOutOnlyButton, relatedSortState.soldOutOnly]
        ].forEach(([button, isPressed]) => button?.setAttribute("aria-pressed", String(isPressed)));
        const hasFilter = relatedSortState.mode !== "DEFAULT"
            || relatedSortState.lowStockOnly
            || relatedSortState.sameBrandOnly
            || relatedSortState.sameCategoryOnly
            || relatedSortState.availableOnly
            || relatedSortState.cheaperOnly
            || relatedSortState.stockAdvantageOnly
            || relatedSortState.soldOutOnly;
        if (elements.detailResetRelatedFiltersButton) {
            elements.detailResetRelatedFiltersButton.disabled = !hasFilter;
        }
        const activeLabels = [
            relatedSortState.mode === "STOCK_ASC" ? "재고 낮은 순" : "",
            relatedSortState.mode === "PRICE_HIGH" ? "가격 높은 순" : "",
            relatedSortState.mode === "PRICE_LOW" ? "가격 낮은 순" : "",
            relatedSortState.lowStockOnly ? "긴장 재고" : "",
            relatedSortState.sameBrandOnly ? "같은 브랜드" : "",
            relatedSortState.sameCategoryOnly ? "같은 카테고리" : "",
            relatedSortState.availableOnly ? "구매 가능" : "",
            relatedSortState.cheaperOnly ? "현재보다 저렴함" : "",
            relatedSortState.stockAdvantageOnly ? "재고 우위" : "",
            relatedSortState.soldOutOnly ? "품절" : ""
        ].filter(Boolean);
        setElementText(elements.detailRelatedFilterStatus, activeLabels.join(" · ") || "기본 추천");
    }

    function bindRelatedCardActions(related) {
        const findRelated = (id) => related.find((item) => Number(item.id) === Number(id));
        elements.detailRelatedGrid?.querySelectorAll("[data-related-compare-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const item = findRelated(button.dataset.relatedCompareId);
                if (item) {
                    toggleCompareProduct(item);
                    renderRelated(currentProduct);
                }
            });
        });
        elements.detailRelatedGrid?.querySelectorAll("[data-related-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const item = findRelated(button.dataset.relatedBookmarkId);
                if (item) {
                    toggleBookmarkProduct(item);
                    renderRelated(currentProduct);
                }
            });
        });
        elements.detailRelatedGrid?.querySelectorAll("[data-related-copy-id]").forEach((button) => {
            button.addEventListener("click", async () => {
                const item = findRelated(button.dataset.relatedCopyId);
                if (item) {
                    await copyText(`${item.name} · ${item.brand} · ${item.priceLabel || formatPrice(item.price)} · ${stockPressureDetail(item.stock)}`, "연관 상품 요약을 복사했습니다.");
                }
            });
        });
    }

    async function copyText(text, successTitle) {
        try {
            if (!navigator.clipboard?.writeText) {
                throw new Error("Clipboard not available");
            }
            await navigator.clipboard.writeText(text);
            showToast(successTitle, "메신저나 문서에 바로 붙여 넣을 수 있습니다.");
        } catch (error) {
            window.prompt("내용을 복사하세요.", text);
        }
    }

    function summaryText(product) {
        return [
            product.headline || product.name,
            product.brand,
            product.category,
            product.model,
            product.priceLabel || formatPrice(product.price),
            stockPressureDetail(product.stock)
        ].filter(Boolean).join(" · ");
    }

    function optionSummaryText(product) {
        const options = sortedOptions(product);
        return options.length
            ? options.map((option, index) => `${index + 1}. ${option.name} · ${option.stock}개 · ${stockLabel(option.stock)}`).join("\n")
            : "표시 가능한 옵션이 없습니다.";
    }

    function relatedSummaryText(product) {
        const related = sortedRelatedProducts(product);
        return related.length
            ? related.map((item, index) => `${index + 1}. ${item.name} · ${item.priceLabel || formatPrice(item.price)} · 재고 ${item.stock}개`).join("\n")
            : "표시 가능한 연관 상품이 없습니다.";
    }

    function readBookmarkProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(BOOKMARK_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeBookmarkProducts(bookmarkedProducts) {
        if (window.StorefrontState) {
            window.StorefrontState.write(BOOKMARK_PRODUCTS_KEY, bookmarkedProducts);
            return;
        }
        window.localStorage.setItem(BOOKMARK_PRODUCTS_KEY, JSON.stringify(bookmarkedProducts));
    }

    function readCompareProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(COMPARE_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeCompareProducts(comparedProducts) {
        if (window.StorefrontState) {
            window.StorefrontState.write(COMPARE_PRODUCTS_KEY, comparedProducts);
            return;
        }
        window.localStorage.setItem(COMPARE_PRODUCTS_KEY, JSON.stringify(comparedProducts));
    }

    function isBookmarkedProduct(productIdValue) {
        return readBookmarkProducts().some((item) => Number(item.id) === Number(productIdValue));
    }

    function isComparedProduct(productIdValue) {
        return readCompareProducts().some((item) => Number(item.id) === Number(productIdValue));
    }

    function toggleBookmarkProduct(product) {
        const current = readBookmarkProducts();
        const exists = current.some((item) => Number(item.id) === Number(product.id));
        if (exists) {
            writeBookmarkProducts(current.filter((item) => Number(item.id) !== Number(product.id)));
            showToast("관심 상품에서 제외했습니다.", `${product.headline || product.name}을 찜 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: product.id,
                brand: product.brand,
                name: product.name,
                headline: product.headline,
                model: product.model,
                category: product.category,
                price: product.price,
                priceLabel: product.priceLabel,
                stock: product.stock,
                stockStatus: product.stockStatus,
                featured: Boolean(product.featured),
                thumbnailUrl: product.thumbnailUrl
            };
            writeBookmarkProducts([summary].concat(current).slice(0, 6));
            showToast("관심 상품에 담았습니다.", `${product.headline || product.name}을 나중에 다시 볼 수 있습니다.`);
        }
        syncActionButtons();
    }

    function toggleCompareProduct(product) {
        const current = readCompareProducts();
        const exists = current.some((item) => Number(item.id) === Number(product.id));
        if (exists) {
            writeCompareProducts(current.filter((item) => Number(item.id) !== Number(product.id)));
            showToast("비교 대상에서 제외했습니다.", `${product.headline || product.name}을 비교 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: product.id,
                brand: product.brand,
                name: product.name,
                headline: product.headline,
                model: product.model,
                category: product.category,
                price: product.price,
                priceLabel: product.priceLabel,
                stock: product.stock,
                stockStatus: product.stockStatus,
                thumbnailUrl: product.thumbnailUrl
            };
            writeCompareProducts([summary].concat(current).slice(0, 3));
            showToast("비교 보드에 담았습니다.", `${product.headline || product.name}을 비교 목록에 추가했습니다.`);
        }
        syncActionButtons();
    }

    function syncActionButtons() {
        if (!currentProduct) {
            return;
        }
        if (elements.detailBookmarkButton) {
            const bookmarked = isBookmarkedProduct(currentProduct.id);
            elements.detailBookmarkButton.textContent = bookmarked ? "관심 상품 해제" : "관심 상품 담기";
            elements.detailBookmarkButton.classList.toggle("is-active", bookmarked);
            elements.detailBookmarkButton.setAttribute("aria-pressed", String(bookmarked));
            elements.detailMobileBookmarkButton?.classList.toggle("is-active", bookmarked);
            elements.detailMobileBookmarkButton?.setAttribute("aria-pressed", String(bookmarked));
            const mobileIcon = elements.detailMobileBookmarkButton?.querySelector("span");
            if (mobileIcon) {
                mobileIcon.textContent = bookmarked ? "♥" : "♡";
            }
        }
        if (elements.detailCompareButton) {
            const compared = isComparedProduct(currentProduct.id);
            elements.detailCompareButton.textContent = compared ? "비교 보드 해제" : "비교 보드 담기";
            elements.detailCompareButton.classList.toggle("is-active", compared);
            elements.detailCompareButton.setAttribute("aria-pressed", String(compared));
            elements.detailMobileCompareButton?.classList.toggle("is-active", compared);
            elements.detailMobileCompareButton?.setAttribute("aria-pressed", String(compared));
        }
        if (elements.detailMobilePrice) {
            elements.detailMobilePrice.textContent = currentProduct.priceLabel || formatPrice(currentProduct.price);
        }
    }

    function focusDetailOptions() {
        document.getElementById("detailOptions")?.scrollIntoView({ behavior: "smooth", block: "start" });
        elements.detailPrimaryAction?.classList.add("is-active");
        elements.detailMobilePrimaryButton?.classList.add("is-active");
        window.setTimeout(() => {
            elements.detailPrimaryAction?.classList.remove("is-active");
            elements.detailMobilePrimaryButton?.classList.remove("is-active");
        }, 700);
    }

    function openDetailOptionsFromKeyboard() {
        focusDetailOptions();
        window.setTimeout(() => {
            elements.detailOptionGrid?.querySelector("[data-detail-option]:not(:disabled)")?.focus({ preventScroll: true });
            setElementText(elements.detailStatus, "구매 옵션 영역으로 이동했습니다.");
        }, 120);
    }

    function initSectionNavigation() {
        const navLinks = Array.from(document.querySelectorAll(".detail-section-nav a[href^=\"#\"]"));
        if (!navLinks.length || typeof IntersectionObserver === "undefined") {
            return;
        }
        const sections = navLinks
            .map((link) => {
                const section = document.querySelector(link.getAttribute("href"));
                return section ? { link, section } : null;
            })
            .filter(Boolean);
        const observer = new IntersectionObserver((entries) => {
            const visible = entries
                .filter((entry) => entry.isIntersecting)
                .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
            if (!visible) {
                return;
            }
            sections.forEach(({ link, section }) => {
                const isActive = section === visible.target;
                link.classList.toggle("is-active", isActive);
                if (isActive) {
                    link.setAttribute("aria-current", "location");
                } else {
                    link.removeAttribute("aria-current");
                }
            });
            const activeLink = sections.find(({ section }) => section === visible.target)?.link;
            if (elements.detailStatus) {
                elements.detailStatus.textContent = activeLink ? `${activeLink.textContent.trim()} 영역을 보고 있습니다.` : "";
            }
        }, {
            rootMargin: "-25% 0px -55% 0px",
            threshold: [0.2, 0.45, 0.7]
        });
        sections.forEach(({ section }) => observer.observe(section));
    }

    function syncDetailScrollProgress() {
        const scrollableHeight = document.documentElement.scrollHeight - window.innerHeight;
        const progress = scrollableHeight > 0 ? Math.min(100, Math.max(0, (window.scrollY / scrollableHeight) * 100)) : 0;
        if (elements.detailScrollProgress) {
            elements.detailScrollProgress.style.transform = `scaleX(${progress / 100})`;
        }
        elements.detailScrollTopButton?.classList.toggle("is-visible", window.scrollY > 480);
    }

    function openDetailImageModal() {
        if (!elements.detailImageModal || !elements.detailImageModalImage?.src) {
            return;
        }
        detailModalReturnFocus = document.activeElement;
        elements.detailImageModal.classList.add("is-open");
        elements.detailImageModal.setAttribute("aria-hidden", "false");
        setDetailModalBackgroundInert(true);
        document.body.classList.add("has-open-modal");
        elements.detailImageModalCloseButton?.focus();
    }

    function closeDetailImageModal() {
        if (!elements.detailImageModal?.classList.contains("is-open")) {
            return;
        }
        elements.detailImageModal.classList.remove("is-open");
        elements.detailImageModal.setAttribute("aria-hidden", "true");
        setDetailModalBackgroundInert(false);
        document.body.classList.remove("has-open-modal");
        if (detailModalReturnFocus?.isConnected) {
            detailModalReturnFocus.focus();
        }
        detailModalReturnFocus = null;
    }

    function setDetailModalBackgroundInert(isInert) {
        [document.querySelector(".page-shell--detail"), elements.detailMobileActions, elements.detailScrollTopButton]
            .filter(Boolean)
            .forEach((element) => {
                element.inert = isInert;
            });
    }

    function keepFocusInsideDetailImageModal(event) {
        const focusable = Array.from(elements.detailImageModal.querySelectorAll(
            'button:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter((element) => element.getClientRects().length);
        if (!focusable.length) {
            event.preventDefault();
            elements.detailImageModal.querySelector(".detail-image-modal__panel")?.focus();
            return;
        }
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    }

    function showToast(title, body, isWarning = false) {
        const stack = ensureToastStack();
        if (!stack) {
            return;
        }
        const toast = document.createElement("article");
        toast.className = `toast${isWarning ? " is-warning" : ""}`;
        toast.setAttribute("role", isWarning ? "alert" : "status");
        toast.setAttribute("aria-live", isWarning ? "assertive" : "polite");
        const titleElement = document.createElement("strong");
        titleElement.textContent = String(title || "");
        const bodyElement = document.createElement("span");
        bodyElement.textContent = String(body || "");
        const closeButton = document.createElement("button");
        closeButton.type = "button";
        closeButton.textContent = "×";
        closeButton.setAttribute("aria-label", `${String(title || "알림")} 알림 닫기`);
        toast.append(titleElement, bodyElement, closeButton);
        toast.dataset.toastId = String(++toastTimerSeed);
        while (stack.childElementCount >= 3) {
            stack.firstElementChild?.remove();
        }
        stack.appendChild(toast);
        const dismissToast = () => {
            toast.remove();
            if (!stack.childElementCount) {
                stack.remove();
            }
        };
        toast.querySelector("button")?.addEventListener("click", dismissToast);
        window.setTimeout(dismissToast, 3600);
    }

    function ensureToastStack() {
        let stack = document.querySelector(".toast-stack");
        if (stack) {
            return stack;
        }
        stack = document.createElement("div");
        stack.className = "toast-stack";
        stack.setAttribute("role", "region");
        stack.setAttribute("aria-label", "화면 알림");
        document.body.appendChild(stack);
        return stack;
    }

    function initMobileActionDock() {
        const desktopActions = document.querySelector(".detail-actions");
        const mobileActions = elements.detailMobileActions;
        if (!desktopActions || !mobileActions) return;
        const mobileQuery = window.matchMedia("(max-width: 767px)");
        const syncDock = () => {
            const shouldDock = mobileQuery.matches && desktopActions.getBoundingClientRect().bottom < 0;
            document.body.classList.toggle("is-detail-purchase-docked", shouldDock);
        };
        window.addEventListener("scroll", syncDock, { passive: true });
        mobileQuery.addEventListener("change", syncDock);
        syncDock();
    }

    async function init() {
        if (!productId) {
            return;
        }
        syncCatalogLinks();
        document.addEventListener("error", handleProductImageError, true);
        initSectionNavigation();
        initMobileActionDock();
        window.addEventListener("scroll", syncDetailScrollProgress, { passive: true });
        window.addEventListener("storage", syncDetailStateFromStorage);
        document.addEventListener("storefront:state-ready", () => {
            [BOOKMARK_PRODUCTS_KEY, COMPARE_PRODUCTS_KEY, RECENT_VIEWED_KEY]
                .forEach((key) => syncDetailStateFromStorage({ key }));
        });
        syncDetailScrollProgress();
        elements.detailZoomButton?.addEventListener("click", openDetailImageModal);
        elements.detailImageModalCloseButton?.addEventListener("click", closeDetailImageModal);
        elements.detailReportModalCloseButton?.addEventListener("click", closeReportModal);
        elements.detailReportCancelButton?.addEventListener("click", closeReportModal);
        elements.detailReportForm?.addEventListener("submit", submitReviewReport);
        elements.detailImageModal?.addEventListener("click", (event) => {
            if (event.target === elements.detailImageModal) {
                closeDetailImageModal();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeDetailImageModal();
                closeReportModal();
            }
            if (event.key === "Tab" && elements.detailImageModal?.classList.contains("is-open")) {
                keepFocusInsideDetailImageModal(event);
            }
            const target = event.target;
            const isEditable = ["INPUT", "TEXTAREA", "SELECT"].includes(target?.tagName) || target?.isContentEditable;
            if (event.key.toLowerCase() === "o" && !isEditable && !event.ctrlKey && !event.metaKey && !event.altKey) {
                event.preventDefault();
                openDetailOptionsFromKeyboard();
            }
            if (selectedOptionName && !isEditable && !event.ctrlKey && !event.metaKey && !event.altKey && ["-", "+", "="].includes(event.key)) {
                event.preventDefault();
                setSelectedQuantity(selectedQuantity + (event.key === "-" ? -1 : 1));
            }
        });
        elements.detailClearOptionButton?.addEventListener("click", () => {
            selectedOptionName = "";
            selectedQuantity = 1;
            rememberSelectedOption(productId, "");
            syncSelectedOptionUrl();
            if (currentProduct) {
                renderOptions(currentProduct);
                syncSelectedOptionActions({});
            }
        });
        elements.detailCopySelectedOptionButton?.addEventListener("click", async () => {
            const text = selectedOptionSummary();
            if (text) {
                await copyText(text, "선택 옵션을 복사했습니다.");
            }
        });
        elements.detailShareSelectedOptionButton?.addEventListener("click", shareSelectedOption);
        elements.detailQuantityDecreaseButton?.addEventListener("click", () => setSelectedQuantity(selectedQuantity - 1));
        elements.detailQuantityIncreaseButton?.addEventListener("click", () => setSelectedQuantity(selectedQuantity + 1));
        elements.detailQuantityInput?.addEventListener("change", () => setSelectedQuantity(elements.detailQuantityInput.value));
        elements.detailQuantityPresetTwoButton?.addEventListener("click", () => setSelectedQuantity(2));
        elements.detailQuantityPresetThreeButton?.addEventListener("click", () => setSelectedQuantity(3));
        elements.detailQuantityPresetFiveButton?.addEventListener("click", () => setSelectedQuantity(5));
        elements.detailQuantityMaxButton?.addEventListener("click", () => {
            const option = currentProduct?.options?.find((item) => item.name === selectedOptionName);
            setSelectedQuantity(option?.stock || 1);
        });
        elements.detailQuantityResetButton?.addEventListener("click", () => setSelectedQuantity(1));
        elements.detailCopyOrderSummaryButton?.addEventListener("click", copyOrderSummary);
        elements.detailCopyOptionMatrixButton?.addEventListener("click", copyOptionStockMatrix);
        elements.detailRetryButton?.addEventListener("click", () => window.location.reload());
        elements.detailScrollTopButton?.addEventListener("click", () => window.scrollTo({ top: 0, behavior: "smooth" }));
        elements.detailCopyBreadcrumbButton?.addEventListener("click", copyDetailBreadcrumb);
        elements.detailRecommendOptionButton?.addEventListener("click", () => {
            if (!currentProduct) {
                return;
            }
            const recommended = (currentProduct.options || [])
                .filter((option) => Number(option.stock || 0) > 0)
                .sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0))[0];
            if (recommended) {
                selectedOptionName = recommended.name;
                selectedQuantity = readSelectedQuantity();
                rememberSelectedOption(productId, selectedOptionName);
                syncSelectedOptionUrl();
                renderOptions(currentProduct);
                syncSelectedOptionActions(recommended);
            }
        });
        elements.detailPreviousRelatedButton?.addEventListener("click", () => openRelatedByDirection(-1));
        elements.detailNextRelatedButton?.addEventListener("click", () => openRelatedByDirection(1));
        elements.detailPreviousRecentButton?.addEventListener("click", () => openRecentProductByDirection(-1));
        elements.detailNextRecentButton?.addEventListener("click", () => openRecentProductByDirection(1));
        elements.detailCompareAllRelatedButton?.addEventListener("click", addAllRelatedToCompare);
        elements.detailCheapestRelatedButton?.addEventListener("click", () => openRelatedByMetric("PRICE_LOW"));
        elements.detailHighestStockRelatedButton?.addEventListener("click", () => openRelatedByMetric("STOCK_HIGH"));
        elements.detailBalancedRelatedButton?.addEventListener("click", openBalancedRelatedProduct);
        elements.detailCopyPriceComparisonButton?.addEventListener("click", copyRelatedPriceComparison);
        elements.detailCopyValueAnalysisButton?.addEventListener("click", copyRelatedValueAnalysis);
        elements.detailRecentGrid?.addEventListener("click", (event) => {
            const removeButton = event.target.closest("[data-remove-detail-recent-id]");
            if (removeButton) {
                removeRecentProduct(removeButton.dataset.removeDetailRecentId);
            }
        });
        elements.detailFocusRelated?.addEventListener("click", () => {
            document.getElementById("detailRelated")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.detailPrimaryAction?.addEventListener("click", () => {
            if (selectedOptionName) {
                addSelectedOptionToCart(false);
                return;
            }
            focusDetailOptions();
        });
        elements.detailMobilePrimaryButton?.addEventListener("click", () => {
            if (selectedOptionName) {
                addSelectedOptionToCart(false);
                return;
            }
            focusDetailOptions();
        });
        elements.detailAddCartButton?.addEventListener("click", () => addSelectedOptionToCart(false));
        elements.detailBuyNowButton?.addEventListener("click", () => addSelectedOptionToCart(true));
        elements.detailOptionGrid?.addEventListener("click", (event) => {
            const optionButton = event.target.closest("[data-detail-option]");
            if (optionButton) {
                selectDetailOption(optionButton.dataset.detailOption);
            }
        });
        elements.detailOptionGrid?.addEventListener("keydown", handleDetailOptionNavigation);
        elements.detailPreviousOptionButton?.addEventListener("click", () => moveDetailOption(-1));
        elements.detailNextOptionButton?.addEventListener("click", () => moveDetailOption(1));
        elements.detailCopyOptionHistoryButton?.addEventListener("click", copyOptionHistory);
        elements.detailClearOptionHistoryButton?.addEventListener("click", () => {
            optionSelectionHistory.length = 0;
            renderOptionHistory();
            showToast("옵션 탐색 이력을 비웠습니다.", "현재 선택 옵션은 유지합니다.");
        });
        elements.detailOptionHistoryList?.addEventListener("click", (event) => {
            const historyButton = event.target.closest("[data-option-history-name]");
            if (historyButton && historyButton.dataset.optionHistoryName !== selectedOptionName) {
                selectDetailOption(historyButton.dataset.optionHistoryName);
            }
        });
        elements.detailRelatedGrid?.addEventListener("keydown", handleRelatedCardNavigation);
        elements.detailShareButton?.addEventListener("click", async () => {
            const shareUrl = `${window.location.origin}${window.location.pathname}${window.location.search}`;
            try {
                if (navigator.share) {
                    await navigator.share({
                        title: currentProduct?.name || "NOREN 상품",
                        text: currentProduct ? summaryText(currentProduct) : "상품 상세",
                        url: shareUrl
                    });
                    showToast("상품을 공유했습니다.", "상품 요약과 상세 URL을 전달했습니다.");
                    return;
                }
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(shareUrl);
                }
                showToast("상품 URL을 복사했습니다.", "같은 탐색 조건까지 함께 공유됩니다.");
            } catch (error) {
                if (error?.name === "AbortError") {
                    return;
                }
                window.prompt("현재 상품 URL을 복사하세요.", shareUrl);
            }
        });
        elements.detailCopySummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = summaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("상품 요약을 복사했습니다.", "메신저나 문서에 바로 붙여 넣을 수 있습니다.");
            } catch (error) {
                window.prompt("상품 요약을 복사하세요.", text);
            }
        });
        elements.detailBookmarkButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleBookmarkProduct(currentProduct);
            }
        });
        elements.detailMobileBookmarkButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleBookmarkProduct(currentProduct);
            }
        });
        elements.detailCompareButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleCompareProduct(currentProduct);
            }
        });
        elements.detailMobileCompareButton?.addEventListener("click", () => {
            if (currentProduct) {
                toggleCompareProduct(currentProduct);
            }
        });
        elements.detailOptionSortStockButton?.addEventListener("click", () => {
            optionSortState.mode = "STOCK_ASC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 재고 낮은 순으로 정렬했습니다.", "품절 임박 옵션을 먼저 확인할 수 있습니다.");
            }
        });
        elements.detailOptionSortNameButton?.addEventListener("click", () => {
            optionSortState.mode = "NAME_ASC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 이름순으로 정렬했습니다.", "사이즈/옵션 라인을 더 빠르게 찾을 수 있습니다.");
            }
        });
        elements.detailOptionLowStockOnlyButton?.addEventListener("click", () => {
            optionSortState.lowStockOnly = !optionSortState.lowStockOnly;
            optionSortState.stableOnly = false;
            optionSortState.availableOnly = false;
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast(optionSortState.lowStockOnly ? "긴장 재고 옵션만 표시합니다." : "전체 옵션 표시로 복구했습니다.", "옵션 목록 밀도를 빠르게 전환할 수 있습니다.");
            }
        });
        elements.detailCopyOptionSummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = optionSummaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("옵션 요약을 복사했습니다.", "사이즈별 재고 정보를 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("옵션 요약을 복사하세요.", text);
            }
        });
        elements.detailOptionSortStockHighButton?.addEventListener("click", () => {
            optionSortState.mode = "STOCK_DESC";
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast("옵션을 재고 높은 순으로 정렬했습니다.", "선택 여유가 있는 옵션부터 확인할 수 있습니다.");
            }
        });
        elements.detailOptionStableOnlyButton?.addEventListener("click", () => {
            optionSortState.stableOnly = !optionSortState.stableOnly;
            optionSortState.lowStockOnly = false;
            optionSortState.availableOnly = false;
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast(optionSortState.stableOnly ? "안정 재고 옵션만 표시합니다." : "전체 옵션 표시로 복구했습니다.", "구매 가능한 옵션 밀도를 빠르게 전환할 수 있습니다.");
            }
        });
        elements.detailOptionAvailableOnlyButton?.addEventListener("click", () => {
            optionSortState.availableOnly = !optionSortState.availableOnly;
            optionSortState.lowStockOnly = false;
            optionSortState.stableOnly = false;
            if (currentProduct) {
                renderOptions(currentProduct);
                showToast(optionSortState.availableOnly ? "구매 가능한 옵션만 표시합니다." : "전체 옵션 표시로 복구했습니다.", "품절 옵션 노출을 빠르게 전환할 수 있습니다.");
            }
        });
        elements.detailCopyAvailableOptionsButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const available = (currentProduct.options || []).filter((option) => Number(option.stock || 0) > 0);
            const text = available.length
                ? available.map((option, index) => `${index + 1}. ${option.name} · 재고 ${option.stock}개`).join("\n")
                : "구매 가능한 옵션이 없습니다.";
            await copyText(text, "구매 가능 옵션을 복사했습니다.");
        });
        elements.detailRelatedSortStockButton?.addEventListener("click", () => {
            relatedSortState.mode = "STOCK_ASC";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 재고 낮은 순으로 정렬했습니다.", "긴장 재고 연관 상품을 먼저 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSortPriceButton?.addEventListener("click", () => {
            relatedSortState.mode = "PRICE_HIGH";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 가격 높은 순으로 정렬했습니다.", "고가 비교 대상부터 바로 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSortPriceLowButton?.addEventListener("click", () => {
            relatedSortState.mode = "PRICE_LOW";
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품을 가격 낮은 순으로 정렬했습니다.", "부담이 낮은 비교 후보부터 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedSameBrandButton?.addEventListener("click", () => {
            relatedSortState.sameBrandOnly = !relatedSortState.sameBrandOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.sameBrandOnly ? "같은 브랜드 연관 상품만 표시합니다." : "전체 브랜드로 복구했습니다.", "브랜드 내부 대안을 빠르게 비교할 수 있습니다.");
            }
        });
        elements.detailRelatedSameCategoryButton?.addEventListener("click", () => {
            relatedSortState.sameCategoryOnly = !relatedSortState.sameCategoryOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.sameCategoryOnly ? "같은 카테고리 연관 상품만 표시합니다." : "전체 카테고리로 복구했습니다.", "동일 상품군 안에서 대안을 비교할 수 있습니다.");
            }
        });
        elements.detailRelatedAvailableOnlyButton?.addEventListener("click", () => {
            relatedSortState.availableOnly = !relatedSortState.availableOnly;
            relatedSortState.soldOutOnly = false;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.availableOnly ? "구매 가능한 연관 상품만 표시합니다." : "품절 상품도 다시 표시합니다.", "재고가 있는 비교 후보를 우선 확인할 수 있습니다.");
            }
        });
        elements.detailRelatedCheaperOnlyButton?.addEventListener("click", () => {
            relatedSortState.cheaperOnly = !relatedSortState.cheaperOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
            }
        });
        elements.detailRelatedStockAdvantageOnlyButton?.addEventListener("click", () => {
            relatedSortState.stockAdvantageOnly = !relatedSortState.stockAdvantageOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
            }
        });
        elements.detailRelatedSoldOutOnlyButton?.addEventListener("click", () => {
            relatedSortState.soldOutOnly = !relatedSortState.soldOutOnly;
            relatedSortState.availableOnly = false;
            if (currentProduct) {
                renderRelated(currentProduct);
            }
        });
        elements.detailBookmarkAllRelatedButton?.addEventListener("click", addAllRelatedToBookmark);
        elements.detailCopyRelatedLinksButton?.addEventListener("click", copyRelatedProductLinks);
        elements.detailResetRelatedFiltersButton?.addEventListener("click", () => {
            Object.assign(relatedSortState, {
                mode: "DEFAULT",
                lowStockOnly: false,
                sameBrandOnly: false,
                sameCategoryOnly: false,
                availableOnly: false,
                cheaperOnly: false,
                stockAdvantageOnly: false,
                soldOutOnly: false
            });
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast("연관 상품 조건을 초기화했습니다.", "기본 추천 순서와 전체 상품을 표시합니다.");
            }
        });
        elements.detailRandomRelatedButton?.addEventListener("click", () => {
            if (!currentProduct) {
                return;
            }
            const related = sortedRelatedProducts(currentProduct);
            if (!related.length) {
                showToast("이동할 연관 상품이 없습니다.", "현재 필터를 넓혀 다시 시도해주세요.", true);
                return;
            }
            const item = related[Math.floor(Math.random() * related.length)];
            window.location.href = buildProductUrl(item.id);
        });
        elements.detailRelatedLowStockOnlyButton?.addEventListener("click", () => {
            relatedSortState.lowStockOnly = !relatedSortState.lowStockOnly;
            if (currentProduct) {
                renderRelated(currentProduct);
                showToast(relatedSortState.lowStockOnly ? "긴장 재고 연관 상품만 표시합니다." : "전체 연관 상품 표시로 복구했습니다.", "비교 대상을 더 빠르게 좁힐 수 있습니다.");
            }
        });
        elements.detailCopyRelatedSummaryButton?.addEventListener("click", async () => {
            if (!currentProduct) {
                return;
            }
            const text = relatedSummaryText(currentProduct);
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("연관 상품 요약을 복사했습니다.", "비교 후보 상품 목록을 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("연관 상품 요약을 복사하세요.", text);
            }
        });
        elements.clearDetailRecentButton?.addEventListener("click", () => {
            if (window.StorefrontState) {
                window.StorefrontState.remove("recent");
            } else {
                try {
                    window.localStorage.removeItem(RECENT_VIEWED_KEY);
                } catch (error) {
                    showToast("최근 본 상품을 비우지 못했습니다.", "브라우저 저장소를 확인해주세요.", true);
                    return;
                }
            }
            renderRecentProducts(productId);
            showToast("최근 본 상품을 비웠습니다.", "상세 최근 흐름 보드가 초기화되었습니다.");
        });
        elements.detailRecentSortPriceButton?.addEventListener("click", () => {
            detailRecentState.sort = detailRecentState.sort === "PRICE_LOW" ? "RECENT" : "PRICE_LOW";
            renderRecentProducts(productId);
        });
        elements.detailRecentSortStockButton?.addEventListener("click", () => {
            detailRecentState.sort = detailRecentState.sort === "STOCK_ASC" ? "RECENT" : "STOCK_ASC";
            renderRecentProducts(productId);
        });
        elements.detailRecentAvailableOnlyButton?.addEventListener("click", () => {
            detailRecentState.availableOnly = !detailRecentState.availableOnly;
            renderRecentProducts(productId);
        });
        elements.detailRecentCompareAllButton?.addEventListener("click", () => addDetailRecentToBoard("COMPARE"));
        elements.detailRecentBookmarkAllButton?.addEventListener("click", () => addDetailRecentToBoard("BOOKMARK"));
        elements.copyDetailRecentSummaryButton?.addEventListener("click", async () => {
            const recentProducts = visibleDetailRecentProducts(productId);
            const text = recentProducts.length
                ? recentProducts.map((item, index) => `${index + 1}. ${item.headline || item.name} · ${item.model || "-"} · ${item.priceLabel || formatPrice(item.price)}`).join("\n")
                : "최근 본 상품이 없습니다.";
            try {
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(text);
                }
                showToast("최근 흐름을 복사했습니다.", "상세에서 이어 본 상품 목록을 바로 전달할 수 있습니다.");
            } catch (error) {
                window.prompt("최근 흐름을 복사하세요.", text);
            }
        });
        elements.copyDetailRecentLinksButton?.addEventListener("click", copyRecentProductLinks);
        elements.detailReviewMoreButton?.addEventListener("click", () => loadReviews(detailReviewState.page + 1, true));
        elements.detailReviewSort?.addEventListener("change", event => {
            detailReviewState.sort = event.currentTarget.value;
            loadReviews(0);
        });
        elements.detailReviewWriteButton?.addEventListener("click", () => setReviewFormOpen(true));
        elements.detailReviewFormCloseButton?.addEventListener("click", () => setReviewFormOpen(false));
        elements.detailReviewContent?.addEventListener("input", updateReviewContentCount);
        elements.detailReviewForm?.addEventListener("submit", submitReview);
        elements.detailReviewList?.addEventListener("click", event => {
            const button = event.target.closest("[data-review-report-id]");
            if (button) openReportModal(Number(button.dataset.reviewReportId));
        });
        try {
            const response = await fetch(`/api/front/products/${productId}`);
            if (!response.ok) {
                throw new Error("상품 상세를 불러오지 못했습니다.");
            }
            const product = normalizeDetailProduct(await response.json());
            currentProduct = product;
            restoreRememberedOption(product);
            if (elements.detailRetryButton) {
                elements.detailRetryButton.hidden = true;
            }
            document.title = `${product.name} | NOREN`;
            if (elements.detailTitle) {
                elements.detailTitle.textContent = product.headline || product.name;
            }
            if (elements.detailDescription) {
                elements.detailDescription.textContent = product.description || "상품 설명이 아직 등록되지 않았습니다.";
            }
            setElementText(elements.detailGuideDescription, product.description || "상품 설명이 아직 등록되지 않았습니다.");
            setElementText(elements.detailGuideModel, product.model || "-");
            setElementText(elements.detailGuideCategory, product.category || "-");
            if (elements.detailPrice) {
                elements.detailPrice.textContent = product.priceLabel || formatPrice(product.price);
            }
            if (elements.detailStockText) {
                elements.detailStockText.textContent = `총 재고 ${product.stock}개 · 등록 ${product.createdDate || "-"}`;
            }
            saveRecentProduct(product);
            renderMeta(product);
            renderSignals(product);
            renderOverview(product);
            renderOptions(product);
            const restoredOption = (product.options || []).find((option) => option.name === selectedOptionName);
            syncSelectedOptionActions(restoredOption || {});
            renderRelated(product);
            renderRecentProducts(product.id);
            loadReviews();
            if (reviewOrderNumber) {
                setReviewFormOpen(true);
                document.getElementById("detailReviews")?.scrollIntoView({ behavior: "smooth", block: "start" });
            }
            syncActionButtons();
        } catch (error) {
            if (elements.detailTitle) {
                elements.detailTitle.textContent = "상품 상세를 불러오지 못했습니다.";
            }
            if (elements.detailDescription) {
                elements.detailDescription.textContent = "잠시 후 다시 시도해주세요.";
            }
            if (elements.detailRetryButton) {
                elements.detailRetryButton.hidden = false;
            }
        }
    }

    function detailText(value, maxLength, required = false) {
        const text = String(value ?? "").trim().replace(/\s+/g, " ");
        if ((required && !text) || text.length > maxLength) throw new Error("상품 문자 정보가 올바르지 않습니다.");
        return text;
    }

    function detailInteger(value, fieldName, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }

    function detailImage(value) {
        const image = detailText(value, 500);
        return /^\/(?!\/)/.test(image) || /^https?:\/\//i.test(image) ? image : PRODUCT_IMAGE_FALLBACK_URL;
    }

    function normalizeDetailOptions(value) {
        if (!Array.isArray(value) || value.length > 100) throw new Error("상품 옵션 정보가 올바르지 않습니다.");
        const ids = new Set();
        const names = new Set();
        return value.map((option) => {
            const id = detailInteger(option?.id, "옵션 번호", 1);
            const name = detailText(option?.name, 100, true);
            if (ids.has(id) || names.has(name)) throw new Error("상품 옵션 정보가 중복되었습니다.");
            ids.add(id);
            names.add(name);
            return {
                id,
                name,
                stock: detailInteger(option.stock, "옵션 재고"),
                additionalPrice: detailInteger(option.additionalPrice, "옵션 추가 금액")
            };
        });
    }

    function normalizeRelatedProducts(value, currentId) {
        if (!Array.isArray(value) || value.length > 6) throw new Error("연관 상품 정보가 올바르지 않습니다.");
        const ids = new Set();
        return value.map((item) => {
            const id = detailInteger(item?.id, "연관 상품 번호", 1);
            if (id === currentId || ids.has(id)) throw new Error("연관 상품 정보가 중복되었습니다.");
            ids.add(id);
            const price = detailInteger(item.price, "연관 상품 가격");
            const stock = detailInteger(item.stock, "연관 상품 재고");
            return {
                id,
                brand: detailText(item.brand, 100, true),
                category: detailText(item.category, 100, true),
                name: detailText(item.name, 200, true),
                reason: detailText(item.reason, 500),
                model: detailText(item.model, 100),
                price,
                stock,
                stockStatus: detailText(item.stockStatus, 40) || stockLabel(stock),
                priceLabel: formatPrice(price),
                thumbnailUrl: detailImage(item.thumbnailUrl)
            };
        });
    }

    function normalizeDetailProduct(value) {
        if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("상품 상세 응답이 올바르지 않습니다.");
        const id = detailInteger(value.id, "상품 번호", 1);
        if (id !== productId) throw new Error("조회한 상품과 응답 정보가 일치하지 않습니다.");
        const price = detailInteger(value.price, "상품 가격");
        const stock = detailInteger(value.stock, "상품 재고");
        const options = normalizeDetailOptions(value.options);
        if (options.length && options.reduce((sum, option) => sum + option.stock, 0) !== stock) {
            throw new Error("상품 재고가 옵션 합계와 일치하지 않습니다.");
        }
        return {
            id,
            brand: detailText(value.brand, 100, true),
            category: detailText(value.category, 100, true),
            name: detailText(value.name, 200, true),
            headline: detailText(value.headline, 200),
            model: detailText(value.model, 100),
            price,
            stock,
            createdDate: detailText(value.createdDate, 30),
            description: detailText(value.description, 2000),
            mood: detailText(value.mood, 100),
            featured: value.featured === true,
            featuredRank: Number.isSafeInteger(value.featuredRank) && value.featuredRank > 0 ? value.featuredRank : null,
            stockStatus: detailText(value.stockStatus, 40) || stockLabel(stock),
            priceLabel: formatPrice(price),
            options,
            relatedProducts: normalizeRelatedProducts(value.relatedProducts, id),
            thumbnailUrl: detailImage(value.thumbnailUrl)
        };
    }

    init();
})();
