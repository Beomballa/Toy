(function () {
    const BOOKMARK_PRODUCTS_KEY = window.StorefrontState?.keys.bookmark || "front-bookmark-products";
    const COMPARE_PRODUCTS_KEY = window.StorefrontState?.keys.compare || "front-compare-products";
    const RECENT_VIEWED_KEY = "front-recent-viewed-products";
    const SAVED_VIEWS_KEY = "front-saved-views";
    const SEARCH_HISTORY_KEY = "front-search-history";
    const LAST_CATALOG_STATE_KEY = "front-last-catalog-state";
    const LAST_DRAWER_PRODUCT_KEY = "front-last-drawer-product";
    const HIDDEN_PRODUCTS_KEY = "front-hidden-products";
    const VIEW_MODE_KEY = "front-catalog-view-mode";
    const DISPLAY_PREFERENCES_KEY = "front-catalog-display-preferences";
    const PAGE_SIZE_KEY = "front-catalog-page-size";
    const CATALOG_CACHE_KEY = "front-catalog-session-cache";
    const SCROLL_POSITION_KEY = "front-catalog-scroll-position";
    const FILTER_PANEL_OPEN_KEY = "front-catalog-filter-panel-open";
    const PRODUCT_IMAGE_FALLBACK_URL = "/images/product-placeholder.svg";
    const SELECTED_PRODUCTS_SESSION_KEY = "front-selected-product-ids";
    const DEFAULT_STATE = {
        search: "",
        brand: "ALL",
        category: "ALL",
        stock: "ALL",
        sort: "LATEST",
        lowStockThreshold: "20",
        featuredOnly: "ALL",
        priceBand: "ALL"
    };
    const state = {
        ...DEFAULT_STATE
    };
    const savedDisplayPreferences = readDisplayPreferences();
    const uiState = {
        todayOnly: false,
        showHiddenProducts: false,
        viewMode: readStorageValue(window.localStorage, VIEW_MODE_KEY) === "COMPACT" ? "COMPACT" : "DEFAULT",
        layout: savedDisplayPreferences.layout || (readStorageValue(window.localStorage, VIEW_MODE_KEY) === "COMPACT" ? "COMFORT" : "STANDARD"),
        hideDescriptions: Boolean(savedDisplayPreferences.hideDescriptions),
        hideSignals: Boolean(savedDisplayPreferences.hideSignals),
        hideActions: Boolean(savedDisplayPreferences.hideActions),
        hideMetrics: Boolean(savedDisplayPreferences.hideMetrics),
        hideDistribution: Boolean(savedDisplayPreferences.hideDistribution),
        hideDecision: Boolean(savedDisplayPreferences.hideDecision),
        reducedMotion: Boolean(savedDisplayPreferences.reducedMotion)
    };
    let products = [];
    let metrics = {
        totalCount: 0,
        lowStockCount: 0,
        latestCreatedDate: null,
        latestDropCount: 0,
        featuredCount: 0,
        totalStock: 0,
        averagePrice: 0,
        minimumPrice: 0,
        maximumPrice: 0,
        brandCount: 0,
        under200Count: 0,
        between200And300Count: 0,
        over300Count: 0
    };
    let brandFacets = [];
    let categoryFacets = [];
    let homeCollections = {
        recommended: [],
        ranking: [],
        fastDelivery: [],
        latestDrops: [],
        lowStock: []
    };
    let catalogPagination = {
        page: 0,
        size: 12,
        totalElements: 0,
        totalPages: 0,
        first: true,
        last: true
    };
    let searchIndexProducts = [];
    let activeSearchSuggestions = [];
    let activeSearchSuggestionIndex = -1;
    let searchDebounceTimer = null;
    let catalogLoadError = "";
    let catalogRequestController = null;
    let catalogRequestSequence = 0;
    let contentHighlightRequestSequence = 0;
    let shortcutHelpReturnFocus = null;
    let headerSearchReturnFocus = null;
    let mobileMenuReturnFocus = null;
    let networkStatusDismissed = false;
    const detailCache = new Map();
    let toastTimerSeed = 0;
    const boardState = {
        recentSort: "RECENT",
        recentFilter: "ALL",
        recentExpanded: false,
        compareSort: "DEFAULT",
        bookmarkSort: "RECENT",
        bookmarkFilter: "ALL",
        latestSort: "DEFAULT",
        lowStockSort: "STOCK_ASC",
        featuredSort: "DEFAULT"
    };
    const drawerState = {
        optionLowStockOnly: false,
        optionAvailableOnly: false,
        optionStableOnly: false,
        optionSort: "DEFAULT",
        relatedSort: "DEFAULT",
        relatedSameBrandOnly: false,
        relatedAvailableOnly: false,
        relatedSameCategoryOnly: false
    };
    const memoryState = {
        savedReversed: false,
        searchAlphabetical: false,
        hiddenAlphabetical: false
    };
    const selectedProductIds = new Set();
    const selectionHistory = [];
    const selectionFuture = [];
    const paginationState = {
        page: 1,
        size: ["6", "12", "24", "48"].includes(readStorageValue(window.localStorage, PAGE_SIZE_KEY))
            ? readStorageValue(window.localStorage, PAGE_SIZE_KEY)
            : "12",
        extra: 0
    };
    const heroSlides = [
        {
            eyebrow: "2026 Summer Edit",
            title: "Still<br>in motion",
            description: "멈추지 않는 여름의 마지막 장면과 지금 입기 좋은 셀렉션을 만나보세요.",
            image: "/images/campaign/summer-edit/frame-01.jpg",
            tone: "LIGHT"
        },
        {
            eyebrow: "Everyday Form",
            title: "가볍게<br>이어지는 태도",
            description: "차분한 색과 자연스러운 실루엣으로 완성한 NOREN의 데일리 에디트입니다.",
            image: "/images/campaign/summer-edit/frame-02.jpg",
            tone: "LIGHT"
        },
        {
            eyebrow: "Last Summer Scene",
            title: "선명하게<br>남는 선택",
            description: "신규 드롭과 안정 재고 상품을 한 흐름에서 빠르게 비교해보세요.",
            image: "/images/campaign/summer-edit/frame-03.jpg",
            tone: "LIGHT"
        }
    ];
    let activeHeroSlide = 0;
    let heroCarouselTimer = null;
    let heroPointerStartX = null;
    let heroCarouselPaused = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const productRailSwipers = new Map();
    let drawerReturnFocus = null;
    let activeDrawerProductId = 0;
    let previousDrawerProductId = 0;
    let nextDrawerProductId = 0;
    let drawerRequestSequence = 0;
    let drawerRequestController = null;

    const elements = {
        brandFilter: document.getElementById("brandFilter"),
        categoryFilter: document.getElementById("categoryFilter"),
        stockFilter: document.getElementById("stockFilter"),
        featuredOnlyFilter: document.getElementById("featuredOnlyFilter"),
        priceBandFilter: document.getElementById("priceBandFilter"),
        lowStockThresholdFilter: document.getElementById("lowStockThresholdFilter"),
        sortFilter: document.getElementById("sortFilter"),
        catalogFilterPanel: document.getElementById("catalogFilterPanel"),
        catalogFilterCount: document.getElementById("catalogFilterCount"),
        searchInput: document.getElementById("searchInput"),
        clearInlineSearchButton: document.getElementById("clearInlineSearchButton"),
        searchAssist: document.getElementById("searchAssist"),
        searchResultStatus: document.getElementById("searchResultStatus"),
        searchSuggestionList: document.getElementById("searchSuggestionList"),
        catalogGrid: document.getElementById("catalogGrid"),
        catalogDisplayStatus: document.getElementById("catalogDisplayStatus"),
        catalogLayoutShopButton: document.getElementById("catalogLayoutShopButton"),
        catalogLayoutStandardButton: document.getElementById("catalogLayoutStandardButton"),
        catalogLayoutComfortButton: document.getElementById("catalogLayoutComfortButton"),
        catalogLayoutListButton: document.getElementById("catalogLayoutListButton"),
        toggleCatalogDescriptionButton: document.getElementById("toggleCatalogDescriptionButton"),
        toggleCatalogSignalsButton: document.getElementById("toggleCatalogSignalsButton"),
        toggleCatalogActionsButton: document.getElementById("toggleCatalogActionsButton"),
        toggleCatalogMetricsButton: document.getElementById("toggleCatalogMetricsButton"),
        toggleCatalogDistributionButton: document.getElementById("toggleCatalogDistributionButton"),
        toggleCatalogDecisionButton: document.getElementById("toggleCatalogDecisionButton"),
        compactCatalogAnalyticsButton: document.getElementById("compactCatalogAnalyticsButton"),
        resetCatalogAnalyticsButton: document.getElementById("resetCatalogAnalyticsButton"),
        toggleReducedMotionButton: document.getElementById("toggleReducedMotionButton"),
        resetCatalogDisplayButton: document.getElementById("resetCatalogDisplayButton"),
        catalogPagination: document.getElementById("catalogPagination"),
        catalogPageProgress: document.getElementById("catalogPageProgress"),
        catalogPageRange: document.getElementById("catalogPageRange"),
        catalogPageSize: document.getElementById("catalogPageSize"),
        catalogPageSelect: document.getElementById("catalogPageSelect"),
        catalogFirstPageButton: document.getElementById("catalogFirstPageButton"),
        catalogPreviousPageButton: document.getElementById("catalogPreviousPageButton"),
        catalogNextPageButton: document.getElementById("catalogNextPageButton"),
        catalogLastPageButton: document.getElementById("catalogLastPageButton"),
        catalogLoadMoreButton: document.getElementById("catalogLoadMoreButton"),
        copyCurrentPageLinksButton: document.getElementById("copyCurrentPageLinksButton"),
        catalogPageAveragePrice: document.getElementById("catalogPageAveragePrice"),
        catalogPageTotalStock: document.getElementById("catalogPageTotalStock"),
        catalogPageLowStockCount: document.getElementById("catalogPageLowStockCount"),
        catalogPageFeaturedCount: document.getElementById("catalogPageFeaturedCount"),
        catalogPageSelectedCount: document.getElementById("catalogPageSelectedCount"),
        catalogSelection: document.getElementById("catalogSelection"),
        catalogSelectionTitle: document.getElementById("catalogSelectionTitle"),
        catalogSelectionText: document.getElementById("catalogSelectionText"),
        catalogSelectionCount: document.getElementById("catalogSelectionCount"),
        catalogSelectionTotalPrice: document.getElementById("catalogSelectionTotalPrice"),
        catalogSelectionTotalStock: document.getElementById("catalogSelectionTotalStock"),
        catalogSelectionAveragePrice: document.getElementById("catalogSelectionAveragePrice"),
        catalogSelectionBrandCount: document.getElementById("catalogSelectionBrandCount"),
        catalogSelectionLowStockCount: document.getElementById("catalogSelectionLowStockCount"),
        catalogSelectionSoldOutCount: document.getElementById("catalogSelectionSoldOutCount"),
        catalogSelectionCoverage: document.getElementById("catalogSelectionCoverage"),
        catalogSelectionPriceShare: document.getElementById("catalogSelectionPriceShare"),
        catalogSelectionStockShare: document.getElementById("catalogSelectionStockShare"),
        catalogSelectionAvailableCount: document.getElementById("catalogSelectionAvailableCount"),
        catalogSelectionDominantCategory: document.getElementById("catalogSelectionDominantCategory"),
        catalogSelectionHistoryCount: document.getElementById("catalogSelectionHistoryCount"),
        catalogSelectionHistoryText: document.getElementById("catalogSelectionHistoryText"),
        undoCatalogSelectionButton: document.getElementById("undoCatalogSelectionButton"),
        redoCatalogSelectionButton: document.getElementById("redoCatalogSelectionButton"),
        clearCatalogSelectionHistoryButton: document.getElementById("clearCatalogSelectionHistoryButton"),
        selectVisibleProductsButton: document.getElementById("selectVisibleProductsButton"),
        clearSelectedProductsButton: document.getElementById("clearSelectedProductsButton"),
        compareSelectedProductsButton: document.getElementById("compareSelectedProductsButton"),
        bookmarkSelectedProductsButton: document.getElementById("bookmarkSelectedProductsButton"),
        hideSelectedProductsButton: document.getElementById("hideSelectedProductsButton"),
        copySelectedSummaryButton: document.getElementById("copySelectedSummaryButton"),
        copySelectedLinksButton: document.getElementById("copySelectedLinksButton"),
        openUrgentSelectedButton: document.getElementById("openUrgentSelectedButton"),
        focusSelectedBrandButton: document.getElementById("focusSelectedBrandButton"),
        selectLowStockPageButton: document.getElementById("selectLowStockPageButton"),
        selectFeaturedPageButton: document.getElementById("selectFeaturedPageButton"),
        selectAvailablePageButton: document.getElementById("selectAvailablePageButton"),
        selectStablePageButton: document.getElementById("selectStablePageButton"),
        selectPremiumPageButton: document.getElementById("selectPremiumPageButton"),
        selectDominantBrandPageButton: document.getElementById("selectDominantBrandPageButton"),
        removeLowStockSelectionButton: document.getElementById("removeLowStockSelectionButton"),
        invertPageSelectionButton: document.getElementById("invertPageSelectionButton"),
        removeSoldOutSelectionButton: document.getElementById("removeSoldOutSelectionButton"),
        selectCheapestPageButton: document.getElementById("selectCheapestPageButton"),
        selectHighestStockPageButton: document.getElementById("selectHighestStockPageButton"),
        selectSoldOutPageButton: document.getElementById("selectSoldOutPageButton"),
        openCheapestSelectedButton: document.getElementById("openCheapestSelectedButton"),
        openHighestStockSelectedButton: document.getElementById("openHighestStockSelectedButton"),
        copyCurrentPageSummaryButton: document.getElementById("copyCurrentPageSummaryButton"),
        exportCurrentPageCsvButton: document.getElementById("exportCurrentPageCsvButton"),
        exportSelectedProductsCsvButton: document.getElementById("exportSelectedProductsCsvButton"),
        catalogInsightGrid: document.getElementById("catalogInsightGrid"),
        catalogPresetStrip: document.getElementById("catalogPresetStrip"),
        catalogTags: document.getElementById("catalogTags"),
        catalogCountText: document.getElementById("catalogCountText"),
        catalogSummaryText: document.getElementById("catalogSummaryText"),
        catalogAveragePrice: document.getElementById("catalogAveragePrice"),
        catalogPriceRange: document.getElementById("catalogPriceRange"),
        catalogTotalStock: document.getElementById("catalogTotalStock"),
        catalogBrandCount: document.getElementById("catalogBrandCount"),
        catalogLowStockCount: document.getElementById("catalogLowStockCount"),
        catalogLiveMetrics: document.getElementById("catalogLiveMetrics"),
        catalogDistribution: document.getElementById("catalogDistribution"),
        catalogDecisionRail: document.getElementById("catalogDecisionRail"),
        catalogUnderPriceRate: document.getElementById("catalogUnderPriceRate"),
        catalogMiddlePriceRate: document.getElementById("catalogMiddlePriceRate"),
        catalogHighPriceRate: document.getElementById("catalogHighPriceRate"),
        catalogLowStockRate: document.getElementById("catalogLowStockRate"),
        catalogFeaturedRate: document.getElementById("catalogFeaturedRate"),
        catalogUnderPriceBar: document.getElementById("catalogUnderPriceBar"),
        catalogMiddlePriceBar: document.getElementById("catalogMiddlePriceBar"),
        catalogHighPriceBar: document.getElementById("catalogHighPriceBar"),
        catalogLowStockBar: document.getElementById("catalogLowStockBar"),
        catalogFeaturedBar: document.getElementById("catalogFeaturedBar"),
        catalogSearchQuality: document.getElementById("catalogSearchQuality"),
        catalogSearchNameMatch: document.getElementById("catalogSearchNameMatch"),
        catalogSearchBrandMatch: document.getElementById("catalogSearchBrandMatch"),
        catalogSearchModelMatch: document.getElementById("catalogSearchModelMatch"),
        catalogSearchAvailableMatch: document.getElementById("catalogSearchAvailableMatch"),
        catalogSearchFilterCount: document.getElementById("catalogSearchFilterCount"),
        catalogCheapestLabel: document.getElementById("catalogCheapestLabel"),
        catalogHighestStockLabel: document.getElementById("catalogHighestStockLabel"),
        catalogUrgentLabel: document.getElementById("catalogUrgentLabel"),
        catalogDominantBrandLabel: document.getElementById("catalogDominantBrandLabel"),
        openCheapestCatalogButton: document.getElementById("openCheapestCatalogButton"),
        openHighestStockCatalogButton: document.getElementById("openHighestStockCatalogButton"),
        openUrgentCatalogButton: document.getElementById("openUrgentCatalogButton"),
        focusDominantCatalogBrandButton: document.getElementById("focusDominantCatalogBrandButton"),
        copyCatalogDecisionButton: document.getElementById("copyCatalogDecisionButton"),
        shareCatalogButton: document.getElementById("shareCatalogButton"),
        copyCatalogSummaryButton: document.getElementById("copyCatalogSummaryButton"),
        jumpFirstProductButton: document.getElementById("jumpFirstProductButton"),
        randomProductButton: document.getElementById("randomProductButton"),
        clearSearchButton: document.getElementById("clearSearchButton"),
        saveCurrentViewButton: document.getElementById("saveCurrentViewButton"),
        restoreLastStateButton: document.getElementById("restoreLastStateButton"),
        applyLatestSavedViewButton: document.getElementById("applyLatestSavedViewButton"),
        applyOldestSavedViewButton: document.getElementById("applyOldestSavedViewButton"),
        removeLatestSavedViewButton: document.getElementById("removeLatestSavedViewButton"),
        removeOldestSavedViewButton: document.getElementById("removeOldestSavedViewButton"),
        exportSavedViewsButton: document.getElementById("exportSavedViewsButton"),
        reverseSavedViewsButton: document.getElementById("reverseSavedViewsButton"),
        copySavedViewsButton: document.getElementById("copySavedViewsButton"),
        clearSearchHistoryButton: document.getElementById("clearSearchHistoryButton"),
        sortSearchHistoryButton: document.getElementById("sortSearchHistoryButton"),
        copySearchHistoryButton: document.getElementById("copySearchHistoryButton"),
        applyLatestSearchButton: document.getElementById("applyLatestSearchButton"),
        applyOldestSearchButton: document.getElementById("applyOldestSearchButton"),
        removeLatestSearchButton: document.getElementById("removeLatestSearchButton"),
        removeOldestSearchButton: document.getElementById("removeOldestSearchButton"),
        exportSearchHistoryButton: document.getElementById("exportSearchHistoryButton"),
        reopenLastDrawerButton: document.getElementById("reopenLastDrawerButton"),
        clearHiddenProductsButton: document.getElementById("clearHiddenProductsButton"),
        toggleHiddenViewButton: document.getElementById("toggleHiddenViewButton"),
        sortHiddenProductsButton: document.getElementById("sortHiddenProductsButton"),
        copyHiddenProductsButton: document.getElementById("copyHiddenProductsButton"),
        restoreLatestHiddenButton: document.getElementById("restoreLatestHiddenButton"),
        bookmarkHiddenProductsButton: document.getElementById("bookmarkHiddenProductsButton"),
        savedViewList: document.getElementById("savedViewList"),
        searchHistoryList: document.getElementById("searchHistoryList"),
        hiddenProductList: document.getElementById("hiddenProductList"),
        savedViewCount: document.getElementById("savedViewCount"),
        searchHistoryCount: document.getElementById("searchHistoryCount"),
        hiddenProductCount: document.getElementById("hiddenProductCount"),
        brandSpotlightGrid: document.getElementById("brandSpotlightGrid"),
        categoryShortcutGrid: document.getElementById("categoryShortcutGrid"),
        latestDropGrid: document.getElementById("latestDropGrid"),
        lowStockGrid: document.getElementById("lowStockGrid"),
        latestDropPreviousButton: document.getElementById("latestDropPreviousButton"),
        latestDropNextButton: document.getElementById("latestDropNextButton"),
        lowStockPreviousButton: document.getElementById("lowStockPreviousButton"),
        lowStockNextButton: document.getElementById("lowStockNextButton"),
        featuredGrid: document.getElementById("featuredGrid"),
        sortLatestPriceButton: document.getElementById("sortLatestPriceButton"),
        bookmarkLatestButton: document.getElementById("bookmarkLatestButton"),
        copyLatestSummaryButton: document.getElementById("copyLatestSummaryButton"),
        sortLowStockPriceButton: document.getElementById("sortLowStockPriceButton"),
        compareLowStockButton: document.getElementById("compareLowStockButton"),
        copyLowStockSummaryButton: document.getElementById("copyLowStockSummaryButton"),
        sortFeaturedPriceButton: document.getElementById("sortFeaturedPriceButton"),
        sortFeaturedStockButton: document.getElementById("sortFeaturedStockButton"),
        bookmarkFeaturedButton: document.getElementById("bookmarkFeaturedButton"),
        copyFeaturedLinksButton: document.getElementById("copyFeaturedLinksButton"),
        recentViewedSection: document.getElementById("recentViewedSection"),
        recentViewedTitle: document.getElementById("recentViewedTitle"),
        recentViewedText: document.getElementById("recentViewedText"),
        recentViewedGrid: document.getElementById("recentViewedGrid"),
        clearRecentViewedButton: document.getElementById("clearRecentViewedButton"),
        copyRecentViewedSummaryButton: document.getElementById("copyRecentViewedSummaryButton"),
        focusRecentLowStockButton: document.getElementById("focusRecentLowStockButton"),
        sortRecentPriceButton: document.getElementById("sortRecentPriceButton"),
        sortRecentStockButton: document.getElementById("sortRecentStockButton"),
        addRecentToCompareButton: document.getElementById("addRecentToCompareButton"),
        addRecentToBookmarkButton: document.getElementById("addRecentToBookmarkButton"),
        sortRecentNameButton: document.getElementById("sortRecentNameButton"),
        filterRecentLowStockButton: document.getElementById("filterRecentLowStockButton"),
        filterRecentBrandButton: document.getElementById("filterRecentBrandButton"),
        filterRecentAvailableButton: document.getElementById("filterRecentAvailableButton"),
        filterRecentSoldOutButton: document.getElementById("filterRecentSoldOutButton"),
        sortRecentOldestButton: document.getElementById("sortRecentOldestButton"),
        toggleRecentExpandedButton: document.getElementById("toggleRecentExpandedButton"),
        copyRecentViewedLinksButton: document.getElementById("copyRecentViewedLinksButton"),
        resetRecentBoardFilterButton: document.getElementById("resetRecentBoardFilterButton"),
        openRecommendedRecentButton: document.getElementById("openRecommendedRecentButton"),
        compareBoardSection: document.getElementById("compareBoardSection"),
        compareBoardGrid: document.getElementById("compareBoardGrid"),
        compareBoardTitle: document.getElementById("compareBoardTitle"),
        compareBoardText: document.getElementById("compareBoardText"),
        compareAveragePrice: document.getElementById("compareAveragePrice"),
        comparePriceGap: document.getElementById("comparePriceGap"),
        compareTotalStock: document.getElementById("compareTotalStock"),
        compareStockGap: document.getElementById("compareStockGap"),
        compareCategoryCount: document.getElementById("compareCategoryCount"),
        applyCompareCategoryButton: document.getElementById("applyCompareCategoryButton"),
        applyCompareLowStockButton: document.getElementById("applyCompareLowStockButton"),
        sortComparePriceButton: document.getElementById("sortComparePriceButton"),
        sortCompareStockButton: document.getElementById("sortCompareStockButton"),
        copyCompareSummaryButton: document.getElementById("copyCompareSummaryButton"),
        addCompareToBookmarkButton: document.getElementById("addCompareToBookmarkButton"),
        openCheapestCompareButton: document.getElementById("openCheapestCompareButton"),
        openHighestStockCompareButton: document.getElementById("openHighestStockCompareButton"),
        openLowestStockCompareButton: document.getElementById("openLowestStockCompareButton"),
        openHighestPriceCompareButton: document.getElementById("openHighestPriceCompareButton"),
        sortCompareOldestButton: document.getElementById("sortCompareOldestButton"),
        exportCompareCsvButton: document.getElementById("exportCompareCsvButton"),
        sortComparePriceLowButton: document.getElementById("sortComparePriceLowButton"),
        sortCompareNameButton: document.getElementById("sortCompareNameButton"),
        copyCompareLinksButton: document.getElementById("copyCompareLinksButton"),
        openRecommendedCompareButton: document.getElementById("openRecommendedCompareButton"),
        clearCompareButton: document.getElementById("clearCompareButton"),
        bookmarkBoardSection: document.getElementById("bookmarkBoardSection"),
        bookmarkBoardGrid: document.getElementById("bookmarkBoardGrid"),
        bookmarkBoardTitle: document.getElementById("bookmarkBoardTitle"),
        bookmarkBoardText: document.getElementById("bookmarkBoardText"),
        bookmarkAveragePrice: document.getElementById("bookmarkAveragePrice"),
        bookmarkTotalStock: document.getElementById("bookmarkTotalStock"),
        bookmarkLowStockCount: document.getElementById("bookmarkLowStockCount"),
        bookmarkFeaturedCount: document.getElementById("bookmarkFeaturedCount"),
        bookmarkDominantCategory: document.getElementById("bookmarkDominantCategory"),
        applyBookmarkFeaturedButton: document.getElementById("applyBookmarkFeaturedButton"),
        applyBookmarkLowStockButton: document.getElementById("applyBookmarkLowStockButton"),
        sortBookmarkRecentButton: document.getElementById("sortBookmarkRecentButton"),
        sortBookmarkFeaturedButton: document.getElementById("sortBookmarkFeaturedButton"),
        sortBookmarkPriceButton: document.getElementById("sortBookmarkPriceButton"),
        sortBookmarkStockButton: document.getElementById("sortBookmarkStockButton"),
        addBookmarkToCompareButton: document.getElementById("addBookmarkToCompareButton"),
        copyBookmarkLinksButton: document.getElementById("copyBookmarkLinksButton"),
        copyBookmarkSummaryButton: document.getElementById("copyBookmarkSummaryButton"),
        sortBookmarkNameButton: document.getElementById("sortBookmarkNameButton"),
        filterBookmarkLowStockButton: document.getElementById("filterBookmarkLowStockButton"),
        filterBookmarkFeaturedButton: document.getElementById("filterBookmarkFeaturedButton"),
        filterBookmarkAvailableButton: document.getElementById("filterBookmarkAvailableButton"),
        filterBookmarkSoldOutButton: document.getElementById("filterBookmarkSoldOutButton"),
        filterBookmarkCategoryButton: document.getElementById("filterBookmarkCategoryButton"),
        sortBookmarkOldestButton: document.getElementById("sortBookmarkOldestButton"),
        exportBookmarkCsvButton: document.getElementById("exportBookmarkCsvButton"),
        resetBookmarkBoardFilterButton: document.getElementById("resetBookmarkBoardFilterButton"),
        openRecommendedBookmarkButton: document.getElementById("openRecommendedBookmarkButton"),
        clearBookmarkButton: document.getElementById("clearBookmarkButton"),
        toggleCompactViewButton: document.getElementById("toggleCompactViewButton"),
        toggleTodayOnlyButton: document.getElementById("toggleTodayOnlyButton"),
        signalList: document.getElementById("signalList"),
        todaySignalTitle: document.getElementById("todaySignalTitle"),
        todaySignalText: document.getElementById("todaySignalText"),
        metricCount: document.getElementById("metricCount"),
        metricLowStock: document.getElementById("metricLowStock"),
        metricToday: document.getElementById("metricToday"),
        flowBoardTitle: document.getElementById("flowBoardTitle"),
        flowBoardText: document.getElementById("flowBoardText"),
        flowBoardGrid: document.getElementById("flowBoardGrid"),
        popularHighlightList: document.getElementById("popularHighlightList"),
        popularHighlightRange: document.getElementById("popularHighlightRange"),
        noticeHighlightList: document.getElementById("noticeHighlightList"),
        styleHighlightList: document.getElementById("styleHighlightList"),
        contentHighlightStatus: document.getElementById("contentHighlightStatus"),
        contentHighlightRetryButton: document.getElementById("contentHighlightRetryButton"),
        copyFlowBoardSummaryButton: document.getElementById("copyFlowBoardSummaryButton"),
        openRecentFlowButton: document.getElementById("openRecentFlowButton"),
        openCompareFlowButton: document.getElementById("openCompareFlowButton"),
        openBookmarkFlowButton: document.getElementById("openBookmarkFlowButton"),
        applyFlowLowStockButton: document.getElementById("applyFlowLowStockButton"),
        restoreHiddenFlowButton: document.getElementById("restoreHiddenFlowButton"),
        productDrawer: document.getElementById("productDrawer"),
        drawerBody: document.getElementById("drawerBody"),
        closeDrawerButton: document.getElementById("closeDrawerButton"),
        focusLowStockButton: document.getElementById("focusLowStockButton"),
        openDrawerFromTop: document.getElementById("openDrawerFromTop"),
        headerSearchPanel: document.getElementById("headerSearchPanel"),
        headerSearchInput: document.getElementById("headerSearchInput"),
        submitHeaderSearchButton: document.getElementById("submitHeaderSearchButton"),
        closeHeaderSearchButton: document.getElementById("closeHeaderSearchButton"),
        mobileMenuButton: document.getElementById("mobileMenuButton"),
        topbarSubnav: document.getElementById("topbarSubnav"),
        mobileStoreNav: document.getElementById("mobileStoreNav"),
        mobileSavedCount: document.getElementById("mobileSavedCount"),
        utilityRecentCount: document.getElementById("utilityRecentCount"),
        utilityBookmarkCount: document.getElementById("utilityBookmarkCount"),
        utilityCompareCount: document.getElementById("utilityCompareCount"),
        resetPersonalDataButton: document.getElementById("resetPersonalDataButton"),
        homeCategoryRail: document.getElementById("homeCategoryRail"),
        heroPreviousButton: document.getElementById("heroPreviousButton"),
        heroNextButton: document.getElementById("heroNextButton"),
        heroSlideStatus: document.getElementById("heroSlideStatus"),
        heroDots: document.getElementById("heroDots"),
        heroPauseButton: document.getElementById("heroPauseButton"),
        heroCampaignImage: document.getElementById("heroCampaignImage"),
        resetFiltersButton: document.getElementById("resetFiltersButton"),
        scrollTopButton: document.getElementById("scrollTopButton"),
        scrollProgress: document.getElementById("storefrontScrollProgress"),
        storefrontStatus: document.getElementById("storefrontStatus"),
        networkStatus: document.getElementById("networkStatus"),
        networkStatusText: document.getElementById("networkStatusText"),
        networkRetryButton: document.getElementById("networkRetryButton"),
        networkDismissButton: document.getElementById("networkDismissButton"),
        keyboardHelpButton: document.getElementById("keyboardHelpButton"),
        shortcutHelpModal: document.getElementById("shortcutHelpModal"),
        shortcutHelpCloseButton: document.getElementById("shortcutHelpCloseButton")
    };

    async function init() {
        hydrateStateFromUrl();
        const [shouldRender] = await Promise.all([loadProducts(), loadHomeCollections()]);
        if (!shouldRender) {
            return;
        }
        populateFilters();
        syncControls();
        restoreSelectedProductIds();
        bindEvents();
        initActionMenuBehavior();
        restoreCatalogFilterPanelState();
        initSectionNavigation();
        initMobileStoreNavigation();
        renderHeroMetrics();
        renderFlowBoard();
        renderBrandSpotlight();
        renderCategoryShortcuts();
        renderSignalStrip();
        renderFeatured();
        renderSavedViews();
        renderSearchHistory();
        renderHiddenProducts();
        syncMemoryButtons();
        renderCatalogInsights();
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderSignals();
        renderCatalog();
        syncViewButtons();
        renderHeroSlide();
        initHeroCarousel();
        syncScrollState();
        syncNetworkStatus();
        restoreCatalogScrollPosition();
        refreshLucideIcons();
        void loadContentHighlights();
    }

    async function loadContentHighlights() {
        if (!elements.popularHighlightList || !elements.noticeHighlightList || !elements.styleHighlightList) {
            return;
        }
        const requestSequence = ++contentHighlightRequestSequence;
        renderContentHighlightState("LOADING");
        try {
            const response = await fetch("/api/front/content/highlights?limit=4");
            if (!response.ok) {
                throw new Error("콘텐츠를 불러오지 못했습니다.");
            }
            const payload = normalizeContentHighlights(await response.json());
            if (requestSequence !== contentHighlightRequestSequence) {
                return;
            }
            renderContentHighlights(payload);
        } catch (error) {
            if (requestSequence !== contentHighlightRequestSequence) {
                return;
            }
            renderContentHighlightState("ERROR");
        }
    }

    function renderContentHighlights(payload) {
        const notices = payload.notices;
        const styles = payload.styles;
        const popular = payload.popular;
        renderPopularContentHighlights(popular);
        renderContentHighlightList(elements.noticeHighlightList, notices, "현재 공개된 공지가 없습니다.");
        renderContentHighlightList(elements.styleHighlightList, styles, "현재 공개된 스타일 콘텐츠가 없습니다.");
        const range = payload?.popularStartDate && payload?.popularEndDate
            ? `${payload.popularStartDate} - ${payload.popularEndDate}`
            : "최근 7일";
        setText(elements.popularHighlightRange, range);
        elements.contentHighlightRetryButton.hidden = true;
        setText(
            elements.contentHighlightStatus,
            `인기 ${popular.length}개, 공지 ${notices.length}개, 스타일 ${styles.length}개를 불러왔습니다.`
        );
    }

    function renderPopularContentHighlights(items) {
        const target = elements.popularHighlightList;
        target.classList.remove("is-loading", "is-error");
        target.setAttribute("aria-busy", "false");
        if (!items.length) {
            target.innerHTML = '<div class="content-highlight-state">최근 7일 조회 데이터가 없습니다.</div>';
            return;
        }
        target.innerHTML = items.map((rawItem, index) => {
            const item = markupSafeObject(rawItem);
            const documentId = Number(item.id);
            const boardLabel = item.boardType === "NOTICE" ? "NOTICE" : "STYLE";
            const viewCount = Number(item.recentViewCount || 0).toLocaleString("ko-KR");
            const visitorCount = Number(item.uniqueVisitors || 0).toLocaleString("ko-KR");
            return `
                <article class="content-popular-item">
                    <a href="/front/content/${documentId}" aria-label="${item.title} 상세 보기">
                        <div class="content-popular-item__head">
                            <strong>${String(index + 1).padStart(2, "0")}</strong>
                            <span>${boardLabel}</span>
                        </div>
                        <h4>${item.title}</h4>
                        <p>${item.summary || "내용을 확인해 주세요."}</p>
                        <div class="content-popular-item__metrics">
                            <span>최근 조회 <b>${viewCount}</b></span>
                            <span>방문자 <b>${visitorCount}</b></span>
                        </div>
                    </a>
                </article>`;
        }).join("");
    }

    function renderContentHighlightList(target, items, emptyMessage) {
        target.classList.remove("is-loading", "is-error");
        target.setAttribute("aria-busy", "false");
        if (!items.length) {
            target.innerHTML = `<div class="content-highlight-state">${escapeMarkup(emptyMessage)}</div>`;
            return;
        }
        target.innerHTML = items.map((rawItem, index) => {
            const item = markupSafeObject(rawItem);
            const sequence = String(index + 1).padStart(2, "0");
            return `
                <article class="content-highlight-item">
                    <span class="content-highlight-item__number">${sequence}</span>
                    <a class="content-highlight-item__body" href="/front/content/${Number(item.id)}" aria-label="${item.title} 상세 보기">
                        <div class="content-highlight-item__meta">
                            ${item.pinned ? '<strong>PINNED</strong>' : ""}
                            <span>${item.createdDate || "최근 게시"}</span>
                            <span>조회 ${Number(item.viewCount || 0).toLocaleString("ko-KR")}</span>
                        </div>
                        <h4>${item.title}</h4>
                        <p>${item.summary || "내용을 확인해 주세요."}</p>
                    </a>
                </article>`;
        }).join("");
    }

    function renderContentHighlightState(stateName) {
        const isError = stateName === "ERROR";
        const message = isError ? "콘텐츠를 불러오지 못했습니다." : "콘텐츠를 불러오는 중입니다.";
        [elements.popularHighlightList, elements.noticeHighlightList, elements.styleHighlightList].forEach((target) => {
            target.classList.toggle("is-loading", !isError);
            target.classList.toggle("is-error", isError);
            target.setAttribute("aria-busy", String(!isError));
            target.innerHTML = `<div class="content-highlight-state">${message}</div>`;
        });
        elements.contentHighlightRetryButton.hidden = !isError;
        setText(elements.contentHighlightStatus, message);
    }

    function homeText(value, maxLength, required = false) {
        const text = String(value ?? "").trim().replace(/\s+/g, " ");
        if ((required && !text) || text.length > maxLength) throw new Error("홈 응답 문자 정보가 올바르지 않습니다.");
        return text;
    }

    function homeInteger(value, fieldName, minimum = 0) {
        if (!Number.isSafeInteger(value) || value < minimum) throw new Error(`${fieldName} 정보가 올바르지 않습니다.`);
        return value;
    }

    function normalizeHighlightItems(value, boardType, popular = false) {
        if (!Array.isArray(value) || value.length > 4) throw new Error("콘텐츠 하이라이트 목록이 올바르지 않습니다.");
        const ids = new Set();
        return value.map((item) => {
            const id = homeInteger(item?.id, "콘텐츠 번호", 1);
            if (ids.has(id) || !["NOTICE", "STYLE"].includes(item?.boardType)
                || (!popular && item.boardType !== boardType) || typeof item.pinned !== "boolean") {
                throw new Error("콘텐츠 하이라이트 항목이 올바르지 않습니다.");
            }
            ids.add(id);
            const normalized = {
                id,
                boardType: item.boardType,
                title: homeText(item.title, 200, true),
                summary: homeText(item.summary, 500),
                pinned: item.pinned,
                createdDate: homeText(item.createdDate, 30, true)
            };
            if (popular) {
                normalized.recentViewCount = homeInteger(item.recentViewCount, "최근 조회 수");
                normalized.uniqueVisitors = homeInteger(item.uniqueVisitors, "방문자 수");
                if (normalized.uniqueVisitors > normalized.recentViewCount) throw new Error("콘텐츠 방문자 집계가 올바르지 않습니다.");
            } else {
                normalized.viewCount = homeInteger(item.viewCount, "조회 수");
            }
            return normalized;
        });
    }

    function normalizeContentHighlights(value) {
        if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("콘텐츠 하이라이트 응답이 올바르지 않습니다.");
        const popularStartDate = homeText(value.popularStartDate, 30, true);
        const popularEndDate = homeText(value.popularEndDate, 30, true);
        if (popularStartDate > popularEndDate) throw new Error("콘텐츠 집계 기간이 올바르지 않습니다.");
        return {
            notices: normalizeHighlightItems(value.notices, "NOTICE"),
            styles: normalizeHighlightItems(value.styles, "STYLE"),
            popular: normalizeHighlightItems(value.popular, null, true),
            popularStartDate,
            popularEndDate
        };
    }

    function initActionMenuBehavior() {
        const menuSelector = "details.board-action-menu, details.catalog-pagination__menu, details.saved-product-card__menu, details.rail-action-menu";
        const openMenuSelector = menuSelector.split(", ").map((selector) => `${selector}[open]`).join(", ");
        document.addEventListener("toggle", (event) => {
            const menu = event.target.closest?.(menuSelector);
            if (!menu) {
                return;
            }
            const summary = menu.querySelector(":scope > summary");
            summary?.setAttribute("aria-expanded", String(menu.open));
            if (!menu.open) {
                return;
            }
            document.querySelectorAll(menuSelector).forEach((otherMenu) => {
                if (otherMenu !== menu && otherMenu.open) {
                    otherMenu.open = false;
                    otherMenu.querySelector(":scope > summary")?.setAttribute("aria-expanded", "false");
                }
            });
        }, true);
        document.addEventListener("click", (event) => {
            const actionMenu = event.target.closest?.(menuSelector);
            if (!actionMenu) {
                document.querySelectorAll(openMenuSelector).forEach((menu) => menu.open = false);
                return;
            }
            const summary = actionMenu.querySelector(":scope > summary");
            if (!event.target.closest("button, a") || event.target.closest("summary") === summary) {
                return;
            }
            actionMenu.open = false;
            window.requestAnimationFrame(() => summary?.isConnected && summary.focus());
        });
        document.addEventListener("keydown", (event) => {
            if (event.key !== "Escape") {
                return;
            }
            const openMenus = Array.from(document.querySelectorAll(openMenuSelector));
            const menu = openMenus[openMenus.length - 1];
            if (!menu) {
                return;
            }
            menu.open = false;
            menu.querySelector(":scope > summary")?.focus();
        });
    }

    async function loadProducts(includeSummary = true) {
        const requestSequence = ++catalogRequestSequence;
        catalogRequestController?.abort();
        catalogRequestController = new AbortController();
        catalogLoadError = "";
        try {
            const endpoint = includeSummary ? "/api/front/catalog/bootstrap" : "/api/front/products";
            const response = await fetch(`${endpoint}?${new URLSearchParams({
                keyword: state.search,
                brand: state.brand,
                category: state.category,
                stock: state.stock,
                sort: state.sort,
                lowStockThreshold: state.lowStockThreshold,
                featuredOnly: state.featuredOnly === "FEATURED",
                priceBand: state.priceBand,
                page: Math.max(0, paginationState.page - 1),
                size: requestedCatalogPageSize()
            })}`, { signal: catalogRequestController.signal });
            if (!response.ok) {
                throw new Error("상품 데이터를 불러오지 못했습니다.");
            }
            const payload = normalizeCatalogPayload(await response.json(), includeSummary);
            if (requestSequence !== catalogRequestSequence) {
                return false;
            }
            products = payload.products.slice();
            catalogPagination = payload.pagination;
            paginationState.page = catalogPagination.page + 1;
            syncUrlState();
            if (!searchIndexProducts.length || !state.search) {
                searchIndexProducts = products.slice();
            }
            metrics = payload.metrics || metrics;
            brandFacets = payload.brandFacets || brandFacets;
            categoryFacets = payload.categoryFacets || categoryFacets;
            writeCatalogSessionCache({ ...payload, metrics, brandFacets, categoryFacets });
        } catch (error) {
            if (error?.name === "AbortError" || requestSequence !== catalogRequestSequence) {
                return false;
            }
            const cachedPayload = readCatalogSessionCache();
            if (cachedPayload) {
                products = cachedPayload.products.slice();
                catalogPagination = normalizeCatalogPagination(cachedPayload.pagination);
                paginationState.page = catalogPagination.page + 1;
                metrics = cachedPayload.metrics || metrics;
                brandFacets = cachedPayload.brandFacets.slice();
                categoryFacets = cachedPayload.categoryFacets.slice();
                announceStorefrontStatus("네트워크 오류로 최근 세션 카탈로그를 표시합니다.");
                return true;
            }
            catalogLoadError = error instanceof Error ? error.message : "상품 데이터를 불러오지 못했습니다.";
            products = [];
            catalogPagination = normalizeCatalogPagination(null);
            metrics = {
                totalCount: 0,
                lowStockCount: 0,
                latestCreatedDate: null,
                latestDropCount: 0,
                featuredCount: 0,
                totalStock: 0,
                averagePrice: 0,
                minimumPrice: 0,
                maximumPrice: 0,
                brandCount: 0,
                under200Count: 0,
                between200And300Count: 0,
                over300Count: 0
            };
            brandFacets = [];
            categoryFacets = [];
            setText(elements.catalogCountText, "상품 데이터를 불러오지 못했습니다.");
            if (elements.catalogGrid) {
                elements.catalogGrid.innerHTML = `
                    <div class="catalog-empty">
                        <strong>카탈로그를 불러오지 못했습니다.</strong>
                        <p>잠시 후 다시 시도해주세요.</p>
                    </div>
                `;
            }
        } finally {
            if (requestSequence === catalogRequestSequence) {
                catalogRequestController = null;
            }
        }
        if (elements.lowStockThresholdFilter) {
            elements.lowStockThresholdFilter.value = state.lowStockThreshold;
        }
        syncPresetButtons();
        return true;
    }

    async function loadHomeCollections() {
        try {
            const response = await fetch("/api/front/catalog/home-collections");
            if (!response.ok) {
                throw new Error("홈 컬렉션을 불러오지 못했습니다.");
            }
            homeCollections = normalizeHomeCollections(await response.json());
        } catch (error) {
            homeCollections = {
                recommended: products.filter((product) => product.featured).slice(0, 8),
                ranking: products.slice().sort((left, right) => right.stock - left.stock).slice(0, 8),
                fastDelivery: products.filter((product) => product.stock >= lowStockThresholdValue()).slice(0, 8),
                latestDrops: products.slice().sort((left, right) => right.createdDate.localeCompare(left.createdDate)).slice(0, 8),
                lowStock: products.filter((product) => product.stock < lowStockThresholdValue()).slice(0, 8)
            };
        }
    }

    function populateFilters() {
        fillSelect(elements.brandFilter, "전체 브랜드", brandFacets, uniqueValues("brand"));
        fillSelect(elements.categoryFilter, "전체 카테고리", categoryFacets, uniqueValues("category"));
        if (elements.brandFilter) {
            elements.brandFilter.value = state.brand;
        }
        if (elements.categoryFilter) {
            elements.categoryFilter.value = state.category;
        }
        if (elements.stockFilter) {
            elements.stockFilter.value = state.stock;
        }
        if (elements.featuredOnlyFilter) {
            elements.featuredOnlyFilter.value = state.featuredOnly;
        }
        if (elements.priceBandFilter) {
            elements.priceBandFilter.value = state.priceBand;
        }
        if (elements.sortFilter) {
            elements.sortFilter.value = state.sort;
        }
        syncFilterFieldStates();
    }

    function fillSelect(select, defaultLabel, facets, fallbackValues) {
        if (!select) {
            return;
        }
        const options = Array.isArray(facets) && facets.length
            ? facets.map((facet) => ({
                value: facet.value,
                label: `${facet.value} (${facet.count})`
            }))
            : fallbackValues.map((value) => ({
                value,
                label: value
            }));
        select.innerHTML = [`<option value="ALL">${escapeMarkup(defaultLabel)}</option>`]
            .concat(options.map((option) => `<option value="${escapeAttribute(option.value)}">${escapeMarkup(option.label)}</option>`))
            .join("");
    }

    function uniqueValues(key) {
        return Array.from(new Set(products.map((product) => product[key]))).sort((a, b) => a.localeCompare(b));
    }

    function bindEvents() {
        document.addEventListener("error", handleProductImageError, true);
        elements.contentHighlightRetryButton?.addEventListener("click", loadContentHighlights);
        elements.searchInput?.addEventListener("input", (event) => {
            state.search = event.target.value.trim().toLowerCase();
            renderSearchAssist(state.search);
            window.clearTimeout(searchDebounceTimer);
            searchDebounceTimer = window.setTimeout(() => {
                refreshCatalog();
            }, 300);
        });
        elements.searchInput?.addEventListener("focus", () => {
            renderSearchAssist(elements.searchInput.value.trim().toLowerCase());
        });
        elements.searchInput?.addEventListener("keydown", (event) => {
            if (event.key === "ArrowDown" || event.key === "ArrowUp") {
                event.preventDefault();
                moveSearchSuggestion(event.key === "ArrowDown" ? 1 : -1);
                return;
            }
            if (event.key === "Enter") {
                event.preventDefault();
                const suggestion = activeSearchSuggestions[activeSearchSuggestionIndex];
                applySearchSuggestion(suggestion || { query: elements.searchInput.value.trim().toLowerCase() });
                return;
            }
            if (event.key === "Escape") {
                event.preventDefault();
                event.stopPropagation();
                if (elements.searchInput.value) {
                    applySearchSuggestion({ query: "" });
                    elements.searchInput.focus();
                } else {
                    closeSearchAssist();
                    elements.searchInput.blur();
                }
            }
        });
        elements.clearInlineSearchButton?.addEventListener("click", () => {
            applySearchSuggestion({ query: "" });
            elements.searchInput?.focus();
        });
        elements.searchSuggestionList?.addEventListener("click", (event) => {
            const button = event.target.closest("[data-search-suggestion-index]");
            if (!button) {
                return;
            }
            applySearchSuggestion(activeSearchSuggestions[Number(button.dataset.searchSuggestionIndex)]);
        });
        document.addEventListener("click", (event) => {
            if (!event.target.closest(".toolbar-field--search")) {
                closeSearchAssist();
            }
            if (!event.target.closest(".header-search-panel")
                && !event.target.closest("#openDrawerFromTop")
                && !event.target.closest('[data-mobile-nav="SEARCH"]')) {
                closeHeaderSearch();
            }
            if (!event.target.closest("#topbarSubnav") && !event.target.closest("#mobileMenuButton")) {
                closeMobileMenu();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape") {
                closeHeaderSearch(true);
                closeMobileMenu(true);
                closeShortcutHelp();
                closeCatalogFilterPanel();
            }
            if (event.key === "Tab" && elements.shortcutHelpModal?.classList.contains("is-open")) {
                keepFocusInsideShortcutHelp(event);
            }
            const target = event.target;
            const tagName = target?.tagName;
            const isEditable = tagName === "INPUT" || tagName === "TEXTAREA" || tagName === "SELECT" || target?.isContentEditable;
            if (!isEditable && handleSectionKeyboardShortcut(event)) {
                return;
            }
            if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === "k") {
                event.preventDefault();
                elements.searchInput?.focus();
                elements.searchInput?.select?.();
                announceStorefrontStatus("상품 검색으로 이동했습니다.");
                return;
            }
            if (event.key === "?" && !isEditable) {
                event.preventDefault();
                openShortcutHelp();
                return;
            }
            if (event.key.toLowerCase() === "f" && !isEditable) {
                event.preventDefault();
                openCatalogFilterPanel();
                return;
            }
            if (event.key !== "/" || document.activeElement === elements.searchInput) {
                return;
            }
            if (isEditable) {
                return;
            }
            event.preventDefault();
            elements.searchInput?.focus();
            elements.searchInput?.select?.();
        });
        elements.brandFilter?.addEventListener("change", (event) => {
            state.brand = event.target.value;
            refreshCatalog();
        });
        elements.categoryFilter?.addEventListener("change", (event) => {
            state.category = event.target.value;
            refreshCatalog();
        });
        elements.stockFilter?.addEventListener("change", (event) => {
            state.stock = event.target.value;
            refreshCatalog();
        });
        elements.featuredOnlyFilter?.addEventListener("change", (event) => {
            state.featuredOnly = event.target.value;
            if (state.featuredOnly === "FEATURED" && state.sort !== "FEATURED") {
                state.sort = "FEATURED";
                if (elements.sortFilter) {
                    elements.sortFilter.value = "FEATURED";
                }
            }
            refreshCatalog();
        });
        elements.priceBandFilter?.addEventListener("change", (event) => {
            state.priceBand = event.target.value;
            refreshCatalog();
        });
        elements.sortFilter?.addEventListener("change", (event) => {
            state.sort = event.target.value;
            refreshCatalog();
        });
        elements.lowStockThresholdFilter?.addEventListener("change", (event) => {
            state.lowStockThreshold = event.target.value;
            refreshCatalog();
        });
        elements.closeDrawerButton?.addEventListener("click", closeDrawer);
        elements.productDrawer?.addEventListener("click", (event) => {
            if (event.target === elements.productDrawer) {
                closeDrawer();
            }
        });
        document.addEventListener("keydown", (event) => {
            if (event.key === "Escape" && elements.productDrawer?.classList.contains("is-open")) {
                closeDrawer();
            }
            if (event.key === "Tab" && elements.productDrawer?.classList.contains("is-open")) {
                keepFocusInsideDrawer(event);
            }
            if (handleDrawerKeyboardShortcut(event)) {
                return;
            }
            if (event.altKey && event.key === "ArrowLeft") {
                event.preventDefault();
                moveCatalogPage(paginationState.page - 1);
            }
            if (event.altKey && event.key === "ArrowRight") {
                event.preventDefault();
                moveCatalogPage(paginationState.page + 1);
            }
        });
        elements.focusLowStockButton?.addEventListener("click", () => {
            state.stock = "LOW";
            if (elements.stockFilter) {
                elements.stockFilter.value = "LOW";
            }
            refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.openDrawerFromTop?.addEventListener("click", openHeaderSearch);
        elements.closeHeaderSearchButton?.addEventListener("click", () => closeHeaderSearch(true));
        elements.headerSearchPanel?.querySelector("form")?.addEventListener("submit", (event) => {
            event.preventDefault();
            void applyHeaderSearch();
        });
        elements.headerSearchInput?.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                applyHeaderSearch();
            }
            if (event.key === "Escape") {
                closeHeaderSearch(true);
            }
        });
        elements.headerSearchPanel?.addEventListener("click", async (event) => {
            const presetButton = event.target.closest("[data-header-preset]");
            if (!presetButton) {
                return;
            }
            await applyHomePreset(presetButton.dataset.headerPreset);
            closeHeaderSearch();
        });
        elements.mobileMenuButton?.addEventListener("click", toggleMobileMenu);
        elements.topbarSubnav?.addEventListener("click", () => closeMobileMenu());
        elements.topbarSubnav?.addEventListener("keydown", handleMobileMenuKeyboard);
        window.addEventListener("resize", () => {
            if (window.innerWidth > 768) {
                closeMobileMenu();
            }
        });
        elements.homeCategoryRail?.addEventListener("click", async (event) => {
            const button = event.target.closest("button");
            if (!button) {
                return;
            }
            if (button.dataset.homePreset) {
                await applyHomePreset(button.dataset.homePreset);
                return;
            }
            const target = document.getElementById(button.dataset.homeTarget);
            if (target) {
                target.scrollIntoView({ behavior: "smooth", block: "start" });
            }
        });
        elements.mobileStoreNav?.addEventListener("click", handleMobileStoreNavigation);
        elements.heroPreviousButton?.addEventListener("click", () => moveHeroSlide(-1, true));
        elements.heroNextButton?.addEventListener("click", () => moveHeroSlide(1, true));
        elements.heroPauseButton?.addEventListener("click", () => {
            heroCarouselPaused = !heroCarouselPaused;
            syncHeroPlaybackControl();
            heroCarouselPaused ? stopHeroCarousel() : startHeroCarousel();
        });
        elements.heroDots?.addEventListener("click", (event) => {
            const dot = event.target.closest("[data-hero-slide]");
            if (!dot) {
                return;
            }
            activeHeroSlide = Number(dot.dataset.heroSlide);
            renderHeroSlide();
            restartHeroCarousel();
        });
        elements.catalogTags?.addEventListener("click", (event) => {
            const actionButton = event.target.closest("[data-filter-remove]");
            if (!actionButton) {
                return;
            }
            removeFilter(actionButton.dataset.filterRemove);
        });
        elements.resetFiltersButton?.addEventListener("click", async () => {
            resetState();
            syncControls();
            await refreshCatalog();
        });
        elements.catalogPresetStrip?.addEventListener("click", async (event) => {
            const presetButton = event.target.closest("[data-preset]");
            if (!presetButton) {
                return;
            }
            applyPreset(presetButton.dataset.preset);
            syncControls();
            await refreshCatalog();
        });
        elements.shareCatalogButton?.addEventListener("click", async () => {
            const shareUrl = `${window.location.origin}${window.location.pathname}${window.location.search}`;
            try {
                if (navigator.share) {
                    await navigator.share({ title: "NOREN 상품 탐색", text: buildSummaryText(filteredProducts().length), url: shareUrl });
                    showToast("탐색 조건을 공유했습니다.", "현재 상품 조건과 URL을 전달했습니다.");
                    return;
                }
                if (navigator.clipboard?.writeText) {
                    await navigator.clipboard.writeText(shareUrl);
                }
                showToast("탐색 URL을 복사했습니다.", "현재 조건을 그대로 공유할 수 있습니다.");
            } catch (error) {
                if (error?.name === "AbortError") {
                    return;
                }
                window.prompt("현재 조건 URL을 복사하세요.", shareUrl);
            }
        });
        elements.copyCatalogSummaryButton?.addEventListener("click", async () => {
            const text = catalogSummaryClipboardText(filteredProducts());
            await copyTextWithFeedback(text, "탐색 요약을 복사했습니다.", "현재 보이는 상품 상태를 문서나 메신저로 옮길 수 있습니다.");
        });
        elements.openCheapestCatalogButton?.addEventListener("click", () => openCatalogDecisionProduct("PRICE_LOW"));
        elements.openHighestStockCatalogButton?.addEventListener("click", () => openCatalogDecisionProduct("STOCK_HIGH"));
        elements.openUrgentCatalogButton?.addEventListener("click", () => openCatalogDecisionProduct("STOCK_LOW"));
        elements.focusDominantCatalogBrandButton?.addEventListener("click", async () => {
            const brand = dominantBrand(filteredProducts());
            if (!brand) {
                return;
            }
            state.brand = brand;
            syncControls();
            await refreshCatalog();
            showToast("대표 브랜드를 적용했습니다.", `${brand} 상품만 집중해서 확인합니다.`);
        });
        elements.copyCatalogDecisionButton?.addEventListener("click", async () => {
            await copyTextWithFeedback(catalogDecisionSummary(filteredProducts()), "상품 판단 요약을 복사했습니다.", "가격과 재고 우선순위를 함께 전달할 수 있습니다.");
        });
        elements.copyFlowBoardSummaryButton?.addEventListener("click", async () => {
            await copyTextWithFeedback(buildFlowBoardSummaryText(), "개인 보드 요약을 복사했습니다.", "최근 흐름과 저장 상태를 한 번에 전달할 수 있습니다.");
        });
        elements.openRecentFlowButton?.addEventListener("click", () => {
            handleFlowAction("RECENT");
        });
        elements.openCompareFlowButton?.addEventListener("click", () => {
            handleFlowAction("COMPARE");
        });
        elements.openBookmarkFlowButton?.addEventListener("click", () => {
            handleFlowAction("BOOKMARK");
        });
        elements.applyFlowLowStockButton?.addEventListener("click", async () => {
            await handleFlowAction("LOW_STOCK");
        });
        elements.restoreHiddenFlowButton?.addEventListener("click", async () => {
            await handleFlowAction("RESTORE_HIDDEN");
        });
        elements.sortLatestPriceButton?.addEventListener("click", () => {
            boardState.latestSort = boardState.latestSort === "PRICE_LOW" ? "DEFAULT" : "PRICE_LOW";
            renderLatestDrops();
            syncCurationButtons();
            showToast("신규 드롭 정렬을 변경했습니다.", boardState.latestSort === "PRICE_LOW" ? "가격이 낮은 상품부터 표시합니다." : "최신 등록 흐름으로 복구했습니다.");
        });
        elements.bookmarkLatestButton?.addEventListener("click", () => {
            addProductsToBoard(latestDropProducts(), "BOOKMARK");
        });
        elements.copyLatestSummaryButton?.addEventListener("click", async () => {
            await copyProductCollection(latestDropProducts(), "신규 드롭", "신규 드롭 요약을 복사했습니다.");
        });
        elements.sortLowStockPriceButton?.addEventListener("click", () => {
            boardState.lowStockSort = boardState.lowStockSort === "PRICE_LOW" ? "STOCK_ASC" : "PRICE_LOW";
            renderLowStockHighlights();
            syncCurationButtons();
            showToast("저재고 정렬을 변경했습니다.", boardState.lowStockSort === "PRICE_LOW" ? "가격이 낮은 상품부터 표시합니다." : "재고가 적은 상품부터 표시합니다.");
        });
        elements.compareLowStockButton?.addEventListener("click", () => {
            addProductsToBoard(lowStockHighlightProducts(), "COMPARE");
        });
        elements.copyLowStockSummaryButton?.addEventListener("click", async () => {
            await copyProductCollection(lowStockHighlightProducts(), "저재고 하이라이트", "저재고 요약을 복사했습니다.");
        });
        elements.sortFeaturedPriceButton?.addEventListener("click", () => {
            boardState.featuredSort = "PRICE_LOW";
            renderFeatured();
            syncCurationButtons();
            showToast("Featured를 가격순으로 정렬했습니다.", "가격이 낮은 큐레이션부터 확인할 수 있습니다.");
        });
        elements.sortFeaturedStockButton?.addEventListener("click", () => {
            boardState.featuredSort = "STOCK_ASC";
            renderFeatured();
            syncCurationButtons();
            showToast("Featured를 재고순으로 정렬했습니다.", "구매 판단이 급한 큐레이션부터 확인할 수 있습니다.");
        });
        elements.bookmarkFeaturedButton?.addEventListener("click", () => {
            addProductsToBoard(featuredProducts(), "BOOKMARK");
        });
        elements.copyFeaturedLinksButton?.addEventListener("click", async () => {
            const links = featuredProducts().map((product) => `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`);
            await copyTextWithFeedback(links.join("\n") || "Featured 상품이 없습니다.", "Featured 링크를 복사했습니다.", "큐레이션 상세 페이지를 한 번에 공유할 수 있습니다.");
        });
        elements.clearSearchButton?.addEventListener("click", async () => {
            state.search = "";
            syncControls();
            await refreshCatalog();
            showToast("검색어를 비웠습니다.", "전체 조건 흐름으로 다시 탐색할 수 있습니다.");
        });
        elements.saveCurrentViewButton?.addEventListener("click", () => {
            saveCurrentView();
        });
        elements.reverseSavedViewsButton?.addEventListener("click", () => {
            memoryState.savedReversed = !memoryState.savedReversed;
            renderSavedViews();
            syncMemoryButtons();
            showToast("저장 탐색 순서를 변경했습니다.", memoryState.savedReversed ? "오래된 조건부터 표시합니다." : "최근 저장한 조건부터 표시합니다.");
        });
        elements.copySavedViewsButton?.addEventListener("click", async () => {
            const savedViews = readSavedViews();
            const text = savedViews.length ? savedViews.map((item, index) => `${index + 1}. ${item.summary}`).join("\n") : "저장된 탐색이 없습니다.";
            await copyTextWithFeedback(text, "저장 탐색을 복사했습니다.", "보관한 필터 조건을 한 번에 전달할 수 있습니다.");
        });
        elements.restoreLastStateButton?.addEventListener("click", async () => {
            if (!restoreLastCatalogState()) {
                showToast("복구할 마지막 조건이 없습니다.", "먼저 탐색 조건을 적용한 뒤 다시 시도해주세요.", true);
                return;
            }
            syncControls();
            await refreshCatalog();
            showToast("마지막 탐색 조건을 복구했습니다.", "직전에 보던 카탈로그 흐름으로 돌아왔습니다.");
        });
        elements.applyLatestSavedViewButton?.addEventListener("click", () => applySavedViewEdge("LATEST"));
        elements.applyOldestSavedViewButton?.addEventListener("click", () => applySavedViewEdge("OLDEST"));
        elements.removeLatestSavedViewButton?.addEventListener("click", () => removeSavedViewEdge("LATEST"));
        elements.removeOldestSavedViewButton?.addEventListener("click", () => removeSavedViewEdge("OLDEST"));
        elements.exportSavedViewsButton?.addEventListener("click", () => {
            const savedViews = readSavedViews();
            downloadTextFile("grade-stock-saved-views.json", JSON.stringify(savedViews, null, 2), "application/json;charset=utf-8");
            showToast("저장 탐색 백업을 생성했습니다.", `${savedViews.length}개 탐색 조건을 JSON으로 저장했습니다.`);
        });
        elements.clearSearchHistoryButton?.addEventListener("click", () => {
            window.localStorage.removeItem(SEARCH_HISTORY_KEY);
            renderSearchHistory();
            showToast("검색 기록을 비웠습니다.", "최근 검색어 목록이 초기화되었습니다.");
        });
        elements.sortSearchHistoryButton?.addEventListener("click", () => {
            memoryState.searchAlphabetical = !memoryState.searchAlphabetical;
            renderSearchHistory();
            syncMemoryButtons();
            showToast("검색 기록 순서를 변경했습니다.", memoryState.searchAlphabetical ? "가나다순으로 표시합니다." : "최근 검색순으로 복구했습니다.");
        });
        elements.copySearchHistoryButton?.addEventListener("click", async () => {
            const history = readSearchHistory();
            await copyTextWithFeedback(history.join("\n") || "최근 검색이 없습니다.", "검색 기록을 복사했습니다.", "최근 탐색 키워드를 한 번에 공유할 수 있습니다.");
        });
        elements.applyLatestSearchButton?.addEventListener("click", () => applySearchHistoryEdge("LATEST"));
        elements.applyOldestSearchButton?.addEventListener("click", () => applySearchHistoryEdge("OLDEST"));
        elements.removeLatestSearchButton?.addEventListener("click", () => removeSearchHistoryEdge("LATEST"));
        elements.removeOldestSearchButton?.addEventListener("click", () => removeSearchHistoryEdge("OLDEST"));
        elements.exportSearchHistoryButton?.addEventListener("click", () => {
            const history = readSearchHistory();
            downloadTextFile("grade-stock-search-history.txt", history.join("\n"), "text/plain;charset=utf-8");
            showToast("검색 기록 파일을 생성했습니다.", `${history.length}개 검색어를 저장했습니다.`);
        });
        elements.reopenLastDrawerButton?.addEventListener("click", () => {
            const lastDrawerProductId = Number(window.localStorage.getItem(LAST_DRAWER_PRODUCT_KEY) || 0);
            if (!lastDrawerProductId) {
                showToast("마지막으로 본 상품이 없습니다.", "먼저 상품 상세를 열어주세요.", true);
                return;
            }
            openDrawer(lastDrawerProductId);
            showToast("마지막으로 보던 상품을 열었습니다.", "이전 탐색 흐름을 바로 이어갈 수 있습니다.");
        });
        elements.clearHiddenProductsButton?.addEventListener("click", async () => {
            window.localStorage.removeItem(HIDDEN_PRODUCTS_KEY);
            renderHiddenProducts();
            await refreshCatalog();
            showToast("숨김 상품을 모두 복구했습니다.", "다시 전체 카탈로그에서 노출됩니다.");
        });
        elements.toggleHiddenViewButton?.addEventListener("click", async () => {
            uiState.showHiddenProducts = !uiState.showHiddenProducts;
            await refreshCatalog();
            showToast(
                uiState.showHiddenProducts ? "숨긴 상품 보기 모드를 켰습니다." : "숨긴 상품 보기 모드를 껐습니다.",
                uiState.showHiddenProducts ? "숨김 처리한 상품도 목록에 다시 표시됩니다." : "숨긴 상품은 목록에서 제외됩니다."
            );
        });
        elements.sortHiddenProductsButton?.addEventListener("click", () => {
            memoryState.hiddenAlphabetical = !memoryState.hiddenAlphabetical;
            renderHiddenProducts();
            syncMemoryButtons();
            showToast("숨김 상품 순서를 변경했습니다.", memoryState.hiddenAlphabetical ? "상품 이름순으로 표시합니다." : "최근 숨김순으로 복구했습니다.");
        });
        elements.copyHiddenProductsButton?.addEventListener("click", async () => {
            const hiddenProducts = readHiddenProducts();
            const text = hiddenProducts.map((product, index) => `${index + 1}. ${product.headline || product.name}`).join("\n") || "숨긴 상품이 없습니다.";
            await copyTextWithFeedback(text, "숨김 목록을 복사했습니다.", "정리한 상품 목록을 한 번에 전달할 수 있습니다.");
        });
        elements.restoreLatestHiddenButton?.addEventListener("click", async () => {
            const hiddenProducts = readHiddenProducts();
            if (!hiddenProducts.length) {
                showToast("복구할 숨김 상품이 없습니다.", "상품을 숨긴 뒤 다시 이용해주세요.", true);
                return;
            }
            window.localStorage.setItem(HIDDEN_PRODUCTS_KEY, JSON.stringify(hiddenProducts.slice(1)));
            renderHiddenProducts();
            renderFlowBoard();
            await refreshCatalog();
            showToast("최근 숨김 상품을 복구했습니다.", `${hiddenProducts[0].headline || hiddenProducts[0].name}이 다시 표시됩니다.`);
        });
        elements.bookmarkHiddenProductsButton?.addEventListener("click", () => {
            const hiddenProducts = readHiddenProducts().map((hidden) =>
                products.find((product) => Number(product.id) === Number(hidden.id)) || hidden
            );
            addProductsToBoard(hiddenProducts, "BOOKMARK");
        });
        elements.jumpFirstProductButton?.addEventListener("click", () => {
            const firstProduct = filteredProducts()[0];
            if (!firstProduct) {
                showToast("이동할 상품이 없습니다.", "먼저 조건을 조정해 상품을 불러와주세요.", true);
                return;
            }
            openDrawer(firstProduct.id);
            showToast("첫 상품을 열었습니다.", `${firstProduct.headline || firstProduct.name} 상세를 빠르게 확인할 수 있습니다.`);
        });
        elements.randomProductButton?.addEventListener("click", () => {
            const list = filteredProducts();
            if (!list.length) {
                showToast("랜덤으로 열 상품이 없습니다.", "먼저 조건을 조정해 상품을 불러와주세요.", true);
                return;
            }
            const product = list[Math.floor(Math.random() * list.length)];
            openDrawer(product.id);
            showToast("랜덤 상품을 열었습니다.", `${product.headline || product.name}을 새로운 시선으로 확인해보세요.`);
        });
        elements.toggleCompactViewButton?.addEventListener("click", () => {
            setCatalogLayout(uiState.layout === "COMFORT" ? "STANDARD" : "COMFORT");
            showToast(uiState.layout === "COMFORT" ? "2열 여유 보기를 켰습니다." : "3열 표준 보기를 복구했습니다.", "카탈로그 카드 밀도를 바로 전환할 수 있습니다.");
        });
        elements.catalogLayoutShopButton?.addEventListener("click", () => setCatalogLayout("SHOP"));
        elements.catalogLayoutStandardButton?.addEventListener("click", () => setCatalogLayout("STANDARD"));
        elements.catalogLayoutComfortButton?.addEventListener("click", () => setCatalogLayout("COMFORT"));
        elements.catalogLayoutListButton?.addEventListener("click", () => setCatalogLayout("LIST"));
        elements.toggleCatalogDescriptionButton?.addEventListener("click", () => toggleCatalogPreference("hideDescriptions", "상품 설명 표시를 변경했습니다."));
        elements.toggleCatalogSignalsButton?.addEventListener("click", () => toggleCatalogPreference("hideSignals", "상품 시그널 표시를 변경했습니다."));
        elements.toggleCatalogActionsButton?.addEventListener("click", () => toggleCatalogPreference("hideActions", "빠른 액션 표시를 변경했습니다."));
        elements.toggleCatalogMetricsButton?.addEventListener("click", () => toggleCatalogPreference("hideMetrics", "실시간 지표 표시를 변경했습니다."));
        elements.toggleCatalogDistributionButton?.addEventListener("click", () => toggleCatalogPreference("hideDistribution", "분포 그래프 표시를 변경했습니다."));
        elements.toggleCatalogDecisionButton?.addEventListener("click", () => toggleCatalogPreference("hideDecision", "판단 바로가기 표시를 변경했습니다."));
        elements.compactCatalogAnalyticsButton?.addEventListener("click", () => {
            Object.assign(uiState, { hideMetrics: true, hideDistribution: true, hideDecision: true });
            persistDisplayPreferences();
            renderCatalog();
            syncViewButtons();
            showToast("분석 영역을 간소화했습니다.", "상품 목록과 핵심 결과에 집중할 수 있습니다.");
        });
        elements.resetCatalogAnalyticsButton?.addEventListener("click", () => {
            Object.assign(uiState, { hideMetrics: false, hideDistribution: false, hideDecision: false });
            persistDisplayPreferences();
            renderCatalog();
            syncViewButtons();
            showToast("분석 영역을 복구했습니다.", "지표와 분포, 판단 바로가기를 모두 표시합니다.");
        });
        elements.toggleReducedMotionButton?.addEventListener("click", () => toggleCatalogPreference("reducedMotion", "모션 설정을 변경했습니다."));
        elements.resetCatalogDisplayButton?.addEventListener("click", () => {
            Object.assign(uiState, {
                layout: "STANDARD",
                hideDescriptions: false,
                hideSignals: false,
                hideActions: false,
                hideMetrics: false,
                hideDistribution: false,
                hideDecision: false,
                reducedMotion: false,
                viewMode: "DEFAULT"
            });
            persistDisplayPreferences();
            renderCatalog();
            syncViewButtons();
            showToast("화면 설정을 초기화했습니다.", "3열 표준 보기와 전체 정보 표시로 복구했습니다.");
        });
        elements.toggleTodayOnlyButton?.addEventListener("click", async () => {
            uiState.todayOnly = !uiState.todayOnly;
            await refreshCatalog();
            showToast(uiState.todayOnly ? "오늘 등록만 보기 필터를 켰습니다." : "오늘 등록만 보기 필터를 해제했습니다.", "신규 드롭 기준으로 빠르게 다시 탐색할 수 있습니다.");
        });
        elements.clearRecentViewedButton?.addEventListener("click", () => {
            window.localStorage.removeItem(RECENT_VIEWED_KEY);
            renderRecentViewed();
            renderFlowBoard();
            showToast("최근 본 상품을 비웠습니다.", "메인 최근 흐름 보드가 초기화되었습니다.");
        });
        elements.copyRecentViewedSummaryButton?.addEventListener("click", async () => {
            const recentProducts = readRecentProducts().slice(0, 3);
            const text = recentProducts.length
                ? recentProducts.map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.model || "-"} · ${product.priceLabel || formatPrice(product.price)}`).join("\n")
                : "최근 본 상품이 없습니다.";
            await copyTextWithFeedback(text, "최근 흐름을 복사했습니다.", "방금 본 상품 목록을 바로 전달할 수 있습니다.");
        });
        elements.focusRecentLowStockButton?.addEventListener("click", async () => {
            const recentLowStock = readRecentProducts().find((product) => Number(product.stock || 0) < lowStockThresholdValue());
            if (!recentLowStock) {
                showToast("최근 본 상품 중 긴장 재고가 없습니다.", "현재 최근 흐름은 안정 재고 위주입니다.", true);
                return;
            }
            state.stock = "LOW";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("최근 흐름 기준 저재고 필터를 적용했습니다.", `${recentLowStock.headline || recentLowStock.name}과 비슷한 조건으로 다시 볼 수 있습니다.`);
        });
        elements.sortRecentPriceButton?.addEventListener("click", () => {
            boardState.recentSort = "PRICE_LOW";
            renderRecentViewed();
            showToast("최근 본 상품을 가격순으로 정렬했습니다.", "부담이 낮은 상품부터 다시 확인할 수 있습니다.");
        });
        elements.sortRecentStockButton?.addEventListener("click", () => {
            boardState.recentSort = "STOCK_ASC";
            renderRecentViewed();
            showToast("최근 본 상품을 재고순으로 정렬했습니다.", "구매 판단이 급한 상품부터 확인할 수 있습니다.");
        });
        elements.sortRecentNameButton?.addEventListener("click", () => {
            boardState.recentSort = "NAME_ASC";
            renderRecentViewed();
            showToast("최근 본 상품을 이름순으로 정렬했습니다.", "최근 상품을 이름 기준으로 빠르게 찾을 수 있습니다.");
        });
        elements.filterRecentLowStockButton?.addEventListener("click", () => {
            boardState.recentFilter = "LOW_STOCK";
            renderRecentViewed();
            showToast("최근 보드에서 저재고만 표시합니다.", "다시 확인이 급한 상품만 남겼습니다.");
        });
        elements.filterRecentBrandButton?.addEventListener("click", () => {
            boardState.recentFilter = "DOMINANT_BRAND";
            renderRecentViewed();
            showToast("최근 보드에서 주요 브랜드만 표시합니다.", "가장 많이 확인한 브랜드 흐름만 남겼습니다.");
        });
        elements.filterRecentAvailableButton?.addEventListener("click", () => {
            boardState.recentFilter = "AVAILABLE";
            renderRecentViewed();
            showToast("최근 보드에서 구매 가능 상품만 표시합니다.", "현재 재고가 있는 상품 흐름만 남겼습니다.");
        });
        elements.filterRecentSoldOutButton?.addEventListener("click", () => {
            boardState.recentFilter = "SOLD_OUT";
            renderRecentViewed();
            showToast("최근 보드에서 품절 상품만 표시합니다.", "대체 상품 탐색이 필요한 항목을 모았습니다.");
        });
        elements.sortRecentOldestButton?.addEventListener("click", () => {
            boardState.recentSort = "OLDEST";
            renderRecentViewed();
            showToast("오래 확인한 상품부터 정렬했습니다.", "놓치기 쉬운 이전 탐색 항목을 먼저 표시합니다.");
        });
        elements.toggleRecentExpandedButton?.addEventListener("click", () => {
            boardState.recentExpanded = !boardState.recentExpanded;
            renderRecentViewed();
            showToast(boardState.recentExpanded ? "최근 보드를 모두 펼쳤습니다." : "최근 보드를 3개로 줄였습니다.", "현재 보드 안에서 표시 개수만 변경합니다.");
        });
        elements.copyRecentViewedLinksButton?.addEventListener("click", async () => {
            const links = readRecentProducts().map((product) => `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`);
            await copyTextWithFeedback(links.join("\n") || "최근 본 상품이 없습니다.", "최근 상품 링크를 복사했습니다.", `${links.length}개 상세 링크를 전달할 수 있습니다.`);
        });
        elements.resetRecentBoardFilterButton?.addEventListener("click", () => {
            boardState.recentFilter = "ALL";
            renderRecentViewed();
            showToast("최근 보드 필터를 해제했습니다.", "최근 본 상품을 다시 모두 표시합니다.");
        });
        elements.openRecommendedRecentButton?.addEventListener("click", () => {
            const recommended = recommendedRecentProduct(sortedRecentProducts(readRecentProducts()));
            if (!recommended) {
                showToast("추천할 최근 상품이 없습니다.", "현재 보드 필터를 해제하거나 상세 상품을 확인해주세요.", true);
                return;
            }
            openDrawer(recommended.id);
            showToast("다시 볼 상품을 열었습니다.", `${recommended.headline || recommended.name}을 우선 확인합니다.`);
        });
        elements.addRecentToCompareButton?.addEventListener("click", () => {
            addProductsToBoard(readRecentProducts(), "COMPARE");
        });
        elements.addRecentToBookmarkButton?.addEventListener("click", () => {
            addProductsToBoard(readRecentProducts(), "BOOKMARK");
        });
        elements.clearCompareButton?.addEventListener("click", () => {
            writeCompareProducts([]);
            renderCompareBoard();
            renderFlowBoard();
            renderCatalog();
            showToast("비교 보드를 초기화했습니다.", "비교 대상 상품을 모두 비웠습니다.");
        });
        elements.applyCompareCategoryButton?.addEventListener("click", async () => {
            const category = dominantCategory(readCompareProducts());
            if (!category) {
                return;
            }
            state.category = category;
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.applyCompareLowStockButton?.addEventListener("click", async () => {
            state.stock = "LOW";
            state.sort = "STOCK_ASC";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("비교 기준 필터를 적용했습니다.", "재고가 낮은 순서로 바로 카탈로그를 확인할 수 있습니다.");
        });
        elements.sortComparePriceButton?.addEventListener("click", () => {
            boardState.compareSort = "PRICE_HIGH";
            renderCompareBoard();
            showToast("비교 보드를 가격순으로 정렬했습니다.", "고가 상품부터 바로 차이를 확인할 수 있습니다.");
        });
        elements.sortCompareStockButton?.addEventListener("click", () => {
            boardState.compareSort = "STOCK_ASC";
            renderCompareBoard();
            showToast("비교 보드를 재고순으로 정렬했습니다.", "재고가 낮은 상품부터 우선 확인할 수 있습니다.");
        });
        elements.copyCompareSummaryButton?.addEventListener("click", async () => {
            const comparedProducts = readCompareProducts();
            const text = comparedProducts.length
                ? `비교 보드\n${buildCompareSummary(comparedProducts)}\n${comparedProducts.map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · 재고 ${product.stock}개`).join("\n")}`
                : "비교 보드에 담긴 상품이 없습니다.";
            await copyTextWithFeedback(text, "비교 요약을 복사했습니다.", "가격과 재고 차이를 바로 전달할 수 있습니다.");
        });
        elements.addCompareToBookmarkButton?.addEventListener("click", () => {
            addProductsToBoard(readCompareProducts(), "BOOKMARK");
        });
        elements.openCheapestCompareButton?.addEventListener("click", () => {
            const cheapest = readCompareProducts().slice().sort((left, right) => Number(left.price || 0) - Number(right.price || 0))[0];
            if (!cheapest) {
                showToast("빠르게 볼 비교 상품이 없습니다.", "비교 보드에 상품을 먼저 담아주세요.", true);
                return;
            }
            openDrawer(cheapest.id);
            showToast("최저가 비교 상품을 열었습니다.", `${cheapest.headline || cheapest.name}을 바로 확인합니다.`);
        });
        elements.openHighestStockCompareButton?.addEventListener("click", () => openCompareProductByMetric("STOCK_HIGH"));
        elements.openLowestStockCompareButton?.addEventListener("click", () => openCompareProductByMetric("STOCK_LOW"));
        elements.openHighestPriceCompareButton?.addEventListener("click", () => openCompareProductByMetric("PRICE_HIGH"));
        elements.sortCompareOldestButton?.addEventListener("click", () => {
            boardState.compareSort = "OLDEST";
            renderCompareBoard();
            showToast("오래 담은 비교 상품부터 정렬했습니다.", "초기 비교 후보를 먼저 다시 확인할 수 있습니다.");
        });
        elements.exportCompareCsvButton?.addEventListener("click", () => {
            exportProductsCsv(sortedCompareProducts(readCompareProducts()), "grade-stock-compare-products.csv");
        });
        elements.sortComparePriceLowButton?.addEventListener("click", () => {
            boardState.compareSort = "PRICE_LOW";
            renderCompareBoard();
            showToast("비교 보드를 낮은 가격순으로 정렬했습니다.", "가격 부담이 낮은 후보부터 확인할 수 있습니다.");
        });
        elements.sortCompareNameButton?.addEventListener("click", () => {
            boardState.compareSort = "NAME_ASC";
            renderCompareBoard();
            showToast("비교 보드를 상품명순으로 정렬했습니다.", "상품을 이름 기준으로 빠르게 찾을 수 있습니다.");
        });
        elements.copyCompareLinksButton?.addEventListener("click", async () => {
            const links = readCompareProducts().map((product) => `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`);
            await copyTextWithFeedback(links.join("\n") || "비교 상품이 없습니다.", "비교 상품 링크를 복사했습니다.", "비교 중인 상세 페이지를 한 번에 공유할 수 있습니다.");
        });
        elements.openRecommendedCompareButton?.addEventListener("click", () => {
            const recommended = recommendedCompareProduct(readCompareProducts());
            if (!recommended) {
                showToast("추천할 비교 상품이 없습니다.", "비교 보드에 상품을 먼저 담아주세요.", true);
                return;
            }
            openDrawer(recommended.id);
            showToast("균형 추천 상품을 열었습니다.", `${recommended.headline || recommended.name}은 가격과 재고 균형이 가장 좋습니다.`);
        });
        elements.clearBookmarkButton?.addEventListener("click", () => {
            writeBookmarkProducts([]);
            renderBookmarkBoard();
            renderFlowBoard();
            renderCatalog();
            showToast("관심 상품을 초기화했습니다.", "찜 보드에 담긴 상품을 모두 비웠습니다.");
        });
        elements.applyBookmarkFeaturedButton?.addEventListener("click", async () => {
            state.featuredOnly = "FEATURED";
            state.sort = "FEATURED";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
        });
        elements.applyBookmarkLowStockButton?.addEventListener("click", async () => {
            state.stock = "LOW";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("관심 보드 기준 필터를 적용했습니다.", "긴장 재고 상품만 빠르게 다시 볼 수 있습니다.");
        });
        elements.sortBookmarkRecentButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "RECENT";
            renderBookmarkBoard();
            showToast("관심 보드를 최근 담은 순으로 정렬했습니다.", "가장 최근에 저장한 흐름부터 이어서 볼 수 있습니다.");
        });
        elements.sortBookmarkFeaturedButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "FEATURED";
            renderBookmarkBoard();
            showToast("관심 보드를 Featured 우선으로 정렬했습니다.", "대표 노출 상품을 먼저 확인할 수 있습니다.");
        });
        elements.sortBookmarkPriceButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "PRICE_LOW";
            renderBookmarkBoard();
            showToast("관심 상품을 가격순으로 정렬했습니다.", "낮은 가격부터 구매 후보를 비교할 수 있습니다.");
        });
        elements.sortBookmarkStockButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "STOCK_ASC";
            renderBookmarkBoard();
            showToast("관심 상품을 재고순으로 정렬했습니다.", "재고가 적은 후보를 먼저 확인할 수 있습니다.");
        });
        elements.addBookmarkToCompareButton?.addEventListener("click", () => {
            addProductsToBoard(readBookmarkProducts(), "COMPARE");
        });
        elements.copyBookmarkLinksButton?.addEventListener("click", async () => {
            const links = readBookmarkProducts().map((product) => `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`);
            await copyTextWithFeedback(links.join("\n") || "관심 상품이 없습니다.", "관심 상품 링크를 복사했습니다.", "저장한 상세 페이지를 한 번에 공유할 수 있습니다.");
        });
        elements.copyBookmarkSummaryButton?.addEventListener("click", async () => {
            const bookmarkedProducts = readBookmarkProducts();
            const text = bookmarkedProducts.length
                ? `관심 상품 보드\n${buildBookmarkSummary(bookmarkedProducts)}\n${bookmarkedProducts.map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · ${product.featured ? "Featured" : "Watchlist"}`).join("\n")}`
                : "관심 상품 보드에 담긴 상품이 없습니다.";
            await copyTextWithFeedback(text, "관심 보드 요약을 복사했습니다.", "찜한 상품 흐름을 그대로 공유할 수 있습니다.");
        });
        elements.sortBookmarkNameButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "NAME_ASC";
            renderBookmarkBoard();
            showToast("관심 상품을 이름순으로 정렬했습니다.", "저장한 상품을 이름 기준으로 빠르게 찾을 수 있습니다.");
        });
        elements.filterBookmarkLowStockButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "LOW_STOCK";
            renderBookmarkBoard();
            showToast("관심 보드에서 저재고만 표시합니다.", "구매 판단이 급한 상품만 남겼습니다.");
        });
        elements.filterBookmarkFeaturedButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "FEATURED";
            renderBookmarkBoard();
            showToast("관심 보드에서 Featured만 표시합니다.", "대표 큐레이션 상품만 남겼습니다.");
        });
        elements.resetBookmarkBoardFilterButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "ALL";
            renderBookmarkBoard();
            showToast("관심 보드 필터를 해제했습니다.", "저장한 관심 상품을 모두 표시합니다.");
        });
        elements.filterBookmarkAvailableButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "AVAILABLE";
            renderBookmarkBoard();
            showToast("구매 가능한 관심 상품만 표시합니다.", "현재 재고가 있는 후보로 보드를 좁혔습니다.");
        });
        elements.filterBookmarkSoldOutButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "SOLD_OUT";
            renderBookmarkBoard();
            showToast("품절 관심 상품만 표시합니다.", "대체 상품 탐색이 필요한 후보를 모았습니다.");
        });
        elements.filterBookmarkCategoryButton?.addEventListener("click", () => {
            boardState.bookmarkFilter = "DOMINANT_CATEGORY";
            renderBookmarkBoard();
            showToast("대표 카테고리 관심 상품만 표시합니다.", "가장 많이 저장한 카테고리 흐름을 남겼습니다.");
        });
        elements.sortBookmarkOldestButton?.addEventListener("click", () => {
            boardState.bookmarkSort = "OLDEST";
            renderBookmarkBoard();
            showToast("오래 담은 관심 상품부터 정렬했습니다.", "이전에 저장한 후보를 먼저 재검토할 수 있습니다.");
        });
        elements.exportBookmarkCsvButton?.addEventListener("click", () => {
            exportProductsCsv(sortedBookmarkProducts(readBookmarkProducts()), "grade-stock-bookmarks.csv");
        });
        elements.openRecommendedBookmarkButton?.addEventListener("click", () => {
            const recommended = recommendedBookmarkProduct(sortedBookmarkProducts(readBookmarkProducts()));
            if (!recommended) {
                showToast("추천할 관심 상품이 없습니다.", "현재 보드 필터를 해제하거나 상품을 추가해주세요.", true);
                return;
            }
            openDrawer(recommended.id);
            showToast("우선 확인 상품을 열었습니다.", `${recommended.headline || recommended.name}을 먼저 확인합니다.`);
        });
        elements.selectVisibleProductsButton?.addEventListener("click", () => {
            recordSelectionSnapshot("현재 상품 전체 선택");
            currentCatalogPageProducts().forEach((product) => selectedProductIds.add(Number(product.id)));
            renderCatalog();
            showToast("현재 상품을 모두 선택했습니다.", `${selectedProductIds.size}개 상품을 일괄 작업할 수 있습니다.`);
        });
        elements.selectLowStockPageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Number(product.stock || 0) < lowStockThresholdValue(), "저재고");
        });
        elements.selectFeaturedPageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Boolean(product.featured), "Featured");
        });
        elements.selectAvailablePageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Number(product.stock || 0) > 0, "구매 가능");
        });
        elements.selectStablePageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Number(product.stock || 0) >= lowStockThresholdValue(), "안정 재고");
        });
        elements.selectPremiumPageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Number(product.price || 0) > 300000, "30만원 초과");
        });
        elements.selectDominantBrandPageButton?.addEventListener("click", () => {
            const brand = dominantBrand(currentCatalogPageProducts());
            selectCurrentPageProducts((product) => product.brand === brand, "대표 브랜드");
        });
        elements.removeLowStockSelectionButton?.addEventListener("click", () => {
            recordSelectionSnapshot("긴장 재고 선택 제외");
            selectedProducts().filter((product) => Number(product.stock || 0) < lowStockThresholdValue())
                .forEach((product) => selectedProductIds.delete(Number(product.id)));
            renderCatalog();
            showToast("긴장 재고 선택을 제외했습니다.", "안정 재고 선택만 남겼습니다.");
        });
        elements.invertPageSelectionButton?.addEventListener("click", invertCurrentPageSelection);
        elements.removeSoldOutSelectionButton?.addEventListener("click", () => {
            const soldOutIds = selectedProducts()
                .filter((product) => Number(product.stock || 0) <= 0)
                .map((product) => Number(product.id));
            recordSelectionSnapshot("품절 선택 제외");
            soldOutIds.forEach((productId) => selectedProductIds.delete(productId));
            renderCatalog();
            showToast("품절 선택을 제외했습니다.", `${soldOutIds.length}개 품절 상품을 선택 목록에서 정리했습니다.`);
        });
        elements.selectCheapestPageButton?.addEventListener("click", () => selectRankedPageProducts("PRICE_LOW", 3));
        elements.selectHighestStockPageButton?.addEventListener("click", () => selectRankedPageProducts("STOCK_HIGH", 3));
        elements.selectSoldOutPageButton?.addEventListener("click", () => {
            selectCurrentPageProducts((product) => Number(product.stock || 0) <= 0, "품절");
        });
        elements.openCheapestSelectedButton?.addEventListener("click", () => openSelectedProductByMetric("PRICE_LOW"));
        elements.openHighestStockSelectedButton?.addEventListener("click", () => openSelectedProductByMetric("STOCK_HIGH"));
        elements.copyCurrentPageSummaryButton?.addEventListener("click", async () => {
            await copyTextWithFeedback(
                catalogSummaryClipboardText(currentCatalogPageProducts()),
                "현재 페이지 요약을 복사했습니다.",
                "표시 중인 상품의 가격과 재고를 바로 전달할 수 있습니다."
            );
        });
        elements.exportCurrentPageCsvButton?.addEventListener("click", exportCurrentPageCsv);
        elements.exportSelectedProductsCsvButton?.addEventListener("click", () => {
            exportProductsCsv(selectedProducts(), "grade-stock-selected-products.csv");
        });
        elements.catalogPageSize?.addEventListener("change", () => {
            paginationState.size = elements.catalogPageSize.value;
            paginationState.page = 1;
            paginationState.extra = 0;
            window.localStorage.setItem(PAGE_SIZE_KEY, paginationState.size);
            reloadCatalogPage();
            showToast("페이지 표시 개수를 변경했습니다.", `${paginationState.size}개 단위로 상품을 표시합니다.`);
        });
        elements.catalogPageSelect?.addEventListener("change", () => {
            moveCatalogPage(Number(elements.catalogPageSelect.value));
        });
        elements.catalogFirstPageButton?.addEventListener("click", () => moveCatalogPage(1));
        elements.catalogPreviousPageButton?.addEventListener("click", () => moveCatalogPage(paginationState.page - 1));
        elements.catalogNextPageButton?.addEventListener("click", () => moveCatalogPage(paginationState.page + 1));
        elements.catalogLastPageButton?.addEventListener("click", () => moveCatalogPage(catalogPaginationDetails().totalPages));
        elements.catalogLoadMoreButton?.addEventListener("click", () => {
            const nextSize = [6, 12, 24, 48].find((size) => size > requestedCatalogPageSize()) || 48;
            paginationState.size = String(nextSize);
            window.localStorage.setItem(PAGE_SIZE_KEY, paginationState.size);
            paginationState.extra = 0;
            paginationState.page = 1;
            reloadCatalogPage();
            showToast("상품을 더 불러옵니다.", `현재 페이지에 최대 ${nextSize}개 상품을 표시합니다.`);
        });
        elements.copyCurrentPageLinksButton?.addEventListener("click", async () => {
            const links = currentCatalogPageProducts().map((product) =>
                `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`
            );
            await copyTextWithFeedback(links.join("\n") || "현재 페이지에 상품이 없습니다.", "현재 페이지 링크를 복사했습니다.", `${links.length}개 상품 상세 링크를 전달할 수 있습니다.`);
        });
        elements.clearSelectedProductsButton?.addEventListener("click", () => {
            recordSelectionSnapshot("전체 선택 해제");
            selectedProductIds.clear();
            renderCatalog();
            showToast("상품 선택을 해제했습니다.", "카탈로그를 기본 상태로 복구했습니다.");
        });
        elements.undoCatalogSelectionButton?.addEventListener("click", undoCatalogSelection);
        elements.redoCatalogSelectionButton?.addEventListener("click", redoCatalogSelection);
        elements.clearCatalogSelectionHistoryButton?.addEventListener("click", () => {
            selectionHistory.length = 0;
            selectionFuture.length = 0;
            renderCatalogSelection();
            showToast("선택 이력을 비웠습니다.", "현재 선택 상태는 그대로 유지합니다.");
        });
        elements.compareSelectedProductsButton?.addEventListener("click", () => {
            addProductsToBoard(selectedProducts(), "COMPARE");
        });
        elements.bookmarkSelectedProductsButton?.addEventListener("click", () => {
            addProductsToBoard(selectedProducts(), "BOOKMARK");
        });
        elements.hideSelectedProductsButton?.addEventListener("click", async () => {
            const selected = selectedProducts();
            if (!selected.length) {
                showToast("숨길 상품이 없습니다.", "카탈로그 카드에서 상품을 먼저 선택해주세요.", true);
                return;
            }
            const current = readHiddenProducts();
            const next = selected.map(hiddenProductSummary).concat(current).filter((product, index, items) =>
                items.findIndex((item) => Number(item.id) === Number(product.id)) === index
            ).slice(0, 12);
            window.localStorage.setItem(HIDDEN_PRODUCTS_KEY, JSON.stringify(next));
            selectedProductIds.clear();
            renderHiddenProducts();
            renderFlowBoard();
            await refreshCatalog();
            showToast("선택 상품을 숨겼습니다.", `${selected.length}개 상품을 기본 목록에서 제외했습니다.`);
        });
        elements.copySelectedSummaryButton?.addEventListener("click", async () => {
            await copyProductCollection(selectedProducts(), "선택 상품", "선택 상품 요약을 복사했습니다.");
        });
        elements.copySelectedLinksButton?.addEventListener("click", async () => {
            const links = selectedProducts().map((product) => `${product.headline || product.name}: ${window.location.origin}${detailPageUrl(product.id)}`);
            await copyTextWithFeedback(links.join("\n") || "선택한 상품이 없습니다.", "선택 상품 링크를 복사했습니다.", "선택한 상세 페이지를 한 번에 공유할 수 있습니다.");
        });
        elements.openUrgentSelectedButton?.addEventListener("click", () => {
            const urgent = selectedProducts().sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0))[0];
            if (!urgent) {
                showToast("열 상품이 없습니다.", "카탈로그 카드에서 상품을 먼저 선택해주세요.", true);
                return;
            }
            openDrawer(urgent.id);
            showToast("최저 재고 상품을 열었습니다.", `${urgent.headline || urgent.name}을 우선 확인합니다.`);
        });
        elements.focusSelectedBrandButton?.addEventListener("click", async () => {
            const brand = dominantBrand(selectedProducts());
            if (!brand) {
                showToast("집중할 브랜드가 없습니다.", "카탈로그 카드에서 상품을 먼저 선택해주세요.", true);
                return;
            }
            state.brand = brand;
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("대표 브랜드 조건을 적용했습니다.", `${brand} 상품 흐름으로 카탈로그를 좁혔습니다.`);
        });
        elements.catalogGrid?.addEventListener("click", (event) => {
            const previewButton = event.target.closest(".catalog-card__button[data-product-id]");
            if (previewButton) {
                openDrawer(Number(previewButton.dataset.productId));
                return;
            }
            const selectButton = event.target.closest("[data-select-product-id]");
            if (selectButton) {
                toggleSelectedProduct(Number(selectButton.dataset.selectProductId));
                return;
            }
            const compareButton = event.target.closest("[data-compare-product-id]");
            if (compareButton) {
                toggleCompareProduct(Number(compareButton.dataset.compareProductId));
                return;
            }
            const bookmarkButton = event.target.closest("[data-bookmark-product-id]");
            if (bookmarkButton) {
                toggleBookmarkProduct(Number(bookmarkButton.dataset.bookmarkProductId));
                return;
            }
            const copyButton = event.target.closest("[data-copy-product-id]");
            if (copyButton) {
                copyCatalogCardSummary(Number(copyButton.dataset.copyProductId));
                return;
            }
            const shareButton = event.target.closest("[data-share-product-id]");
            if (shareButton) {
                shareCatalogCardLink(Number(shareButton.dataset.shareProductId));
                return;
            }
            const focusButton = event.target.closest("[data-card-focus][data-product-id]");
            if (focusButton) {
                applyCatalogCardFocus(Number(focusButton.dataset.productId), focusButton.dataset.cardFocus);
            }
        });
        elements.catalogGrid?.addEventListener("keydown", handleCatalogCardNavigation);
        elements.catalogFilterPanel?.addEventListener("toggle", persistCatalogFilterPanelState);
        elements.catalogGrid?.addEventListener("pointerover", warmCatalogProductDetail);
        elements.catalogGrid?.addEventListener("focusin", warmCatalogProductDetail);
        window.addEventListener("popstate", async () => {
            hydrateStateFromUrl();
            syncControls();
            await refreshCatalog();
        });
        window.addEventListener("scroll", syncScrollState, { passive: true });
        elements.scrollTopButton?.addEventListener("click", () => {
            window.scrollTo({ top: 0, behavior: "smooth" });
        });
        elements.resetPersonalDataButton?.addEventListener("click", resetPersonalData);
        window.addEventListener("storage", syncPersonalStateFromStorage);
        document.addEventListener("storefront:state-ready", () => syncPersonalStateFromStorage({ key: null }));
        window.addEventListener("pagehide", persistCatalogScrollPosition);
        window.addEventListener("online", handleNetworkReconnect);
        window.addEventListener("offline", handleNetworkOffline);
        document.addEventListener("visibilitychange", handleVisibilityChange);
        elements.networkRetryButton?.addEventListener("click", handleNetworkReconnect);
        elements.networkDismissButton?.addEventListener("click", () => {
            networkStatusDismissed = true;
            if (elements.networkStatus) {
                elements.networkStatus.hidden = true;
            }
        });
        elements.keyboardHelpButton?.addEventListener("click", openShortcutHelp);
        elements.shortcutHelpCloseButton?.addEventListener("click", closeShortcutHelp);
        elements.shortcutHelpModal?.addEventListener("click", (event) => {
            if (event.target === elements.shortcutHelpModal) {
                closeShortcutHelp();
            }
        });
    }

    function handleSectionKeyboardShortcut(event) {
        if (!event.altKey || !/^Digit[1-5]$/.test(event.code)) {
            return false;
        }
        const shortcutTargets = {
            Digit1: ["top", "홈"],
            Digit2: ["featured", "추천 상품"],
            Digit3: ["catalog", "상품 카탈로그"],
            Digit4: ["bookmarkBoardSection", "관심 상품 보드"],
            Digit5: ["compareBoardSection", "비교 상품 보드"]
        };
        const [targetId, label] = shortcutTargets[event.code];
        const section = document.getElementById(targetId);
        event.preventDefault();
        if (!section || section.hidden) {
            showToast(`${label}가 비어 있습니다.`, "상품을 먼저 저장하거나 비교 보드에 추가해주세요.", true);
            return true;
        }
        section.setAttribute("tabindex", "-1");
        section.scrollIntoView({ behavior: "smooth", block: "start" });
        window.requestAnimationFrame(() => section.focus({ preventScroll: true }));
        announceStorefrontStatus(`${label}로 이동했습니다.`);
        return true;
    }

    function openHeaderSearch() {
        if (!elements.headerSearchPanel) {
            return;
        }
        closeDrawer();
        closeShortcutHelp();
        closeMobileMenu();
        if (elements.headerSearchPanel.hidden) {
            headerSearchReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        }
        elements.headerSearchPanel.hidden = false;
        document.body.classList.add("is-header-search-open");
        elements.openDrawerFromTop?.setAttribute("aria-expanded", "true");
        if (elements.headerSearchInput) {
            elements.headerSearchInput.value = state.search;
            window.requestAnimationFrame(() => elements.headerSearchInput.focus());
        }
    }

    function closeHeaderSearch(restoreFocus = false) {
        if (!elements.headerSearchPanel || elements.headerSearchPanel.hidden) {
            return;
        }
        elements.headerSearchPanel.hidden = true;
        document.body.classList.remove("is-header-search-open");
        elements.openDrawerFromTop?.setAttribute("aria-expanded", "false");
        if (elements.mobileStoreNav?.querySelector('[data-mobile-nav="SEARCH"].is-active')) {
            syncMobileStoreNavigation(currentMobileNavigationAction());
        }
        if (restoreFocus && headerSearchReturnFocus?.isConnected) {
            headerSearchReturnFocus.focus();
        }
        headerSearchReturnFocus = null;
    }

    async function applyHeaderSearch() {
        state.search = elements.headerSearchInput?.value.trim().toLowerCase() || "";
        if (elements.searchInput) {
            elements.searchInput.value = state.search;
        }
        closeHeaderSearch();
        await refreshCatalog();
        document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    async function applyHomePreset(preset) {
        applyPreset(preset);
        syncControls();
        await refreshCatalog();
        document.getElementById(preset === "FEATURED" ? "featured" : "catalog")
            ?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function toggleMobileMenu() {
        if (elements.topbarSubnav?.classList.contains("is-mobile-open")) {
            closeMobileMenu(true);
            return;
        }
        openMobileMenu();
    }

    function openMobileMenu() {
        if (!elements.topbarSubnav || elements.topbarSubnav.classList.contains("is-mobile-open")) {
            return;
        }
        closeDrawer();
        closeShortcutHelp();
        closeHeaderSearch();
        mobileMenuReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        elements.topbarSubnav.classList.add("is-mobile-open");
        elements.mobileMenuButton?.classList.add("is-active");
        elements.mobileMenuButton?.setAttribute("aria-expanded", "true");
        elements.mobileMenuButton?.setAttribute("aria-label", "메뉴 닫기");
        window.requestAnimationFrame(() => elements.topbarSubnav?.querySelector("a")?.focus());
    }

    function closeMobileMenu(restoreFocus = false) {
        if (!elements.topbarSubnav?.classList.contains("is-mobile-open")) {
            return;
        }
        elements.topbarSubnav?.classList.remove("is-mobile-open");
        elements.mobileMenuButton?.classList.remove("is-active");
        elements.mobileMenuButton?.setAttribute("aria-expanded", "false");
        elements.mobileMenuButton?.setAttribute("aria-label", "메뉴 열기");
        if (restoreFocus && mobileMenuReturnFocus?.isConnected) {
            mobileMenuReturnFocus.focus();
        }
        mobileMenuReturnFocus = null;
    }

    function handleMobileMenuKeyboard(event) {
        if (!elements.topbarSubnav?.classList.contains("is-mobile-open")
            || !["ArrowLeft", "ArrowRight", "Home", "End"].includes(event.key)) {
            return;
        }
        const links = Array.from(elements.topbarSubnav.querySelectorAll("a"));
        if (!links.length) {
            return;
        }
        event.preventDefault();
        const currentIndex = Math.max(0, links.indexOf(document.activeElement));
        const nextIndex = event.key === "Home" ? 0
            : event.key === "End" ? links.length - 1
                : (currentIndex + (event.key === "ArrowRight" ? 1 : -1) + links.length) % links.length;
        links[nextIndex].focus();
    }

    function handleMobileStoreNavigation(event) {
        const button = event.target.closest("[data-mobile-nav]");
        if (!button) {
            return;
        }
        const action = button.dataset.mobileNav;
        if (action === "SEARCH") {
            if (elements.headerSearchPanel?.hidden === false) {
                closeHeaderSearch();
            } else {
                syncMobileStoreNavigation("SEARCH");
                openHeaderSearch();
                announceStorefrontStatus("모바일 상품 검색을 열었습니다.");
            }
            return;
        }
        closeHeaderSearch();
        if (action === "SAVED") {
            const bookmarked = readBookmarkProducts();
            if (!bookmarked.length) {
                syncMobileStoreNavigation("SHOP");
                showToast("관심 상품이 없습니다.", "상품 카드의 관심 버튼을 눌러 저장해보세요.", true);
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
                return;
            }
            syncMobileStoreNavigation("SAVED");
            renderBookmarkBoard();
            document.getElementById("bookmarkBoardSection")?.scrollIntoView({ behavior: "smooth", block: "start" });
            announceStorefrontStatus("관심 상품 보드로 이동했습니다.");
            return;
        }
        syncMobileStoreNavigation(action);
        const targetId = action === "HOME" ? "top" : action === "FEATURED" ? "featured" : "catalog";
        document.getElementById(targetId)?.scrollIntoView({ behavior: "smooth", block: "start" });
        announceStorefrontStatus(`${button.querySelector("strong")?.textContent || "선택한 영역"}으로 이동했습니다.`);
    }

    function initMobileStoreNavigation() {
        if (!elements.mobileStoreNav) {
            return;
        }
        syncMobileStoreNavigation("HOME");
        if (typeof IntersectionObserver === "undefined") {
            return;
        }
        const targets = [
            ["HOME", document.getElementById("top")],
            ["FEATURED", document.getElementById("featured")],
            ["SHOP", document.getElementById("catalog")],
            ["SAVED", document.getElementById("bookmarkBoardSection")]
        ].filter(([, section]) => section);
        const observer = new IntersectionObserver((entries) => {
            if (elements.headerSearchPanel?.hidden === false) {
                return;
            }
            const visible = entries
                .filter((entry) => entry.isIntersecting)
                .sort((left, right) => right.intersectionRatio - left.intersectionRatio)[0];
            const active = targets.find(([, section]) => section === visible?.target)?.[0];
            if (active) {
                syncMobileStoreNavigation(active);
            }
        }, { rootMargin: "-20% 0px -65% 0px", threshold: [0.1, 0.35] });
        targets.forEach(([, section]) => observer.observe(section));
    }

    function syncMobileStoreNavigation(activeAction) {
        if (activeAction) {
            elements.mobileStoreNav?.querySelectorAll("[data-mobile-nav]").forEach((button) => {
                const active = button.dataset.mobileNav === activeAction;
                button.classList.toggle("is-active", active);
                if (active) {
                    button.setAttribute("aria-current", "page");
                } else {
                    button.removeAttribute("aria-current");
                }
            });
        }
        const savedCount = readBookmarkProducts().length;
        setText(elements.mobileSavedCount, String(savedCount));
        elements.mobileStoreNav?.querySelector('[data-mobile-nav="SAVED"]')
            ?.setAttribute("aria-label", `관심 상품으로 이동, ${savedCount}개 저장됨`);
    }

    function currentMobileNavigationAction() {
        const sections = [
            ["HOME", document.getElementById("top")],
            ["FEATURED", document.getElementById("featured")],
            ["SHOP", document.getElementById("catalog")],
            ["SAVED", document.getElementById("bookmarkBoardSection")]
        ].filter(([, section]) => section && !section.hidden);
        const viewportAnchor = window.innerHeight * 0.3;
        return sections.sort((left, right) =>
            Math.abs(left[1].getBoundingClientRect().top - viewportAnchor) - Math.abs(right[1].getBoundingClientRect().top - viewportAnchor)
        )[0]?.[0] || "HOME";
    }

    function moveHeroSlide(direction, restart = false) {
        activeHeroSlide = (activeHeroSlide + direction + heroSlides.length) % heroSlides.length;
        renderHeroSlide();
        if (restart) {
            restartHeroCarousel();
        }
    }

    function initHeroCarousel() {
        const hero = document.querySelector(".hero");
        if (!hero) {
            return;
        }
        renderHeroDots();
        syncHeroPlaybackControl();
        hero.addEventListener("mouseenter", stopHeroCarousel);
        hero.addEventListener("mouseleave", startHeroCarousel);
        hero.addEventListener("focusin", stopHeroCarousel);
        hero.addEventListener("focusout", (event) => {
            if (!hero.contains(event.relatedTarget)) {
                startHeroCarousel();
            }
        });
        hero.addEventListener("pointerdown", (event) => {
            heroPointerStartX = event.clientX;
        });
        hero.addEventListener("pointerup", (event) => {
            if (heroPointerStartX === null) {
                return;
            }
            const distance = event.clientX - heroPointerStartX;
            heroPointerStartX = null;
            if (Math.abs(distance) >= 48) {
                moveHeroSlide(distance > 0 ? -1 : 1, true);
            }
        });
        document.addEventListener("visibilitychange", () => document.hidden ? stopHeroCarousel() : startHeroCarousel());
        startHeroCarousel();
    }

    function renderHeroDots() {
        if (!elements.heroDots) {
            return;
        }
        elements.heroDots.innerHTML = heroSlides.map((_, index) => `
            <button type="button" data-hero-slide="${index}" aria-label="${index + 1}번 배너" aria-current="${index === activeHeroSlide ? "true" : "false"}"></button>
        `).join("");
    }

    function startHeroCarousel() {
        if (heroCarouselPaused || window.matchMedia("(prefers-reduced-motion: reduce)").matches || window.navigator.connection?.saveData || document.hidden) {
            return;
        }
        stopHeroCarousel();
        heroCarouselTimer = window.setInterval(() => moveHeroSlide(1), 5000);
    }

    function stopHeroCarousel() {
        if (heroCarouselTimer) {
            window.clearInterval(heroCarouselTimer);
            heroCarouselTimer = null;
        }
    }

    function syncHeroPlaybackControl() {
        if (!elements.heroPauseButton) {
            return;
        }
        elements.heroPauseButton.textContent = heroCarouselPaused ? "Play" : "Pause";
        elements.heroPauseButton.setAttribute("aria-pressed", String(heroCarouselPaused));
        elements.heroPauseButton.setAttribute("aria-label", heroCarouselPaused ? "배너 자동 전환 재생" : "배너 자동 전환 일시정지");
    }

    function restartHeroCarousel() {
        stopHeroCarousel();
        startHeroCarousel();
    }

    function renderHeroSlide() {
        const hero = document.querySelector(".hero");
        const slide = heroSlides[activeHeroSlide];
        if (!hero || !slide) {
            return;
        }
        setText(hero.querySelector(".hero-copy > .eyebrow"), slide.eyebrow);
        const title = hero.querySelector(".hero-copy > h1");
        if (title) {
            title.innerHTML = slide.title;
        }
        setText(hero.querySelector(".hero-description"), slide.description);
        if (elements.heroCampaignImage) {
            elements.heroCampaignImage.src = slide.image;
        }
        hero.dataset.tone = slide.tone;
        setText(elements.heroSlideStatus, `${activeHeroSlide + 1} / ${heroSlides.length}`);
        renderHeroDots();
    }

    function renderHeroMetrics() {
        setText(elements.metricCount, String(metrics.totalCount || products.length));
        setText(elements.metricLowStock, String(metrics.lowStockCount || 0));
        setText(elements.metricToday, String(metrics.latestDropCount || 0));
    }

    function renderFlowBoard() {
        if (!elements.flowBoardGrid) {
            return;
        }
        const recentProducts = readRecentProducts();
        const comparedProducts = readCompareProducts();
        const bookmarkedProducts = readBookmarkProducts();
        const hiddenProducts = readHiddenProducts();
        const managedProductCount = recentProducts.length + comparedProducts.length + bookmarkedProducts.length;
        const activeBoardCount = [recentProducts, comparedProducts, bookmarkedProducts, hiddenProducts].filter((items) => items.length).length;

        setText(
            elements.flowBoardTitle,
            managedProductCount
                ? `${managedProductCount}개 개인 보드 흐름을 유지하고 있습니다.`
                : "아직 개인 보드에 누적된 상품이 없습니다."
        );
        setText(
            elements.flowBoardText,
            activeBoardCount
                ? `최근 ${recentProducts.length} · 비교 ${comparedProducts.length} · 관심 ${bookmarkedProducts.length} · 숨김 ${hiddenProducts.length} 상태를 홈에서 바로 이어갈 수 있습니다.`
                : "카탈로그와 상세에서 상품을 둘러보면 최근, 비교, 관심, 숨김 흐름이 자동으로 쌓입니다."
        );

        const cards = [
            {
                action: "RECENT",
                label: "최근 본 상품",
                count: recentProducts.length,
                headline: recentProducts[0]?.headline || recentProducts[0]?.name || "상세에서 본 상품이 아직 없습니다.",
                meta: recentProducts.length ? `${recentProducts[0]?.brand || "-"} · 최근 확인` : "탐색 대기",
                active: recentProducts.length > 0
            },
            {
                action: "COMPARE",
                label: "비교 상품",
                count: comparedProducts.length,
                headline: comparedProducts.length ? buildCompareSummary(comparedProducts) : "비교 보드가 비어 있습니다.",
                meta: comparedProducts.length ? `${dominantCategory(comparedProducts) || "복합 비교"} · 최대 3개` : "상품을 추가해보세요",
                active: comparedProducts.length > 0
            },
            {
                action: "BOOKMARK",
                label: "관심 상품",
                count: bookmarkedProducts.length,
                headline: bookmarkedProducts.length ? buildBookmarkSummary(bookmarkedProducts) : "관심 상품이 아직 없습니다.",
                meta: bookmarkedProducts.length ? `${bookmarkedProducts.filter((product) => product.featured).length}개 추천 상품 포함` : "상품을 저장해보세요",
                active: bookmarkedProducts.length > 0
            },
            {
                action: "HIDDEN",
                label: "숨긴 상품",
                count: hiddenProducts.length,
                headline: hiddenProducts[0]?.headline || hiddenProducts[0]?.name || "숨긴 상품이 없습니다.",
                meta: hiddenProducts.length ? "목록에서 다시 확인 가능" : "정리된 상태",
                active: hiddenProducts.length > 0
            }
        ];

        elements.flowBoardGrid.innerHTML = cards.map((card) => `
            <button class="flow-board__card ${card.active ? "is-active" : ""}" type="button" data-flow-action="${card.action}" aria-label="${card.label} ${card.count}개 보기">
                <span class="flow-board__topline">
                    <span class="flow-board__label">${card.label}</span>
                    <span class="flow-board__status" aria-hidden="true"></span>
                </span>
                <span class="flow-board__count"><strong>${card.count}</strong>개</span>
                <span class="flow-board__headline">${card.headline}</span>
                <span class="flow-board__meta">${card.meta}</span>
                <span class="flow-board__link">바로가기 <span aria-hidden="true">→</span></span>
            </button>
        `).join("");

        elements.flowBoardGrid.querySelectorAll("[data-flow-action]").forEach((button) => {
            button.addEventListener("click", async () => {
                await handleFlowAction(button.dataset.flowAction);
            });
        });
    }

    function featuredRankLabel(product) {
        if (product.featuredRank) {
            return `Featured ${product.featuredRank}`;
        }
        return product.model || "Curated";
    }

    function stockPressureLabel(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return "즉시 확인";
        }
        if (quantity < lowStockThresholdValue()) {
            return "품절 임박";
        }
        return "재고 안정";
    }

    function stockPressureDetail(stock) {
        const quantity = Number(stock || 0);
        if (quantity <= 5) {
            return `재고 ${quantity}개로 즉시 대응이 필요합니다.`;
        }
        if (quantity < lowStockThresholdValue()) {
            return `재고 ${quantity}개로 긴장 구간에 들어가 있습니다.`;
        }
        return `재고 ${quantity}개로 비교적 안정적인 구간입니다.`;
    }

    function relativeDropLabel(createdDate) {
        if (!createdDate) {
            return "최근 등록";
        }
        const latestCreatedDate = metrics.latestCreatedDate;
        if (latestCreatedDate && createdDate === latestCreatedDate) {
            return "오늘 등록";
        }
        return `${createdDate} 등록`;
    }

    function moodLabel(product) {
        return product.mood || product.category || "Curated";
    }

    function compactProductContext(product) {
        return [product.brand, product.category, product.priceLabel || formatPrice(product.price)]
            .filter(Boolean)
            .join(" · ");
    }

    function renderBrandSpotlight() {
        if (!elements.brandSpotlightGrid) {
            return;
        }
        const rankedBrands = brandFacets.slice(0, 5).map((facet) => {
            return {
                brand: facet.value,
                count: facet.count
            };
        });

        if (!rankedBrands.length) {
            elements.brandSpotlightGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>브랜드 데이터를 준비 중입니다.</strong>
                    <p>상품을 불러오면 상단 랭킹을 보여줍니다.</p>
                </article>
            `;
            return;
        }

        elements.brandSpotlightGrid.innerHTML = rankedBrands.map((item, index) => `
            <a class="brand-rank-card" href="/front/brands?brand=${encodeURIComponent(item.brand)}">
                <span class="brand-rank-card__visual" aria-hidden="true">${brandInitials(item.brand)}</span>
                <strong>${item.brand}</strong>
                <span>${item.count}개 상품</span>
                <em>${index + 1}위</em>
            </a>
        `).join("");
    }

    function renderCategoryShortcuts() {
        if (!elements.categoryShortcutGrid) {
            return;
        }
        const rankedCategories = categoryFacets.slice(0, 6).map((facet) => ({
            category: facet.value,
            count: facet.count
        }));

        if (!rankedCategories.length) {
            elements.categoryShortcutGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>카테고리 데이터를 준비 중입니다.</strong>
                    <p>상품을 불러오면 바로가기 카드를 보여줍니다.</p>
                </article>
            `;
            return;
        }

        elements.categoryShortcutGrid.innerHTML = rankedCategories.map((item) => `
            <button class="category-shortcut-card" type="button" data-category-shortcut="${escapeAttribute(item.category)}">
                <span class="category-shortcut-card__visual" aria-hidden="true">${brandInitials(item.category)}</span>
                <strong>${item.category}</strong>
                <span>${item.count}개 상품</span>
            </button>
        `).join("");

        elements.categoryShortcutGrid.querySelectorAll("[data-category-shortcut]").forEach((button) => {
            button.addEventListener("click", async () => {
                state.category = button.dataset.categoryShortcut;
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function renderSignalStrip() {
        renderLatestDrops();
        renderLowStockHighlights();
        syncCurationButtons();
    }

    function latestDropProducts() {
        const list = homeCollections.latestDrops.slice();
        if (boardState.latestSort === "PRICE_LOW") {
            return list.sort((left, right) => Number(left.price || 0) - Number(right.price || 0)).slice(0, 4);
        }
        return list.slice(0, 4);
    }

    function lowStockHighlightProducts() {
        const list = homeCollections.lowStock.slice();
        if (boardState.lowStockSort === "PRICE_LOW") {
            return list.sort((left, right) => Number(left.price || 0) - Number(right.price || 0)).slice(0, 4);
        }
        return list.sort((left, right) => left.stock - right.stock).slice(0, 4);
    }

    function featuredProducts() {
        const list = homeCollections.recommended.slice();
        if (boardState.featuredSort === "PRICE_LOW") {
            return list.sort((left, right) => Number(left.price || 0) - Number(right.price || 0)).slice(0, 4);
        }
        if (boardState.featuredSort === "STOCK_ASC") {
            return list.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0)).slice(0, 4);
        }
        return list.slice(0, 4);
    }

    function renderLatestDrops() {
        if (!elements.latestDropGrid) {
            return;
        }
        const latestProducts = latestDropProducts();

        if (!latestProducts.length) {
            destroyProductSwiper("latest", elements.latestDropGrid, elements.latestDropPreviousButton, elements.latestDropNextButton);
            elements.latestDropGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>신규 드롭을 준비 중입니다.</strong>
                    <p>최근 등록된 상품을 불러오면 이곳에 보여줍니다.</p>
                </article>
            `;
            return;
        }

        renderProductSwiper(
            "latest",
            elements.latestDropGrid,
            latestProducts.map((product) => signalFeedCard(product, relativeDropLabel(product.createdDate), true)).join(""),
            elements.latestDropPreviousButton,
            elements.latestDropNextButton
        );
    }

    function renderLowStockHighlights() {
        if (!elements.lowStockGrid) {
            return;
        }
        const lowStockProducts = lowStockHighlightProducts();

        if (!lowStockProducts.length) {
            destroyProductSwiper("low-stock", elements.lowStockGrid, elements.lowStockPreviousButton, elements.lowStockNextButton);
            elements.lowStockGrid.innerHTML = `
                <article class="catalog-empty">
                    <strong>긴장 재고 상품이 없습니다.</strong>
                    <p>현재 기준으로는 안정 재고 상품이 더 많습니다.</p>
                </article>
            `;
            return;
        }

        renderProductSwiper(
            "low-stock",
            elements.lowStockGrid,
            lowStockProducts.map((product) => signalFeedCard(product, stockPressureLabel(product.stock), true)).join(""),
            elements.lowStockPreviousButton,
            elements.lowStockNextButton
        );
    }

    function signalFeedCard(product, kicker, slide = false) {
        if (slide) {
            return productRailCard(product, kicker, "signal-feed-card", true);
        }
        return productRailCard(product, kicker, "signal-feed-card");
    }

    function renderProductSwiper(key, container, slides, previousButton, nextButton) {
        destroyProductSwiperInstance(key);
        productRailSwipers.delete(key);
        container.classList.add("swiper");
        container.innerHTML = `<div class="swiper-wrapper">${slides}</div><div class="product-rail-pagination"></div>`;
        bindProductButtons(container);
        bindRailBookmarkButtons(container);
        refreshLucideIcons();

        if (typeof window.Swiper !== "function") {
            container.classList.add("is-grid-fallback");
            previousButton.hidden = true;
            nextButton.hidden = true;
            return;
        }

        container.classList.remove("is-grid-fallback");
        previousButton.hidden = false;
        nextButton.hidden = false;
        const swiper = new window.Swiper(container, {
            slidesPerView: 1.25,
            spaceBetween: 12,
            watchOverflow: true,
            observer: true,
            observeParents: true,
            keyboard: { enabled: true, onlyInViewport: true },
            navigation: { prevEl: previousButton, nextEl: nextButton },
            pagination: {
                el: container.querySelector(".product-rail-pagination"),
                clickable: true
            },
            breakpoints: {
                0: { slidesPerView: 1.25, spaceBetween: 12 },
                520: { slidesPerView: 2.15, spaceBetween: 14 },
                768: { slidesPerView: 3, spaceBetween: 16 },
                1100: { slidesPerView: 4, spaceBetween: 16 }
            },
            on: {
                slideChange(current) {
                    announceStorefrontStatus(`${key === "latest" ? "신규 드롭" : "저재고 상품"} ${current.activeIndex + 1}번째 항목을 보고 있습니다.`);
                }
            }
        });
        const visibilityObserver = typeof window.IntersectionObserver === "function"
            ? new window.IntersectionObserver((entries) => {
                if (!entries.some((entry) => entry.isIntersecting)) {
                    return;
                }
                swiper.update();
                visibilityObserver.disconnect();
            }, { rootMargin: "200px 0px" })
            : null;
        visibilityObserver?.observe(container);
        productRailSwipers.set(key, { swiper, visibilityObserver });
    }

    function destroyProductSwiper(key, container, previousButton, nextButton) {
        destroyProductSwiperInstance(key);
        productRailSwipers.delete(key);
        container.classList.remove("swiper", "is-grid-fallback");
        previousButton.hidden = true;
        nextButton.hidden = true;
    }

    function destroyProductSwiperInstance(key) {
        const current = productRailSwipers.get(key);
        current?.visibilityObserver?.disconnect();
        current?.swiper?.destroy(true, true);
    }

    function renderFeatured() {
        if (!elements.featuredGrid) {
            return;
        }
        const curatedProducts = featuredProducts();
        elements.featuredGrid.innerHTML = curatedProducts
            .map((product) => productRailCard(product, featuredRankLabel(product), "spotlight-card"))
            .join("");

        bindProductButtons(elements.featuredGrid);
        bindRailBookmarkButtons(elements.featuredGrid);
        syncCurationButtons();
    }

    function productRailCard(product, kicker, className, slide = false) {
        product = markupSafeObject(product);
        kicker = escapeMarkup(kicker);
        const bookmarked = isBookmarkedProduct(product.id);
        const visualClass = className === "spotlight-card" ? "spotlight-card__visual" : "signal-feed-card__visual";
        return `
            <article class="${className}${slide ? " swiper-slide" : ""} rail-product-card">
                <button class="rail-product-card__wish ${bookmarked ? "is-active" : ""}" type="button" data-bookmark-product-id="${product.id}" aria-pressed="${bookmarked}" aria-label="${bookmarked ? "관심 상품 해제" : "관심 상품 추가"}">
                    <i data-lucide="heart" aria-hidden="true"></i><span aria-hidden="true">${bookmarked ? "♥" : "♡"}</span>
                </button>
                <a class="rail-product-card__visual-link" href="${detailPageUrl(product.id)}" aria-label="${product.name} 상세 보기">
                    ${productVisualMarkup(product, visualClass)}
                </a>
                <div class="rail-product-card__body">
                    <span class="rail-product-card__brand">${product.brand}</span>
                    <span class="rail-product-card__kicker">${kicker}</span>
                    <h3><a href="${detailPageUrl(product.id)}">${product.name}</a></h3>
                    <strong class="rail-product-card__price">${product.priceLabel || formatPrice(product.price)}</strong>
                    <span class="rail-product-card__meta">${product.stockStatus || stockLabel(product.stock)} · 재고 ${Number(product.stock || 0)}개</span>
                    <span class="rail-product-card__category">${product.category}</span>
                    <a class="rail-product-card__detail" href="${detailPageUrl(product.id)}">상품 더보기</a>
                </div>
            </article>
        `;
    }

    function bindRailBookmarkButtons(container) {
        container.querySelectorAll("[data-bookmark-product-id]").forEach((button) => {
            button.addEventListener("click", () => toggleBookmarkProduct(Number(button.dataset.bookmarkProductId)));
        });
    }

    function refreshLucideIcons() {
        if (!window.lucide?.createIcons) {
            document.documentElement.classList.remove("lucide-ready");
            return;
        }
        window.lucide.createIcons({
            attrs: {
                width: 17,
                height: 17,
                "stroke-width": 1.8
            }
        });
        document.documentElement.classList.add("lucide-ready");
    }

    function renderSignals() {
        if (!elements.signalList) {
            return;
        }

        const latestCreatedDate = metrics.latestCreatedDate;
        const latestProducts = products
            .filter((product) => !latestCreatedDate || product.createdDate === latestCreatedDate)
            .sort((left, right) => left.stock - right.stock);
        const lowStockProducts = products
            .filter((product) => product.stock < lowStockThresholdValue())
            .sort((left, right) => left.stock - right.stock);
        const primarySignal = lowStockProducts[0] || latestProducts[0] || products[0];

        if (primarySignal) {
            setText(elements.todaySignalTitle, `${primarySignal.headline || primarySignal.name}을 우선 확인해야 합니다.`);
            setText(elements.todaySignalText, `${compactProductContext(primarySignal)} · ${stockPressureDetail(primarySignal.stock)}`);
        }

        const signals = [
            `${metrics.featuredCount || products.filter((product) => product.featured).length}개 상품이 이번 주 큐레이션에 포함되어 우선 노출 중입니다.`,
            `${metrics.lowStockCount || products.filter((product) => product.stock < lowStockThresholdValue()).length}개 상품이 재고 긴장 구간에 있어 빠른 확인이 필요합니다.`,
            `${metrics.totalStock || (products[0] ? products.reduce((sum, product) => sum + product.stock, 0) : 0)}개 재고와 ${metrics.latestDropCount || latestProducts.length}건 최신 드롭을 첫 화면에서 함께 추적합니다.`
        ];

        elements.signalList.innerHTML = signals.map((message, index) => `
            <article class="signal-card">
                <strong>Signal 0${index + 1}</strong>
                <span>${message}</span>
            </article>
        `).join("");
    }

    function renderCatalog() {
        const allList = filteredProducts();
        const details = catalogPaginationDetails(allList);
        paginationState.page = Math.min(Math.max(1, paginationState.page), details.totalPages);
        const list = currentCatalogPageProducts(allList);
        const positionOffset = (paginationState.page - 1) * details.effectiveSize;
        const bookmarkedIds = new Set(readBookmarkProducts().map((product) => Number(product.id)));
        syncCatalogDocumentTitle(Number(metrics.totalCount || 0));
        renderCatalogSummary(allList);
        renderCatalogSelection();
        renderCatalogPagination(allList);

        if (!elements.catalogGrid) {
            return;
        }

        applyCatalogDisplayClasses();

        if (catalogLoadError) {
            elements.catalogGrid.innerHTML = `
                <div class="catalog-empty" role="alert">
                    <strong>${escapeMarkup(catalogLoadError)}</strong>
                    <p>네트워크 상태를 확인한 뒤 다시 시도해주세요.</p>
                    <button class="catalog-reset-button" type="button" data-empty-action="RETRY">카탈로그 다시 불러오기</button>
                </div>
            `;
            bindEmptyStateButtons();
            return;
        }

        if (!list.length) {
            elements.catalogGrid.innerHTML = `
                <div class="catalog-empty">
                    <strong>조건에 맞는 상품이 없습니다.</strong>
                    <p>필터를 조금 넓히거나 검색어를 비워서 다시 확인해보세요.</p>
                    <div class="catalog-summary__actions">
                        <button class="catalog-reset-button" type="button" data-empty-action="RESET">조건 초기화</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="LOW_STOCK">긴장 재고만 보기</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="CLEAR_SEARCH" ${state.search ? "" : "disabled"}>검색어 해제</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="CLEAR_BRAND" ${state.brand !== "ALL" ? "" : "disabled"}>브랜드 해제</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="CLEAR_CATEGORY" ${state.category !== "ALL" ? "" : "disabled"}>카테고리 해제</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="CLEAR_PRICE" ${state.priceBand !== "ALL" ? "" : "disabled"}>가격대 해제</button>
                        <button class="catalog-reset-button" type="button" data-empty-action="CLEAR_STOCK" ${state.stock !== "ALL" ? "" : "disabled"}>재고 조건 해제</button>
                    </div>
                </div>
            `;
            bindEmptyStateButtons();
            return;
        }

        applyCatalogDisplayClasses();
        elements.catalogGrid.innerHTML = list.map((product, index) => {
            product = markupSafeObject(product);
            return `
                <article class="catalog-card ${selectedProductIds.has(Number(product.id)) ? "is-selected" : ""}" role="listitem" data-catalog-product-id="${product.id}" data-keyboard-hint="B 관심 · C 비교 · S 선택 · Q 미리보기 · L 링크" aria-label="${escapeAttribute(`${product.name}, ${product.priceLabel || formatPrice(product.price)}, ${product.stockStatus || stockLabel(product.stock)}`)}" aria-posinset="${positionOffset + index + 1}" aria-setsize="${details.total}" aria-keyshortcuts="Enter B C S Q L" tabindex="${index === 0 ? "0" : "-1"}">
                <button class="catalog-card__select ${selectedProductIds.has(Number(product.id)) ? "is-active" : ""}" type="button" data-select-product-id="${product.id}" aria-pressed="${selectedProductIds.has(Number(product.id))}" aria-label="${escapeAttribute(product.name)} ${selectedProductIds.has(Number(product.id)) ? "선택 해제" : "선택"}">
                    ${selectedProductIds.has(Number(product.id)) ? "선택됨" : "선택"}
                </button>
                <button class="catalog-card__wish ${bookmarkedIds.has(Number(product.id)) ? "is-active" : ""}" type="button" data-bookmark-product-id="${product.id}" aria-pressed="${bookmarkedIds.has(Number(product.id))}" aria-label="${escapeAttribute(product.name)} ${bookmarkedIds.has(Number(product.id)) ? "관심 상품 해제" : "관심 상품 추가"}">
                    <span aria-hidden="true">${bookmarkedIds.has(Number(product.id)) ? "♥" : "♡"}</span>
                </button>
                <a class="catalog-card__visual-link" href="${detailPageUrl(product.id)}" aria-label="${product.name} 상세 보기">
                    ${productVisualMarkup(product, "catalog-card__visual", { eager: index < 4 && !window.navigator.connection?.saveData })}
                </a>
                <div class="catalog-card__header">
                    <div>
                        <span class="catalog-card__label">${product.brand}</span>
                        <h3 class="catalog-card__title"><a href="${detailPageUrl(product.id)}">${product.name}</a></h3>
                        <div class="catalog-card__meta">
                            <span>${product.headline || product.category}</span>
                        </div>
                    </div>
                </div>
                <div class="catalog-card__footer">
                    <div>
                        <div class="catalog-card__price">${product.priceLabel || formatPrice(product.price)}</div>
                        <div class="catalog-card__meta">${product.stockStatus || stockLabel(product.stock)} · ${relativeDropLabel(product.createdDate)}</div>
                    </div>
                    <div class="catalog-card__action">
                        <a class="catalog-card__link" href="${detailPageUrl(product.id)}">더보기</a>
                        <button class="catalog-card__button" type="button" data-product-id="${product.id}" aria-label="${product.name} 빠른 보기">빠른 보기</button>
                    </div>
                </div>
            </article>
        `;
        }).join("");
    }

    function catalogPaginationDetails(list = filteredProducts()) {
        const total = Number(catalogPagination.totalElements || 0);
        const effectiveSize = Number(catalogPagination.size || requestedCatalogPageSize());
        return {
            total,
            effectiveSize,
            totalPages: Math.max(1, Number(catalogPagination.totalPages || Math.ceil(total / effectiveSize)))
        };
    }

    function currentCatalogPageProducts(list = filteredProducts()) {
        return list;
    }

    async function moveCatalogPage(nextPage) {
        const details = catalogPaginationDetails();
        const clampedPage = Math.min(Math.max(1, Number(nextPage) || 1), details.totalPages);
        if (clampedPage === paginationState.page && !paginationState.extra) {
            return;
        }
        paginationState.page = clampedPage;
        paginationState.extra = 0;
        const shouldRender = await reloadCatalogPage();
        if (!shouldRender) {
            return;
        }
        announceStorefrontStatus(`${paginationState.page} 페이지로 이동했습니다.`);
        document.getElementById("catalogGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
        focusCatalogCardAfterRender();
    }

    function renderCatalogPagination(list) {
        if (!elements.catalogPagination) {
            return;
        }
        const details = catalogPaginationDetails(list);
        const start = details.total ? (paginationState.page - 1) * details.effectiveSize + 1 : 0;
        const end = Math.min(details.total, paginationState.page * details.effectiveSize);
        setText(elements.catalogPageProgress, `${paginationState.page} / ${details.totalPages} 페이지`);
        setText(elements.catalogPageRange, `${start}-${end} / 총 ${details.total}개`);
        const pageProducts = currentCatalogPageProducts(list);
        const pageTotalPrice = pageProducts.reduce((sum, product) => sum + Number(product.price || 0), 0);
        setText(elements.catalogPageAveragePrice, formatPrice(pageProducts.length ? Math.round(pageTotalPrice / pageProducts.length) : 0));
        setText(elements.catalogPageTotalStock, `${pageProducts.reduce((sum, product) => sum + Number(product.stock || 0), 0)}개`);
        setText(elements.catalogPageLowStockCount, `${pageProducts.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length}개`);
        setText(elements.catalogPageFeaturedCount, `${pageProducts.filter((product) => Boolean(product.featured)).length}개`);
        setText(elements.catalogPageSelectedCount, `${pageProducts.filter((product) => selectedProductIds.has(Number(product.id))).length}개`);
        if (elements.catalogPageSize) {
            elements.catalogPageSize.value = paginationState.size;
        }
        if (elements.catalogPageSelect) {
            elements.catalogPageSelect.innerHTML = Array.from({ length: details.totalPages }, (_, index) =>
                `<option value="${index + 1}">${index + 1} 페이지</option>`
            ).join("");
            elements.catalogPageSelect.value = String(paginationState.page);
        }
        const isFirst = paginationState.page <= 1;
        const isLast = paginationState.page >= details.totalPages;
        elements.catalogFirstPageButton.disabled = isFirst;
        elements.catalogPreviousPageButton.disabled = isFirst;
        elements.catalogNextPageButton.disabled = isLast;
        elements.catalogLastPageButton.disabled = isLast;
        elements.catalogLoadMoreButton.disabled = requestedCatalogPageSize() >= 48 || end >= details.total;
    }

    function requestedCatalogPageSize() {
        return Math.min(48, Math.max(1, Number(paginationState.size || 12) + Number(paginationState.extra || 0)));
    }

    function normalizeCatalogPagination(value) {
        const size = Math.min(48, Math.max(1, Number(value?.size || requestedCatalogPageSize())));
        const totalElements = Math.max(0, Number(value?.totalElements || 0));
        const totalPages = Math.max(0, Number(value?.totalPages || 0));
        const page = Math.min(Math.max(0, Number(value?.page || 0)), Math.max(0, totalPages - 1));
        return {
            page,
            size,
            totalElements,
            totalPages,
            first: value?.first !== false && page === 0,
            last: value?.last !== false && (totalPages === 0 || page >= totalPages - 1)
        };
    }

    function homeImage(value) {
        const image = homeText(value, 500);
        return /^\/(?!\/)/.test(image) || /^https?:\/\//i.test(image) ? image : PRODUCT_IMAGE_FALLBACK_URL;
    }

    function normalizeHomeOptions(value) {
        if (!Array.isArray(value) || value.length > 100) throw new Error("홈 상품 옵션이 올바르지 않습니다.");
        const ids = new Set();
        const names = new Set();
        return value.map((option) => {
            const id = homeInteger(option?.id, "옵션 번호", 1);
            const name = homeText(option?.name, 100, true);
            if (ids.has(id) || names.has(name)) throw new Error("홈 상품 옵션이 중복되었습니다.");
            ids.add(id);
            names.add(name);
            return { id, name, stock: homeInteger(option.stock, "옵션 재고"), additionalPrice: homeInteger(option.additionalPrice, "옵션 추가 금액") };
        });
    }

    function normalizeHomeProduct(value) {
        const id = homeInteger(value?.id, "상품 번호", 1);
        const price = homeInteger(value.price, "상품 가격");
        const stock = homeInteger(value.stock, "상품 재고");
        const options = normalizeHomeOptions(value.options);
        if (options.length && options.reduce((sum, option) => sum + option.stock, 0) !== stock) throw new Error("상품 재고가 옵션 합계와 일치하지 않습니다.");
        return {
            id,
            brand: homeText(value.brand, 100, true),
            category: homeText(value.category, 100, true),
            name: homeText(value.name, 200, true),
            headline: homeText(value.headline, 200),
            model: homeText(value.model, 100),
            price,
            stock,
            createdDate: homeText(value.createdDate, 30),
            description: homeText(value.description, 2000),
            mood: homeText(value.mood, 100),
            featured: value.featured === true,
            featuredRank: Number.isSafeInteger(value.featuredRank) && value.featuredRank > 0 ? value.featuredRank : null,
            stockStatus: homeText(value.stockStatus, 40) || stockLabel(stock),
            priceLabel: formatPrice(price),
            options,
            thumbnailUrl: homeImage(value.thumbnailUrl)
        };
    }

    function normalizeHomeProducts(value, limit = 48) {
        if (!Array.isArray(value) || value.length > limit) throw new Error("홈 상품 목록이 올바르지 않습니다.");
        const ids = new Set();
        return value.map((item) => {
            const product = normalizeHomeProduct(item);
            if (ids.has(product.id)) throw new Error("홈 상품이 중복되었습니다.");
            ids.add(product.id);
            return product;
        });
    }

    function normalizeHomeCollections(value) {
        if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("홈 컬렉션 응답이 올바르지 않습니다.");
        const collections = {
            recommended: normalizeHomeProducts(value.recommended, 8),
            ranking: normalizeHomeProducts(value.ranking, 8),
            fastDelivery: normalizeHomeProducts(value.fastDelivery, 8),
            latestDrops: normalizeHomeProducts(value.latestDrops, 8),
            lowStock: normalizeHomeProducts(value.lowStock, 8)
        };
        const threshold = lowStockThresholdValue();
        if (collections.recommended.some((product) => !product.featured)
            || collections.fastDelivery.some((product) => product.stock < threshold)
            || collections.lowStock.some((product) => product.stock >= threshold)
            || collections.ranking.some((product, index, list) => index > 0 && list[index - 1].stock < product.stock)
            || collections.latestDrops.some((product, index, list) => index > 0 && list[index - 1].createdDate < product.createdDate)) {
            throw new Error("홈 컬렉션 조건이 올바르지 않습니다.");
        }
        return collections;
    }

    function normalizeHomeFacets(value) {
        if (!Array.isArray(value) || value.length > 500) throw new Error("홈 상품 분류가 올바르지 않습니다.");
        const names = new Set();
        return value.map((item) => {
            const name = homeText(item?.value, 100, true);
            if (names.has(name)) throw new Error("홈 상품 분류가 중복되었습니다.");
            names.add(name);
            return { value: name, count: homeInteger(item.count, "분류 상품 수") };
        });
    }

    function normalizeHomeMetrics(value) {
        const normalized = {};
        ["totalCount", "lowStockCount", "latestDropCount", "featuredCount", "totalStock", "averagePrice", "minimumPrice",
            "maximumPrice", "brandCount", "under200Count", "between200And300Count", "over300Count"]
            .forEach((key) => { normalized[key] = homeInteger(value?.[key], key); });
        normalized.latestCreatedDate = homeText(value?.latestCreatedDate, 30);
        if (normalized.lowStockCount > normalized.totalCount || normalized.featuredCount > normalized.totalCount
            || (normalized.totalCount > 0 && (normalized.minimumPrice > normalized.averagePrice || normalized.averagePrice > normalized.maximumPrice))) {
            throw new Error("홈 상품 집계가 올바르지 않습니다.");
        }
        return normalized;
    }

    function normalizeCatalogPayload(value, includeSummary) {
        if (!value || typeof value !== "object" || Array.isArray(value)) throw new Error("홈 카탈로그 응답이 올바르지 않습니다.");
        const productsValue = normalizeHomeProducts(value.products, requestedCatalogPageSize());
        const page = homeInteger(value.pagination?.page, "현재 페이지");
        const size = homeInteger(value.pagination?.size, "페이지 크기", 1);
        const totalElements = homeInteger(value.pagination?.totalElements, "전체 상품 수");
        const totalPages = homeInteger(value.pagination?.totalPages, "전체 페이지 수");
        if (size !== requestedCatalogPageSize() || totalPages !== (totalElements ? Math.ceil(totalElements / size) : 0)
            || productsValue.length > size || (totalPages ? page >= totalPages : page !== 0)
            || Boolean(value.pagination?.first) !== (page === 0)
            || Boolean(value.pagination?.last) !== (totalPages === 0 || page === totalPages - 1)) throw new Error("홈 상품 페이지가 올바르지 않습니다.");
        const result = { products: productsValue, pagination: { page, size, totalElements, totalPages, first: page === 0, last: totalPages === 0 || page === totalPages - 1 } };
        if (includeSummary) {
            result.metrics = normalizeHomeMetrics(value.metrics);
            result.brandFacets = normalizeHomeFacets(value.brandFacets);
            result.categoryFacets = normalizeHomeFacets(value.categoryFacets);
            if (result.metrics.totalCount !== totalElements || result.metrics.brandCount !== result.brandFacets.length) throw new Error("홈 상품 요약이 페이지와 일치하지 않습니다.");
        }
        return result;
    }

    async function reloadCatalogPage() {
        elements.catalogGrid?.setAttribute("aria-busy", "true");
        syncUrlState();
        const shouldRender = await loadProducts(false);
        if (shouldRender) {
            renderCatalog();
        }
        elements.catalogGrid?.setAttribute("aria-busy", "false");
        return shouldRender;
    }

    function selectedProducts() {
        return products.filter((product) => selectedProductIds.has(Number(product.id)));
    }

    function toggleSelectedProduct(productId) {
        recordSelectionSnapshot("상품 개별 선택 변경");
        if (selectedProductIds.has(productId)) {
            selectedProductIds.delete(productId);
        } else {
            selectedProductIds.add(productId);
        }
        renderCatalog();
    }

    function selectCurrentPageProducts(predicate, label) {
        const matchingProducts = currentCatalogPageProducts().filter(predicate);
        recordSelectionSnapshot(`${label} 상품 선택`);
        selectedProductIds.clear();
        matchingProducts.forEach((product) => selectedProductIds.add(Number(product.id)));
        renderCatalog();
        showToast(`${label} 상품 ${matchingProducts.length}개를 선택했습니다.`, "현재 페이지 조건으로 선택 목록을 다시 구성했습니다.");
    }

    function invertCurrentPageSelection() {
        recordSelectionSnapshot("현재 페이지 선택 반전");
        currentCatalogPageProducts().forEach((product) => {
            const productId = Number(product.id);
            if (selectedProductIds.has(productId)) {
                selectedProductIds.delete(productId);
            } else {
                selectedProductIds.add(productId);
            }
        });
        renderCatalog();
        showToast("현재 페이지 선택을 반전했습니다.", `${selectedProductIds.size}개 상품이 선택되어 있습니다.`);
    }

    function exportCurrentPageCsv() {
        exportProductsCsv(currentCatalogPageProducts(), `grade-stock-page-${paginationState.page}.csv`);
    }

    function exportProductsCsv(rows, fileName) {
        const header = ["ID", "브랜드", "상품명", "모델", "가격", "재고", "재고상태"];
        const csv = [header].concat(rows.map((product) => [
            product.id,
            product.brand,
            product.name,
            product.model,
            Number(product.price || 0),
            Number(product.stock || 0),
            product.stockStatus || stockLabel(product.stock)
        ])).map((row) => row.map(csvCell).join(",")).join("\n");
        downloadTextFile(fileName, `\uFEFF${csv}`, "text/csv;charset=utf-8");
        showToast("상품 CSV를 생성했습니다.", `${rows.length}개 상품을 스프레드시트에서 확인할 수 있습니다.`);
    }

    function csvCell(value) {
        return `"${String(value ?? "").replaceAll("\"", "\"\"")}"`;
    }

    function downloadTextFile(fileName, content, type) {
        const url = URL.createObjectURL(new Blob([content], { type }));
        const link = document.createElement("a");
        link.href = url;
        link.download = fileName;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    function hiddenProductSummary(product) {
        return {
            id: product.id,
            name: product.name,
            headline: product.headline,
            brand: product.brand
        };
    }

    function renderCatalogSelection() {
        if (!elements.catalogSelection) {
            return;
        }
        const selected = selectedProducts();
        const pageProducts = currentCatalogPageProducts();
        elements.catalogSelection.classList.toggle("has-selection", Boolean(selected.length));
        setText(elements.catalogSelectionCount, String(selected.length));
        setText(elements.catalogSelectionTotalPrice, formatPrice(selected.reduce((sum, product) => sum + Number(product.price || 0), 0)));
        setText(elements.catalogSelectionTotalStock, `${selected.reduce((sum, product) => sum + Number(product.stock || 0), 0)}개`);
        const selectedTotalPrice = selected.reduce((sum, product) => sum + Number(product.price || 0), 0);
        const selectedLowStock = selected.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length;
        const selectedSoldOut = selected.filter((product) => Number(product.stock || 0) <= 0).length;
        const filtered = filteredProducts();
        const filteredTotalPrice = filtered.reduce((sum, product) => sum + Number(product.price || 0), 0);
        const filteredTotalStock = filtered.reduce((sum, product) => sum + Number(product.stock || 0), 0);
        const selectedTotalStock = selected.reduce((sum, product) => sum + Number(product.stock || 0), 0);
        setText(elements.catalogSelectionAveragePrice, formatPrice(selected.length ? Math.round(selectedTotalPrice / selected.length) : 0));
        setText(elements.catalogSelectionBrandCount, `${new Set(selected.map((product) => product.brand).filter(Boolean)).size}개`);
        setText(elements.catalogSelectionLowStockCount, `${selectedLowStock}개`);
        setText(elements.catalogSelectionSoldOutCount, `${selectedSoldOut}개`);
        setText(elements.catalogSelectionCoverage, `${filtered.length ? Math.round((selected.length / filtered.length) * 100) : 0}%`);
        setText(elements.catalogSelectionPriceShare, `${filteredTotalPrice ? Math.round((selectedTotalPrice / filteredTotalPrice) * 100) : 0}%`);
        setText(elements.catalogSelectionStockShare, `${filteredTotalStock ? Math.round((selectedTotalStock / filteredTotalStock) * 100) : 0}%`);
        setText(elements.catalogSelectionAvailableCount, `${selected.length - selectedSoldOut}개`);
        setText(elements.catalogSelectionDominantCategory, dominantCategory(selected) || "-");
        renderSelectionHistory();
        persistSelectedProductIds();
        elements.selectVisibleProductsButton.disabled = pageProducts.length === 0;
        if (elements.selectLowStockPageButton) {
            elements.selectLowStockPageButton.disabled = !pageProducts.some((product) => Number(product.stock || 0) < lowStockThresholdValue());
        }
        if (elements.selectFeaturedPageButton) {
            elements.selectFeaturedPageButton.disabled = !pageProducts.some((product) => Boolean(product.featured));
        }
        if (elements.selectAvailablePageButton) {
            elements.selectAvailablePageButton.disabled = !pageProducts.some((product) => Number(product.stock || 0) > 0);
        }
        if (elements.selectStablePageButton) {
            elements.selectStablePageButton.disabled = !pageProducts.some((product) => Number(product.stock || 0) >= lowStockThresholdValue());
        }
        if (elements.selectPremiumPageButton) {
            elements.selectPremiumPageButton.disabled = !pageProducts.some((product) => Number(product.price || 0) > 300000);
        }
        if (elements.selectDominantBrandPageButton) {
            elements.selectDominantBrandPageButton.disabled = pageProducts.length === 0;
        }
        [elements.invertPageSelectionButton, elements.copyCurrentPageSummaryButton, elements.exportCurrentPageCsvButton]
            .forEach((button) => {
                if (button) {
                    button.disabled = pageProducts.length === 0;
                }
            });
        [
            elements.clearSelectedProductsButton,
            elements.compareSelectedProductsButton,
            elements.bookmarkSelectedProductsButton,
            elements.hideSelectedProductsButton,
            elements.copySelectedSummaryButton,
            elements.copySelectedLinksButton,
            elements.openUrgentSelectedButton,
            elements.focusSelectedBrandButton,
            elements.exportSelectedProductsCsvButton
        ].forEach((button) => {
            if (button) {
                button.disabled = selected.length === 0;
            }
        });
        if (elements.removeSoldOutSelectionButton) {
            elements.removeSoldOutSelectionButton.disabled = selectedSoldOut === 0;
        }
        if (elements.removeLowStockSelectionButton) {
            elements.removeLowStockSelectionButton.disabled = selectedLowStock === 0;
        }
        [elements.selectCheapestPageButton, elements.selectHighestStockPageButton]
            .forEach((button) => {
                if (button) {
                    button.disabled = pageProducts.length === 0;
                }
            });
        if (elements.selectSoldOutPageButton) {
            elements.selectSoldOutPageButton.disabled = !pageProducts.some((product) => Number(product.stock || 0) <= 0);
        }
        [elements.openCheapestSelectedButton, elements.openHighestStockSelectedButton]
            .forEach((button) => {
                if (button) {
                    button.disabled = selected.length === 0;
                }
            });
        if (!selected.length) {
            setText(elements.catalogSelectionTitle, "선택한 상품이 없습니다.");
            setText(elements.catalogSelectionText, "카드에서 선택하거나 현재 상품 전체 선택을 눌러 일괄 작업을 시작할 수 있습니다.");
            return;
        }
        const totalPrice = selected.reduce((sum, product) => sum + Number(product.price || 0), 0);
        const lowStockCount = selected.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length;
        setText(elements.catalogSelectionTitle, `${selected.length}개 상품을 선택했습니다.`);
        setText(elements.catalogSelectionText, `합계 ${formatPrice(totalPrice)} · 긴장 재고 ${lowStockCount}개 · 대표 브랜드 ${dominantBrand(selected) || "-"}`);
    }

    function selectRankedPageProducts(metric, limit) {
        const ranked = currentCatalogPageProducts().slice().sort(metric === "PRICE_LOW"
            ? (left, right) => Number(left.price || 0) - Number(right.price || 0)
            : (left, right) => Number(right.stock || 0) - Number(left.stock || 0));
        recordSelectionSnapshot(metric === "PRICE_LOW" ? "최저가 상품 선택" : "최고재고 상품 선택");
        ranked.slice(0, limit).forEach((product) => selectedProductIds.add(Number(product.id)));
        renderCatalog();
        showToast("우선순위 상품을 선택했습니다.", `${Math.min(limit, ranked.length)}개 상품을 선택 목록에 반영했습니다.`);
    }

    function recordSelectionSnapshot(label) {
        selectionHistory.push({ ids: Array.from(selectedProductIds), label });
        if (selectionHistory.length > 20) {
            selectionHistory.shift();
        }
        selectionFuture.length = 0;
    }

    function restoreSelectionSnapshot(snapshot) {
        selectedProductIds.clear();
        snapshot.ids.forEach((id) => selectedProductIds.add(Number(id)));
        renderCatalog();
    }

    function undoCatalogSelection() {
        const snapshot = selectionHistory.pop();
        if (!snapshot) {
            return;
        }
        selectionFuture.push({ ids: Array.from(selectedProductIds), label: snapshot.label });
        restoreSelectionSnapshot(snapshot);
        showToast("선택 변경을 취소했습니다.", `${snapshot.label} 이전 상태로 돌아갔습니다.`);
    }

    function redoCatalogSelection() {
        const snapshot = selectionFuture.pop();
        if (!snapshot) {
            return;
        }
        selectionHistory.push({ ids: Array.from(selectedProductIds), label: snapshot.label });
        restoreSelectionSnapshot(snapshot);
        showToast("선택 변경을 다시 실행했습니다.", snapshot.label);
    }

    function renderSelectionHistory() {
        const latest = selectionHistory[selectionHistory.length - 1];
        setText(elements.catalogSelectionHistoryCount, `${selectionHistory.length}단계`);
        setText(elements.catalogSelectionHistoryText, latest ? `최근 작업: ${latest.label}` : "아직 변경 이력이 없습니다.");
        elements.undoCatalogSelectionButton?.toggleAttribute("disabled", selectionHistory.length === 0);
        elements.redoCatalogSelectionButton?.toggleAttribute("disabled", selectionFuture.length === 0);
        elements.clearCatalogSelectionHistoryButton?.toggleAttribute("disabled", selectionHistory.length === 0 && selectionFuture.length === 0);
    }

    function openSelectedProductByMetric(metric) {
        const product = catalogDecisionProduct(selectedProducts(), metric);
        if (product) {
            openDrawer(product.id);
        }
    }

    function openCompareProductByMetric(metric) {
        const comparedProducts = readCompareProducts();
        const target = comparedProducts.slice().sort((left, right) => {
            if (metric === "STOCK_HIGH") {
                return Number(right.stock || 0) - Number(left.stock || 0);
            }
            if (metric === "STOCK_LOW") {
                return Number(left.stock || 0) - Number(right.stock || 0);
            }
            return Number(right.price || 0) - Number(left.price || 0);
        })[0];
        if (target) {
            openDrawer(target.id);
        }
    }

    function persistSelectedProductIds() {
        try {
            window.sessionStorage.setItem(SELECTED_PRODUCTS_SESSION_KEY, JSON.stringify(Array.from(selectedProductIds)));
        } catch (error) {
            // 세션 저장이 제한된 환경에서도 현재 선택 작업은 유지한다.
        }
    }

    function restoreSelectedProductIds() {
        try {
            const savedIds = JSON.parse(window.sessionStorage.getItem(SELECTED_PRODUCTS_SESSION_KEY) || "[]");
            const validIds = new Set(products.map((product) => Number(product.id)));
            (Array.isArray(savedIds) ? savedIds.slice(0, 48) : []).forEach((id) => {
                const productId = Number(id);
                if (Number.isSafeInteger(productId) && productId > 0 && validIds.has(productId)) {
                    selectedProductIds.add(productId);
                }
            });
        } catch (error) {
            selectedProductIds.clear();
        }
    }

    function renderCatalogSummary(list) {
        const totalCount = Number(metrics.totalCount || 0);
        setText(elements.catalogCountText, `${totalCount}개 상품이 현재 조건에 맞습니다.`);
        setText(elements.catalogSummaryText, buildSummaryText(totalCount));
        renderCatalogSearchQuality(list);
        const averagePrice = Number(metrics.averagePrice || 0);
        const priceRange = totalCount
            ? `${formatPrice(metrics.minimumPrice)} - ${formatPrice(metrics.maximumPrice)}`
            : "0원";
        setText(elements.catalogAveragePrice, formatPrice(averagePrice));
        setText(elements.catalogPriceRange, priceRange);
        setText(elements.catalogTotalStock, `${Number(metrics.totalStock || 0)}개`);
        setText(elements.catalogBrandCount, `${Number(metrics.brandCount || 0)}개`);
        setText(elements.catalogLowStockCount, `${Number(metrics.lowStockCount || 0)}개`);
        const distributionItems = [
            [elements.catalogUnderPriceRate, elements.catalogUnderPriceBar, Number(metrics.under200Count || 0)],
            [elements.catalogMiddlePriceRate, elements.catalogMiddlePriceBar, Number(metrics.between200And300Count || 0)],
            [elements.catalogHighPriceRate, elements.catalogHighPriceBar, Number(metrics.over300Count || 0)],
            [elements.catalogLowStockRate, elements.catalogLowStockBar, Number(metrics.lowStockCount || 0)],
            [elements.catalogFeaturedRate, elements.catalogFeaturedBar, Number(metrics.featuredCount || 0)]
        ];
        distributionItems.forEach(([label, bar, count]) => {
            const rate = totalCount ? Math.round((count / totalCount) * 100) : 0;
            setText(label, `${rate}%`);
            if (bar) {
                bar.style.width = `${rate}%`;
            }
        });
        const cheapest = catalogDecisionProduct(list, "PRICE_LOW");
        const highestStock = catalogDecisionProduct(list, "STOCK_HIGH");
        const urgent = catalogDecisionProduct(list, "STOCK_LOW");
        setText(elements.catalogCheapestLabel, cheapest ? `${cheapest.brand} · ${formatPrice(cheapest.price)}` : "-");
        setText(elements.catalogHighestStockLabel, highestStock ? `${highestStock.brand} · ${highestStock.stock}개` : "-");
        setText(elements.catalogUrgentLabel, urgent ? `${urgent.brand} · ${urgent.stock}개` : "-");
        setText(elements.catalogDominantBrandLabel, dominantBrand(list) || "-");
        [elements.openCheapestCatalogButton, elements.openHighestStockCatalogButton, elements.openUrgentCatalogButton, elements.focusDominantCatalogBrandButton, elements.copyCatalogDecisionButton]
            .forEach((button) => {
                if (button) {
                    button.disabled = list.length === 0;
                }
            });
        if (!elements.catalogTags) {
            return;
        }

        const tags = [];
        if (state.brand !== "ALL") {
            tags.push({ label: `브랜드 ${state.brand}`, key: "brand" });
        }
        if (state.category !== "ALL") {
            tags.push({ label: `카테고리 ${state.category}`, key: "category" });
        }
        if (state.stock === "LOW") {
            tags.push({ label: `품절 임박 ${state.lowStockThreshold}개 미만`, key: "stock" });
        }
        if (state.stock === "STABLE") {
            tags.push({ label: `재고 안정 ${state.lowStockThreshold}개 이상`, key: "stock" });
        }
        if (state.featuredOnly === "FEATURED") {
            tags.push({ label: "Featured만", key: "featuredOnly" });
        }
        if (state.priceBand === "UNDER_200") {
            tags.push({ label: "20만원 미만", key: "priceBand" });
        }
        if (state.priceBand === "BETWEEN_200_300") {
            tags.push({ label: "20만원-30만원", key: "priceBand" });
        }
        if (state.priceBand === "OVER_300") {
            tags.push({ label: "30만원 초과", key: "priceBand" });
        }
        if (state.search) {
            tags.push({ label: `검색 ${state.search}`, key: "search" });
        }
        if (state.sort === "FEATURED") {
            tags.push({ label: "대표 노출순", key: "sort" });
        }
        if (state.sort === "NAME_ASC") {
            tags.push({ label: "상품명 오름차순", key: "sort" });
        }
        if (state.sort === "PRICE_HIGH") {
            tags.push({ label: "발매가 높은 순", key: "sort" });
        }
        if (state.sort === "PRICE_LOW") {
            tags.push({ label: "발매가 낮은 순", key: "sort" });
        }
        if (state.sort === "STOCK_ASC") {
            tags.push({ label: "재고 낮은 순", key: "sort" });
        }
        if (state.sort === "STOCK_DESC") {
            tags.push({ label: "재고 높은 순", key: "sort" });
        }
        if (!tags.length) {
            tags.push({ label: "전체 탐색", key: "" });
        }

        elements.catalogTags.innerHTML = tags.map((tag) => tag.key
            ? `
                <button class="catalog-tag catalog-tag--interactive" type="button" data-filter-remove="${tag.key}">
                    <span>${tag.label}</span>
                    <span class="catalog-tag__remove" aria-hidden="true">×</span>
                </button>
            `
            : `<span class="catalog-tag">${tag.label}</span>`).join("");
    }

    function renderCatalogSearchQuality(list) {
        const keyword = String(state.search || "").trim().toLocaleLowerCase("ko");
        elements.catalogSearchQuality?.toggleAttribute("hidden", !keyword);
        if (!keyword) {
            return;
        }
        const includesKeyword = (value) => String(value || "").toLocaleLowerCase("ko").includes(keyword);
        const additionalFilterCount = Object.entries(state)
            .filter(([key, value]) => key !== "search" && value !== DEFAULT_STATE[key])
            .length;
        setText(elements.catalogSearchNameMatch, `${list.filter((product) => includesKeyword(product.name) || includesKeyword(product.headline)).length}개`);
        setText(elements.catalogSearchBrandMatch, `${list.filter((product) => includesKeyword(product.brand)).length}개`);
        setText(elements.catalogSearchModelMatch, `${list.filter((product) => includesKeyword(product.model)).length}개`);
        setText(elements.catalogSearchAvailableMatch, `${list.filter((product) => Number(product.stock || 0) > 0).length}개`);
        setText(elements.catalogSearchFilterCount, `${additionalFilterCount}개`);
    }

    function catalogDecisionProduct(list, metric) {
        const next = list.slice();
        if (metric === "PRICE_LOW") {
            return next.sort((left, right) => Number(left.price || 0) - Number(right.price || 0))[0];
        }
        return next.sort((left, right) => metric === "STOCK_HIGH"
            ? Number(right.stock || 0) - Number(left.stock || 0)
            : Number(left.stock || 0) - Number(right.stock || 0))[0];
    }

    function openCatalogDecisionProduct(metric) {
        const product = catalogDecisionProduct(filteredProducts(), metric);
        if (product) {
            openDrawer(product.id);
        }
    }

    function catalogDecisionSummary(list) {
        const cheapest = catalogDecisionProduct(list, "PRICE_LOW");
        const highestStock = catalogDecisionProduct(list, "STOCK_HIGH");
        const urgent = catalogDecisionProduct(list, "STOCK_LOW");
        return [
            `카탈로그 판단 요약 · ${list.length}개`,
            `최저가 ${cheapest?.headline || cheapest?.name || "-"} · ${cheapest ? formatPrice(cheapest.price) : "-"}`,
            `최고 재고 ${highestStock?.headline || highestStock?.name || "-"} · ${highestStock?.stock || 0}개`,
            `긴급 확인 ${urgent?.headline || urgent?.name || "-"} · ${urgent?.stock || 0}개`,
            `대표 브랜드 ${dominantBrand(list) || "-"}`
        ].join("\n");
    }

    function renderCatalogInsights() {
        if (!elements.catalogInsightGrid) {
            return;
        }
        const list = filteredProducts();
        const featuredCount = list.filter((product) => product.featured).length;
        const lowStockCount = list.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length;
        const averagePrice = list.length
            ? Math.round(list.reduce((sum, product) => sum + Number(product.price || 0), 0) / list.length)
            : 0;
        const leadBrand = dominantBrand(list);

        const insights = [
            {
                label: "Visible now",
                value: `${list.length}개`,
                description: "현재 조건으로 즉시 확인 가능한 상품 수입니다.",
                action: "SCROLL_CATALOG"
            },
            {
                label: "Featured mix",
                value: `${featuredCount}개`,
                description: "대표 노출에 걸린 상품 밀도를 빠르게 파악합니다.",
                action: "FEATURED"
            },
            {
                label: "Low stock",
                value: `${lowStockCount}개`,
                description: "긴장 재고 구간 상품 수를 같은 화면에서 추적합니다.",
                action: "LOW_STOCK"
            },
            {
                label: "Lead brand",
                value: leadBrand || "-",
                description: list.length ? `평균 발매가 ${formatPrice(averagePrice)} 기준으로 흐름을 읽을 수 있습니다.` : "상품을 불러오면 조건별 흐름을 요약합니다.",
                action: leadBrand ? "LEAD_BRAND" : "SCROLL_CATALOG"
            }
        ];

        elements.catalogInsightGrid.innerHTML = insights.map((item) => `
            <button class="catalog-insight-card" type="button" data-insight-action="${item.action}">
                <span>${item.label}</span>
                <strong>${item.value}</strong>
                <p>${item.description}</p>
            </button>
        `).join("");
        bindInsightButtons();
    }

    function filteredProducts() {
        const hiddenIds = new Set(readHiddenProducts().map((product) => Number(product.id)));
        return products.filter((product) => {
            if (!uiState.showHiddenProducts && hiddenIds.has(Number(product.id))) {
                return false;
            }
            if (uiState.todayOnly && metrics.latestCreatedDate && product.createdDate !== metrics.latestCreatedDate) {
                return false;
            }
            return true;
        });
    }

    async function refreshCatalog() {
        paginationState.page = 1;
        paginationState.extra = 0;
        elements.catalogGrid?.setAttribute("aria-busy", "true");
        announceStorefrontStatus("상품 목록을 갱신하고 있습니다.");
        syncUrlState();
        persistLastCatalogState();
        persistSearchHistory();
        const shouldRender = await loadProducts();
        if (!shouldRender) {
            return;
        }
        populateFilters();
        renderHeroMetrics();
        renderFlowBoard();
        renderBrandSpotlight();
        renderCategoryShortcuts();
        renderSignalStrip();
        renderFeatured();
        renderSavedViews();
        renderSearchHistory();
        renderHiddenProducts();
        renderCatalogInsights();
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderSignals();
        renderCatalog();
        syncViewButtons();
        elements.catalogGrid?.setAttribute("aria-busy", "false");
        announceStorefrontStatus(catalogLoadError || `${Number(metrics.totalCount || 0)}개 상품 중 ${filteredProducts().length}개를 표시했습니다.`);
        syncNetworkStatus();
    }

    function syncNetworkStatus() {
        const isOffline = !window.navigator.onLine;
        if (elements.networkStatus) {
            elements.networkStatus.hidden = networkStatusDismissed || (!isOffline && !catalogLoadError);
        }
        setText(elements.networkStatusText, isOffline ? "오프라인 상태입니다. 저장된 화면은 계속 볼 수 있습니다." : catalogLoadError || "연결이 복구되었습니다.");
    }

    async function handleNetworkReconnect() {
        networkStatusDismissed = false;
        syncNetworkStatus();
        if (!window.navigator.onLine) {
            announceStorefrontStatus("아직 네트워크에 연결되지 않았습니다.");
            return;
        }
        if (catalogLoadError) {
            await refreshCatalog();
        }
        syncNetworkStatus();
    }

    function handleNetworkOffline() {
        networkStatusDismissed = false;
        syncNetworkStatus();
    }

    function openShortcutHelp() {
        if (!elements.shortcutHelpModal) {
            return;
        }
        closeDrawer();
        closeHeaderSearch();
        closeMobileMenu();
        shortcutHelpReturnFocus = document.activeElement;
        elements.shortcutHelpModal.classList.add("is-open");
        elements.shortcutHelpModal.setAttribute("aria-hidden", "false");
        setShortcutHelpBackgroundInert(true);
        syncBodyLayerState();
        elements.shortcutHelpCloseButton?.focus();
    }

    function closeShortcutHelp() {
        if (!elements.shortcutHelpModal?.classList.contains("is-open")) {
            return;
        }
        elements.shortcutHelpModal.classList.remove("is-open");
        elements.shortcutHelpModal.setAttribute("aria-hidden", "true");
        setShortcutHelpBackgroundInert(false);
        syncBodyLayerState();
        if (shortcutHelpReturnFocus?.isConnected) {
            shortcutHelpReturnFocus.focus();
        }
        shortcutHelpReturnFocus = null;
    }

    function setShortcutHelpBackgroundInert(isInert) {
        [document.querySelector(".page-shell"), elements.mobileStoreNav, elements.scrollTopButton]
            .filter(Boolean)
            .forEach((element) => {
                element.inert = isInert;
            });
    }

    function keepFocusInsideShortcutHelp(event) {
        const focusable = Array.from(elements.shortcutHelpModal.querySelectorAll(
            'button:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter((element) => element.getClientRects().length);
        if (!focusable.length) {
            event.preventDefault();
            elements.shortcutHelpModal.querySelector(".shortcut-help-modal__panel")?.focus();
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

    function syncBodyLayerState() {
        const hasOpenModal = Boolean(
            elements.productDrawer?.classList.contains("is-open")
            || elements.shortcutHelpModal?.classList.contains("is-open")
        );
        document.body.classList.toggle("has-open-modal", hasOpenModal);
    }

    function handleVisibilityChange() {
        if (document.hidden) {
            stopHeroCarousel();
            return;
        }
        startHeroCarousel();
    }

    function hydrateStateFromUrl() {
        const params = new URLSearchParams(window.location.search);
        state.search = (params.get("search") || DEFAULT_STATE.search).trim().toLowerCase();
        state.brand = params.get("brand") || DEFAULT_STATE.brand;
        state.category = params.get("category") || DEFAULT_STATE.category;
        state.stock = normalizeStateValue(params.get("stock"), ["ALL", "LOW", "STABLE"], DEFAULT_STATE.stock);
        state.sort = normalizeStateValue(
            params.get("sort"),
            ["LATEST", "NAME_ASC", "PRICE_HIGH", "PRICE_LOW", "STOCK_ASC", "STOCK_DESC", "FEATURED"],
            DEFAULT_STATE.sort
        );
        state.lowStockThreshold = normalizeStateValue(
            params.get("lowStockThreshold"),
            ["10", "20", "30", "50"],
            DEFAULT_STATE.lowStockThreshold
        );
        state.featuredOnly = normalizeStateValue(params.get("featuredOnly"), ["ALL", "FEATURED"], DEFAULT_STATE.featuredOnly);
        state.priceBand = normalizeStateValue(
            params.get("priceBand"),
            ["ALL", "UNDER_200", "BETWEEN_200_300", "OVER_300"],
            DEFAULT_STATE.priceBand
        );
        paginationState.page = Math.max(1, Number(params.get("page") || 1));
        const requestedSize = params.get("size");
        if (["6", "12", "24", "48"].includes(requestedSize)) {
            paginationState.size = requestedSize;
        }
    }

    function syncUrlState() {
        const params = new URLSearchParams();
        Object.entries(state).forEach(([key, value]) => {
            if (value && value !== DEFAULT_STATE[key]) {
                params.set(key, value);
            }
        });
        if (paginationState.page > 1) {
            params.set("page", paginationState.page);
        }
        if (paginationState.size !== "12") {
            params.set("size", paginationState.size);
        }
        const nextUrl = params.toString() ? `${window.location.pathname}?${params}` : window.location.pathname;
        window.history.replaceState({}, "", nextUrl);
    }

    function catalogUrlForSnapshot(snapshot) {
        const params = new URLSearchParams();
        Object.entries({ ...DEFAULT_STATE, ...snapshot }).forEach(([key, value]) => {
            if (value && value !== DEFAULT_STATE[key]) {
                params.set(key, value);
            }
        });
        const query = params.toString();
        return `${window.location.origin}${window.location.pathname}${query ? `?${query}` : ""}`;
    }

    function syncControls() {
        if (elements.searchInput) {
            elements.searchInput.value = state.search;
        }
        if (elements.brandFilter) {
            elements.brandFilter.value = state.brand;
        }
        if (elements.categoryFilter) {
            elements.categoryFilter.value = state.category;
        }
        if (elements.stockFilter) {
            elements.stockFilter.value = state.stock;
        }
        if (elements.featuredOnlyFilter) {
            elements.featuredOnlyFilter.value = state.featuredOnly;
        }
        if (elements.priceBandFilter) {
            elements.priceBandFilter.value = state.priceBand;
        }
        if (elements.sortFilter) {
            elements.sortFilter.value = state.sort;
        }
        if (elements.lowStockThresholdFilter) {
            elements.lowStockThresholdFilter.value = state.lowStockThreshold;
        }
        syncFilterFieldStates();
    }

    function syncFilterFieldStates() {
        const activeStates = new Map([
            [elements.searchInput, Boolean(state.search)],
            [elements.brandFilter, state.brand !== DEFAULT_STATE.brand],
            [elements.categoryFilter, state.category !== DEFAULT_STATE.category],
            [elements.stockFilter, state.stock !== DEFAULT_STATE.stock],
            [elements.featuredOnlyFilter, state.featuredOnly !== DEFAULT_STATE.featuredOnly],
            [elements.priceBandFilter, state.priceBand !== DEFAULT_STATE.priceBand],
            [elements.lowStockThresholdFilter, state.stock !== DEFAULT_STATE.stock && state.lowStockThreshold !== DEFAULT_STATE.lowStockThreshold],
            [elements.sortFilter, state.sort !== DEFAULT_STATE.sort]
        ]);
        activeStates.forEach((isActive, control) => {
            control?.closest(".toolbar-field")?.classList.toggle("has-value", isActive);
        });
        const activeFilterCount = [
            state.brand !== DEFAULT_STATE.brand,
            state.category !== DEFAULT_STATE.category,
            state.stock !== DEFAULT_STATE.stock,
            state.featuredOnly !== DEFAULT_STATE.featuredOnly,
            state.priceBand !== DEFAULT_STATE.priceBand,
            state.stock !== DEFAULT_STATE.stock && state.lowStockThreshold !== DEFAULT_STATE.lowStockThreshold
        ].filter(Boolean).length;
        setText(elements.catalogFilterCount, String(activeFilterCount));
        elements.catalogFilterPanel?.classList.toggle("has-active-filter", activeFilterCount > 0);
        elements.catalogFilterPanel?.querySelector("summary")?.setAttribute(
            "aria-label",
            activeFilterCount ? `필터 ${activeFilterCount}개 적용됨` : "필터 열기"
        );
    }

    function removeFilter(key) {
        if (!key || !(key in DEFAULT_STATE)) {
            return;
        }
        state[key] = DEFAULT_STATE[key];
        if (key === "stock") {
            state.lowStockThreshold = DEFAULT_STATE.lowStockThreshold;
        }
        if (key === "featuredOnly" && state.sort === "FEATURED") {
            state.sort = DEFAULT_STATE.sort;
        }
        syncControls();
        refreshCatalog();
    }

    function resetState() {
        Object.assign(state, DEFAULT_STATE);
    }

    function readDisplayPreferences() {
        try {
            const parsed = JSON.parse(readStorageValue(window.localStorage, DISPLAY_PREFERENCES_KEY) || "{}");
            if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return {};
            const layout = ["SHOP", "STANDARD", "COMFORT", "LIST"].includes(parsed.layout) ? parsed.layout : "";
            return Object.fromEntries([
                ["layout", layout],
                ...["hideDescriptions", "hideSignals", "hideActions", "hideMetrics", "hideDistribution", "hideDecision", "reducedMotion"]
                    .map((key) => [key, parsed[key] === true])
            ]);
        } catch (error) {
            return {};
        }
    }

    function persistDisplayPreferences() {
        safeStorageWrite(window.localStorage, DISPLAY_PREFERENCES_KEY, JSON.stringify({
            layout: uiState.layout,
            hideDescriptions: uiState.hideDescriptions,
            hideSignals: uiState.hideSignals,
            hideActions: uiState.hideActions,
            hideMetrics: uiState.hideMetrics,
            hideDistribution: uiState.hideDistribution,
            hideDecision: uiState.hideDecision,
            reducedMotion: uiState.reducedMotion
        }));
        safeStorageWrite(window.localStorage, VIEW_MODE_KEY, uiState.layout === "COMFORT" ? "COMPACT" : "DEFAULT");
    }

    function setCatalogLayout(layout) {
        uiState.layout = layout;
        uiState.viewMode = layout === "COMFORT" ? "COMPACT" : "DEFAULT";
        persistDisplayPreferences();
        renderCatalog();
        syncViewButtons();
    }

    function toggleCatalogPreference(key, title) {
        uiState[key] = !uiState[key];
        persistDisplayPreferences();
        renderCatalog();
        syncViewButtons();
        showToast(title, uiState[key] ? "간결한 상품 탐색 화면으로 전환했습니다." : "전체 상품 정보를 다시 표시합니다.");
    }

    function applyCatalogDisplayClasses() {
        if (!elements.catalogGrid) {
            return;
        }
        ["SHOP", "STANDARD", "COMFORT", "LIST"].forEach((layout) => {
            elements.catalogGrid.classList.toggle(`is-layout-${layout.toLowerCase()}`, uiState.layout === layout);
        });
        elements.catalogGrid.classList.toggle("is-compact", uiState.layout === "COMFORT");
        elements.catalogGrid.classList.toggle("is-description-hidden", uiState.hideDescriptions);
        elements.catalogGrid.classList.toggle("is-signal-hidden", uiState.hideSignals);
        elements.catalogGrid.classList.toggle("is-action-hidden", uiState.hideActions);
        elements.catalogLiveMetrics?.toggleAttribute("hidden", uiState.hideMetrics);
        elements.catalogDistribution?.toggleAttribute("hidden", uiState.hideDistribution);
        elements.catalogDecisionRail?.toggleAttribute("hidden", uiState.hideDecision);
        document.body.classList.toggle("is-reduced-motion", uiState.reducedMotion);
    }

    function saveCurrentView() {
        const summary = viewSummaryLabel();
        const current = readSavedViews().filter((item) => item.summary !== summary);
        const next = [{
            summary,
            snapshot: { ...state }
        }].concat(current).slice(0, 6);
        window.localStorage.setItem(SAVED_VIEWS_KEY, JSON.stringify(next));
        renderSavedViews();
        showToast("현재 탐색 조건을 저장했습니다.", `${summary} 조건으로 다시 돌아올 수 있습니다.`);
    }

    function renderSavedViews() {
        if (!elements.savedViewList) {
            return;
        }
        const savedViews = readSavedViews();
        setText(elements.savedViewCount, String(savedViews.length));
        [elements.applyLatestSavedViewButton, elements.applyOldestSavedViewButton, elements.removeLatestSavedViewButton, elements.removeOldestSavedViewButton, elements.exportSavedViewsButton]
            .forEach((button) => button?.toggleAttribute("disabled", savedViews.length === 0));
        if (!savedViews.length) {
            elements.savedViewList.innerHTML = `<span class="catalog-memory-empty">저장된 탐색이 없습니다.</span>`;
            return;
        }
        const visibleViews = savedViews.map((item, originalIndex) => ({ item, originalIndex }));
        if (memoryState.savedReversed) {
            visibleViews.reverse();
        }
        elements.savedViewList.innerHTML = visibleViews.map(({ item, originalIndex }) => `
            <div class="catalog-memory-item">
                <button class="catalog-memory-item__primary" type="button" data-saved-view-index="${originalIndex}">${escapeMarkup(item.summary)}</button>
                <button type="button" data-copy-saved-view-index="${originalIndex}" aria-label="저장 탐색 링크 복사">링크</button>
                <button type="button" data-remove-saved-view-index="${originalIndex}" aria-label="저장 탐색 삭제">×</button>
            </div>
        `).join("");
        elements.savedViewList.querySelectorAll("[data-saved-view-index]").forEach((button) => {
            button.addEventListener("click", async () => {
                const selected = readSavedViews()[Number(button.dataset.savedViewIndex)];
                if (!selected?.snapshot) {
                    return;
                }
                Object.assign(state, DEFAULT_STATE, selected.snapshot);
                syncControls();
                await refreshCatalog();
                showToast("저장한 탐색을 불러왔습니다.", `${selected.summary} 조건으로 다시 탐색합니다.`);
            });
        });
        elements.savedViewList.querySelectorAll("[data-remove-saved-view-index]").forEach((button) => {
            button.addEventListener("click", () => {
                const next = readSavedViews().filter((_, index) => index !== Number(button.dataset.removeSavedViewIndex));
                window.localStorage.setItem(SAVED_VIEWS_KEY, JSON.stringify(next));
                renderSavedViews();
                showToast("저장한 탐색을 삭제했습니다.", "더 이상 목록에 노출되지 않습니다.");
            });
        });
        elements.savedViewList.querySelectorAll("[data-copy-saved-view-index]").forEach((button) => {
            button.addEventListener("click", async () => {
                const selected = readSavedViews()[Number(button.dataset.copySavedViewIndex)];
                if (!selected?.snapshot) {
                    return;
                }
                await copyTextWithFeedback(catalogUrlForSnapshot(selected.snapshot), "저장 탐색 링크를 복사했습니다.", `${selected.summary} 조건을 그대로 공유할 수 있습니다.`);
            });
        });
    }

    async function applySavedViewEdge(position) {
        const savedViews = readSavedViews();
        const selected = position === "LATEST" ? savedViews[0] : savedViews[savedViews.length - 1];
        if (!selected?.snapshot) {
            return;
        }
        Object.assign(state, DEFAULT_STATE, selected.snapshot);
        syncControls();
        await refreshCatalog();
        showToast(position === "LATEST" ? "최신 저장 조건을 적용했습니다." : "최초 저장 조건을 적용했습니다.", selected.summary);
    }

    function removeSavedViewEdge(position) {
        const savedViews = readSavedViews();
        if (!savedViews.length) {
            return;
        }
        const next = position === "LATEST" ? savedViews.slice(1) : savedViews.slice(0, -1);
        window.localStorage.setItem(SAVED_VIEWS_KEY, JSON.stringify(next));
        renderSavedViews();
        showToast(position === "LATEST" ? "최신 저장 탐색을 삭제했습니다." : "최초 저장 탐색을 삭제했습니다.", `${next.length}개 조건이 남았습니다.`);
    }

    function renderSearchHistory() {
        if (!elements.searchHistoryList) {
            return;
        }
        const history = readSearchHistory();
        setText(elements.searchHistoryCount, String(history.length));
        [elements.applyLatestSearchButton, elements.applyOldestSearchButton, elements.removeLatestSearchButton, elements.removeOldestSearchButton, elements.exportSearchHistoryButton]
            .forEach((button) => button?.toggleAttribute("disabled", history.length === 0));
        if (!history.length) {
            elements.searchHistoryList.innerHTML = `<span class="catalog-memory-empty">최근 검색이 없습니다.</span>`;
            return;
        }
        const visibleHistory = memoryState.searchAlphabetical ? history.slice().sort((left, right) => left.localeCompare(right, "ko")) : history;
        elements.searchHistoryList.innerHTML = visibleHistory.map((keyword) => `
            <div class="catalog-memory-item">
                <button class="catalog-memory-item__primary" type="button" data-history-keyword="${escapeMarkup(keyword)}">${escapeMarkup(keyword)}</button>
                <button type="button" data-remove-history-keyword="${escapeMarkup(keyword)}" aria-label="검색 기록 삭제">×</button>
            </div>
        `).join("");
        elements.searchHistoryList.querySelectorAll("[data-history-keyword]").forEach((button) => {
            button.addEventListener("click", async () => {
                state.search = button.dataset.historyKeyword || "";
                syncControls();
                await refreshCatalog();
                showToast("최근 검색어를 적용했습니다.", `${state.search} 기준으로 다시 탐색합니다.`);
            });
        });
        elements.searchHistoryList.querySelectorAll("[data-remove-history-keyword]").forEach((button) => {
            button.addEventListener("click", () => {
                const next = readSearchHistory().filter((keyword) => keyword !== button.dataset.removeHistoryKeyword);
                window.localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
                renderSearchHistory();
                showToast("검색 기록을 삭제했습니다.", "선택한 키워드만 목록에서 제거했습니다.");
            });
        });
    }

    async function applySearchHistoryEdge(position) {
        const history = readSearchHistory();
        const keyword = position === "LATEST" ? history[0] : history[history.length - 1];
        if (!keyword) {
            return;
        }
        state.search = keyword;
        syncControls();
        await refreshCatalog();
        showToast(position === "LATEST" ? "최신 검색어를 적용했습니다." : "최초 검색어를 적용했습니다.", `${keyword} 기준으로 다시 탐색합니다.`);
    }

    function removeSearchHistoryEdge(position) {
        const history = readSearchHistory();
        if (!history.length) {
            return;
        }
        const next = position === "LATEST" ? history.slice(1) : history.slice(0, -1);
        window.localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
        renderSearchHistory();
        showToast(position === "LATEST" ? "최신 검색 기록을 삭제했습니다." : "최초 검색 기록을 삭제했습니다.", `${next.length}개 검색어가 남았습니다.`);
    }

    function renderSearchAssist(keyword = "") {
        if (!elements.searchAssist || !elements.searchSuggestionList) {
            return;
        }
        const normalized = keyword.trim().toLowerCase();
        const recentSuggestions = readSearchHistory()
            .filter((item) => !normalized || item.toLowerCase().includes(normalized))
            .slice(0, 3)
            .map((query) => ({ type: "최근", label: query, description: "최근 검색어", query }));
        const brandSuggestions = uniqueSearchIndexValues("brand")
            .filter((value) => !normalized || value.toLowerCase().includes(normalized))
            .slice(0, normalized ? 3 : 2)
            .map((value) => ({ type: "브랜드", label: value, description: "브랜드 상품만 보기", query: "", brand: value }));
        const categorySuggestions = uniqueSearchIndexValues("category")
            .filter((value) => !normalized || value.toLowerCase().includes(normalized))
            .slice(0, normalized ? 2 : 1)
            .map((value) => ({ type: "카테고리", label: value, description: "카테고리 상품만 보기", query: "", category: value }));
        const productSuggestions = searchIndexProducts
            .filter((product) => normalized && [product.name, product.headline, product.model, product.brand, product.category]
                .some((value) => String(value || "").toLowerCase().includes(normalized)))
            .slice(0, 5)
            .map((product) => ({
                type: "상품",
                label: product.name || product.headline,
                description: `${product.brand || "-"} · ${product.model || "-"} · ${product.priceLabel || formatPrice(product.price)}`,
                query: product.model || product.name || "",
                resetFacets: true
            }));

        activeSearchSuggestions = normalized
            ? productSuggestions.concat(brandSuggestions, categorySuggestions, recentSuggestions).slice(0, 8)
            : recentSuggestions.concat(brandSuggestions, categorySuggestions).slice(0, 6);
        activeSearchSuggestionIndex = activeSearchSuggestions.length ? 0 : -1;
        elements.searchAssist.hidden = false;
        elements.searchInput?.setAttribute("aria-expanded", "true");
        setText(elements.searchResultStatus, activeSearchSuggestions.length
            ? `${activeSearchSuggestions.length}개 추천 · ↑↓ 이동 · Enter 적용`
            : "일치하는 추천이 없습니다. Enter로 직접 검색할 수 있습니다.");
        elements.searchSuggestionList.innerHTML = activeSearchSuggestions.length
            ? activeSearchSuggestions.map((item, index) => `
                <button class="catalog-search-suggestion ${index === activeSearchSuggestionIndex ? "is-active" : ""}" id="searchSuggestion-${index}" role="option" type="button" data-search-suggestion-index="${index}" aria-selected="${index === activeSearchSuggestionIndex}">
                    <span>${escapeMarkup(item.type)}</span>
                    <strong>${escapeMarkup(item.label)}</strong>
                    <small>${escapeMarkup(item.description)}</small>
                </button>
            `).join("")
            : `<div class="catalog-search-assist__empty">직접 검색하려면 Enter를 눌러주세요.</div>`;
        syncSearchActiveDescendant();
    }

    function uniqueSearchIndexValues(key) {
        return Array.from(new Set(searchIndexProducts.map((product) => product[key]).filter(Boolean)))
            .sort((left, right) => left.localeCompare(right, "ko"));
    }

    function moveSearchSuggestion(direction) {
        if (!activeSearchSuggestions.length) {
            return;
        }
        activeSearchSuggestionIndex = (activeSearchSuggestionIndex + direction + activeSearchSuggestions.length) % activeSearchSuggestions.length;
        elements.searchSuggestionList?.querySelectorAll("[data-search-suggestion-index]").forEach((button, index) => {
            const isActive = index === activeSearchSuggestionIndex;
            button.classList.toggle("is-active", isActive);
            button.setAttribute("aria-selected", String(isActive));
        });
        syncSearchActiveDescendant();
    }

    function syncSearchActiveDescendant() {
        const activeId = activeSearchSuggestionIndex >= 0 ? `searchSuggestion-${activeSearchSuggestionIndex}` : "";
        if (activeId) {
            elements.searchInput?.setAttribute("aria-activedescendant", activeId);
        } else {
            elements.searchInput?.removeAttribute("aria-activedescendant");
        }
    }

    async function applySearchSuggestion(suggestion) {
        if (!suggestion) {
            return;
        }
        window.clearTimeout(searchDebounceTimer);
        state.search = String(suggestion.query || "").trim().toLowerCase();
        if (suggestion.resetFacets) {
            state.brand = "ALL";
            state.category = "ALL";
        }
        if (suggestion.brand) {
            state.brand = suggestion.brand;
            state.category = "ALL";
        }
        if (suggestion.category) {
            state.category = suggestion.category;
            state.brand = "ALL";
        }
        syncControls();
        closeSearchAssist();
        await refreshCatalog();
        document.getElementById("catalogGrid")?.scrollIntoView({ behavior: "smooth", block: "start" });
    }

    function closeSearchAssist() {
        if (elements.searchAssist) {
            elements.searchAssist.hidden = true;
        }
        elements.searchInput?.setAttribute("aria-expanded", "false");
        elements.searchInput?.removeAttribute("aria-activedescendant");
        activeSearchSuggestionIndex = -1;
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

    function renderHiddenProducts() {
        if (!elements.hiddenProductList) {
            return;
        }
        const hiddenProducts = readHiddenProducts();
        setText(elements.hiddenProductCount, String(hiddenProducts.length));
        if (!hiddenProducts.length) {
            elements.hiddenProductList.innerHTML = `<span class="catalog-memory-empty">숨긴 상품이 없습니다.</span>`;
            syncViewButtons();
            return;
        }
        const visibleProducts = hiddenProducts.map((product, originalIndex) => ({ product, originalIndex }));
        if (memoryState.hiddenAlphabetical) {
            visibleProducts.sort((left, right) => (left.product.headline || left.product.name || "").localeCompare(right.product.headline || right.product.name || "", "ko"));
        }
        elements.hiddenProductList.innerHTML = visibleProducts.map(({ product, originalIndex }) => `
            <div class="catalog-memory-item">
                <button class="catalog-memory-item__primary" type="button" data-hidden-product-index="${originalIndex}">${product.headline || product.name}</button>
                <button type="button" data-restore-hidden-index="${originalIndex}" aria-label="숨긴 상품 복구">복구</button>
            </div>
        `).join("");
        elements.hiddenProductList.querySelectorAll("[data-hidden-product-index]").forEach((button) => {
            button.addEventListener("click", () => {
                const selected = readHiddenProducts()[Number(button.dataset.hiddenProductIndex)];
                if (selected?.id) {
                    openDrawer(Number(selected.id));
                }
            });
        });
        elements.hiddenProductList.querySelectorAll("[data-restore-hidden-index]").forEach((button) => {
            button.addEventListener("click", async () => {
                const next = readHiddenProducts().filter((_, index) => index !== Number(button.dataset.restoreHiddenIndex));
                window.localStorage.setItem(HIDDEN_PRODUCTS_KEY, JSON.stringify(next));
                renderHiddenProducts();
                await refreshCatalog();
                showToast("숨긴 상품을 복구했습니다.", "카탈로그에서 다시 확인할 수 있습니다.");
            });
        });
        syncViewButtons();
    }

    function persistLastCatalogState() {
        window.localStorage.setItem(LAST_CATALOG_STATE_KEY, JSON.stringify(state));
    }

    function restoreLastCatalogState() {
        try {
            const parsed = JSON.parse(readStorageValue(window.localStorage, LAST_CATALOG_STATE_KEY) || "{}");
            Object.assign(state, normalizeStoredCatalogState(parsed));
            return true;
        } catch (error) {
            return false;
        }
    }

    function persistSearchHistory() {
        if (!state.search) {
            return;
        }
        const current = readSearchHistory().filter((keyword) => keyword !== state.search);
        const next = [state.search].concat(current).slice(0, 8);
        window.localStorage.setItem(SEARCH_HISTORY_KEY, JSON.stringify(next));
    }

    function readSavedViews() {
        try {
            const parsed = JSON.parse(readStorageValue(window.localStorage, SAVED_VIEWS_KEY) || "[]");
            if (!Array.isArray(parsed)) return [];
            const summaries = new Set();
            return parsed.slice(0, 24).flatMap((item) => {
                const summary = storedText(item?.summary, 120);
                if (!summary || summaries.has(summary) || !item?.snapshot || typeof item.snapshot !== "object") return [];
                summaries.add(summary);
                return [{ summary, snapshot: normalizeStoredCatalogState(item.snapshot) }];
            }).slice(0, 6);
        } catch (error) {
            return [];
        }
    }

    function readSearchHistory() {
        try {
            const parsed = JSON.parse(readStorageValue(window.localStorage, SEARCH_HISTORY_KEY) || "[]");
            if (!Array.isArray(parsed)) return [];
            return Array.from(new Set(parsed.map((keyword) => storedText(keyword, 100)).filter(Boolean))).slice(0, 8);
        } catch (error) {
            return [];
        }
    }

    function readHiddenProducts() {
        try {
            const parsed = JSON.parse(readStorageValue(window.localStorage, HIDDEN_PRODUCTS_KEY) || "[]");
            if (!Array.isArray(parsed)) return [];
            const ids = new Set();
            return parsed.slice(0, 48).flatMap((item) => {
                const id = Number(item?.id);
                if (!Number.isSafeInteger(id) || id <= 0 || ids.has(id)) return [];
                ids.add(id);
                return [{ id, name: storedText(item.name, 200), headline: storedText(item.headline, 200), brand: storedText(item.brand, 100) }];
            }).slice(0, 12);
        } catch (error) {
            return [];
        }
    }

    function readStorageValue(storage, key) {
        try {
            return storage.getItem(key);
        } catch (error) {
            return null;
        }
    }

    function safeStorageWrite(storage, key, value) {
        try {
            storage.setItem(key, value);
            return true;
        } catch (error) {
            return false;
        }
    }

    function storedText(value, limit) {
        if (typeof value !== "string") return "";
        const normalized = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
        return normalized.slice(0, limit);
    }

    function normalizeStoredCatalogState(value) {
        const source = value && typeof value === "object" && !Array.isArray(value) ? value : {};
        const pick = (candidate, allowed, fallback) => allowed.includes(candidate) ? candidate : fallback;
        return {
            search: storedText(source.search, 100),
            brand: storedText(source.brand, 100) || DEFAULT_STATE.brand,
            category: storedText(source.category, 100) || DEFAULT_STATE.category,
            stock: pick(source.stock, ["ALL", "LOW", "STABLE"], DEFAULT_STATE.stock),
            sort: pick(source.sort, ["LATEST", "NAME_ASC", "PRICE_HIGH", "PRICE_LOW", "STOCK_ASC", "STOCK_DESC", "FEATURED"], DEFAULT_STATE.sort),
            lowStockThreshold: pick(String(source.lowStockThreshold || ""), ["10", "20", "30", "50"], DEFAULT_STATE.lowStockThreshold),
            featuredOnly: pick(source.featuredOnly, ["ALL", "FEATURED"], DEFAULT_STATE.featuredOnly),
            priceBand: pick(source.priceBand, ["ALL", "UNDER_200", "BETWEEN_200_300", "OVER_300"], DEFAULT_STATE.priceBand)
        };
    }

    function viewSummaryLabel() {
        const labels = [];
        if (state.brand !== "ALL") {
            labels.push(state.brand);
        }
        if (state.category !== "ALL") {
            labels.push(state.category);
        }
        if (state.search) {
            labels.push(`검색:${state.search}`);
        }
        if (state.stock === "LOW") {
            labels.push("저재고");
        }
        if (state.featuredOnly === "FEATURED") {
            labels.push("Featured");
        }
        return labels.length ? labels.join(" · ") : "전체 탐색";
    }

    function buildSummaryText(count) {
        const activeFilters = Object.entries(state)
            .filter(([key, value]) => value !== DEFAULT_STATE[key])
            .map(([key]) => key);

        if (!activeFilters.length) {
            return "브랜드, 카테고리, 가격대 조건 없이 전체 카탈로그를 보고 있습니다.";
        }
        if (!count) {
            return "현재 조건에서는 결과가 없어 일부 필터를 해제하는 편이 좋습니다.";
        }
        return `${activeFilters.length}개 조건이 적용된 상태이며, 공유 가능한 URL로 탐색 상태를 유지합니다.`;
    }

    function buildFlowBoardSummaryText() {
        const recentProducts = readRecentProducts().slice(0, 3);
        const comparedProducts = readCompareProducts();
        const bookmarkedProducts = readBookmarkProducts();
        const hiddenProducts = readHiddenProducts();

        return [
            "개인 보드 요약",
            `최근 본 상품 ${recentProducts.length}개`,
            recentProducts.length
                ? recentProducts.map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)}`).join("\n")
                : "최근 본 상품이 없습니다.",
            `비교 보드 ${comparedProducts.length}개`,
            comparedProducts.length ? buildCompareSummary(comparedProducts) : "비교 보드가 비어 있습니다.",
            `관심 상품 ${bookmarkedProducts.length}개`,
            bookmarkedProducts.length ? buildBookmarkSummary(bookmarkedProducts) : "관심 상품이 없습니다.",
            `숨김 상품 ${hiddenProducts.length}개`
        ].filter(Boolean).join("\n");
    }

    function normalizeStateValue(value, allowedValues, fallbackValue) {
        return allowedValues.includes(value) ? value : fallbackValue;
    }

    function detailPageUrl(productId) {
        return `/front/products/${productId}${window.location.search || ""}`;
    }

    function catalogSummaryClipboardText(list) {
        return [
            `탐색 결과 ${list.length}개`,
            buildSummaryText(list.length),
            list.slice(0, 5).map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · ${stockLabel(product.stock)}`).join("\n")
        ].filter(Boolean).join("\n");
    }

    async function copyCatalogCardSummary(productId) {
        const product = products.find((item) => Number(item.id) === Number(productId));
        if (!product) {
            return;
        }
        const text = [
            product.headline || product.name,
            compactProductContext(product),
            `카테고리 ${product.category} · ${featuredRankLabel(product)}`,
            stockPressureDetail(product.stock),
            product.description
        ].filter(Boolean).join("\n");
        await copyTextWithFeedback(text, "카드 요약을 복사했습니다.", "현재 상품 카드 정보를 바로 전달할 수 있습니다.");
    }

    async function shareCatalogCardLink(productId) {
        const product = products.find((item) => Number(item.id) === Number(productId));
        if (!product) {
            return;
        }
        const shareUrl = `${window.location.origin}${detailPageUrl(product.id)}`;
        await copyTextWithFeedback(shareUrl, "상품 링크를 복사했습니다.", "현재 카드의 상세 링크를 바로 전달할 수 있습니다.");
    }

    async function applyCatalogCardFocus(productId, focusType) {
        const product = products.find((item) => Number(item.id) === Number(productId));
        if (!product) {
            return;
        }
        if (focusType === "BRAND") {
            state.brand = product.brand || "ALL";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("브랜드 흐름으로 전환했습니다.", `${product.brand} 기준으로 카탈로그를 다시 정렬했습니다.`);
            return;
        }
        if (focusType === "CATEGORY") {
            state.category = product.category || "ALL";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("카테고리 흐름으로 전환했습니다.", `${product.category} 기준으로 비슷한 상품을 다시 확인할 수 있습니다.`);
            return;
        }
        if (focusType === "LOW_STOCK") {
            state.stock = "LOW";
            state.sort = "STOCK_ASC";
            state.brand = product.brand || "ALL";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("긴장 재고 흐름으로 전환했습니다.", `${product.brand} 중심으로 저재고 상품부터 다시 탐색합니다.`);
            return;
        }
        if (focusType === "FEATURED") {
            state.featuredOnly = "FEATURED";
            state.sort = "FEATURED";
            state.brand = product.brand || "ALL";
            syncControls();
            await refreshCatalog();
            document.getElementById("featured")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("Featured 흐름으로 전환했습니다.", "대표 노출 상품 기준으로 다시 탐색합니다.");
            return;
        }
        if (focusType === "PREMIUM") {
            state.priceBand = "OVER_300";
            state.sort = "PRICE_HIGH";
            state.category = product.category || "ALL";
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("고가 상품 흐름으로 전환했습니다.", `${product.category} 안에서 가격 상단 상품을 다시 확인할 수 있습니다.`);
        }
    }

    function renderRecentViewed() {
        syncPersonalCounts();
        if (!elements.recentViewedSection || !elements.recentViewedGrid) {
            return;
        }
        const allRecentProducts = readRecentProducts();
        const sortedProducts = sortedRecentProducts(allRecentProducts);
        const recentProducts = boardState.recentExpanded ? sortedProducts : sortedProducts.slice(0, 3);
        if (!allRecentProducts.length) {
            elements.recentViewedSection.hidden = true;
            return;
        }
        elements.recentViewedSection.hidden = false;
        syncBoardButtons();
        setText(elements.recentViewedTitle, `${recentProducts.length} / ${allRecentProducts.length}개 최근 본 상품을 표시합니다.`);
        setText(elements.recentViewedText, "방금 본 흐름을 끊지 않고 상세와 카탈로그를 오갈 수 있습니다.");
        if (elements.toggleRecentExpandedButton) {
            elements.toggleRecentExpandedButton.textContent = boardState.recentExpanded ? "3개만 보기" : "전체 펼치기";
            elements.toggleRecentExpandedButton.disabled = allRecentProducts.length <= 3;
        }
        const recommendedId = recommendedRecentProduct(recentProducts)?.id;
        elements.recentViewedGrid.innerHTML = recentProducts.length ? recentProducts.map((product) => `
            <article class="detail-related-card compare-card saved-product-card">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"} · ${stockPressureDetail(product.stock)}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>최근 확인</span>
                    ${Number(product.id) === Number(recommendedId) ? "<span>다시 볼 상품</span>" : ""}
                </div>
                <div class="compare-card__actions saved-product-card__actions">
                    <a class="catalog-card__link" href="${detailPageUrl(product.id)}">상세 보기</a>
                    <button class="catalog-reset-button" type="button" data-open-recent-id="${product.id}">빠른 보기</button>
                    <details class="saved-product-card__menu">
                        <summary aria-label="상품 추가 작업">•••</summary>
                        <div>
                            <button class="catalog-reset-button" type="button" data-compare-recent-id="${product.id}">비교 담기</button>
                            <button class="catalog-reset-button" type="button" data-bookmark-recent-id="${product.id}">관심 상품 추가</button>
                            <button class="catalog-reset-button" type="button" data-copy-recent-id="${product.id}">요약 복사</button>
                        </div>
                    </details>
                </div>
            </article>
        `).join("") : `
            <article class="catalog-empty">
                <strong>보드 필터에 맞는 최근 상품이 없습니다.</strong>
                <p>보드 필터를 해제해 최근 본 상품을 다시 확인해보세요.</p>
            </article>
        `;
        bindRecentCardActions(recentProducts);
    }

    function bindRecentCardActions(recentProducts) {
        elements.recentViewedGrid?.querySelectorAll("[data-open-recent-id]").forEach((button) => {
            button.addEventListener("click", () => openDrawer(Number(button.dataset.openRecentId)));
        });
        elements.recentViewedGrid?.querySelectorAll("[data-compare-recent-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const product = recentProducts.find((item) => Number(item.id) === Number(button.dataset.compareRecentId));
                if (product) {
                    addProductsToBoard([product], "COMPARE");
                }
            });
        });
        elements.recentViewedGrid?.querySelectorAll("[data-bookmark-recent-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const product = recentProducts.find((item) => Number(item.id) === Number(button.dataset.bookmarkRecentId));
                if (product) {
                    addProductsToBoard([product], "BOOKMARK");
                }
            });
        });
        elements.recentViewedGrid?.querySelectorAll("[data-copy-recent-id]").forEach((button) => {
            button.addEventListener("click", async () => {
                const product = recentProducts.find((item) => Number(item.id) === Number(button.dataset.copyRecentId));
                if (product) {
                    await copyTextWithFeedback(`${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · 재고 ${product.stock}개`, "최근 상품 요약을 복사했습니다.", "최근 확인한 상품 정보를 바로 전달할 수 있습니다.");
                }
            });
        });
    }

    function readRecentProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(RECENT_VIEWED_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function addProductsToBoard(sourceProducts, target) {
        if (!sourceProducts.length) {
            showToast("옮길 상품이 없습니다.", "현재 보드에 상품을 먼저 추가해주세요.", true);
            return;
        }
        const isCompare = target === "COMPARE";
        const current = isCompare ? readCompareProducts() : readBookmarkProducts();
        const limit = isCompare ? 3 : 6;
        const merged = sourceProducts.concat(current).filter((product, index, items) =>
            items.findIndex((item) => Number(item.id) === Number(product.id)) === index
        ).slice(0, limit);
        if (isCompare) {
            writeCompareProducts(merged);
        } else {
            writeBookmarkProducts(merged);
        }
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderFlowBoard();
        renderCatalog();
        showToast(
            isCompare ? "비교 보드에 상품을 담았습니다." : "관심 상품에 모두 담았습니다.",
            `${merged.length}개 상품을 ${isCompare ? "비교 후보" : "관심 목록"}로 유지합니다.`
        );
    }

    function renderCompareBoard() {
        syncPersonalCounts();
        if (!elements.compareBoardSection || !elements.compareBoardGrid) {
            return;
        }
        const comparedProducts = sortedCompareProducts(readCompareProducts());
        if (!comparedProducts.length) {
            elements.compareBoardSection.hidden = true;
            return;
        }
        elements.compareBoardSection.hidden = false;
        syncBoardButtons();
        setText(elements.compareBoardTitle, `${comparedProducts.length}개 상품을 비교 중입니다.`);
        setText(elements.compareBoardText, buildCompareSummary(comparedProducts));
        const comparePrices = comparedProducts.map((product) => Number(product.price || 0));
        const compareStocks = comparedProducts.map((product) => Number(product.stock || 0));
        setText(elements.compareAveragePrice, formatPrice(Math.round(comparePrices.reduce((sum, price) => sum + price, 0) / comparedProducts.length)));
        setText(elements.comparePriceGap, formatPrice(Math.max(...comparePrices) - Math.min(...comparePrices)));
        setText(elements.compareTotalStock, `${compareStocks.reduce((sum, stock) => sum + stock, 0)}개`);
        setText(elements.compareStockGap, `${Math.max(...compareStocks) - Math.min(...compareStocks)}개`);
        setText(elements.compareCategoryCount, `${new Set(comparedProducts.map((product) => product.category).filter(Boolean)).size}개`);
        const cheapestId = comparedProducts.slice().sort((left, right) => Number(left.price || 0) - Number(right.price || 0))[0]?.id;
        const highestStockId = comparedProducts.slice().sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0))[0]?.id;
        const recommendedId = recommendedCompareProduct(comparedProducts)?.id;
        elements.compareBoardGrid.innerHTML = comparedProducts.map((product) => `
            <article class="detail-related-card compare-card saved-product-card">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"} · ${product.category || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>재고 ${product.stock}개</span>
                    ${Number(product.id) === Number(cheapestId) ? "<span>최저가</span>" : ""}
                    ${Number(product.id) === Number(highestStockId) ? "<span>최다 재고</span>" : ""}
                    ${Number(product.id) === Number(recommendedId) ? "<span>균형 추천</span>" : ""}
                </div>
                <div class="compare-card__actions saved-product-card__actions">
                    <a class="catalog-card__link" href="${detailPageUrl(product.id)}">상세 보기</a>
                    <button class="catalog-reset-button" type="button" data-open-compare-id="${product.id}">빠른 보기</button>
                    <details class="saved-product-card__menu">
                        <summary aria-label="상품 추가 작업">•••</summary>
                        <div>
                            <button class="catalog-reset-button" type="button" data-bookmark-compare-id="${product.id}">관심 상품 추가</button>
                            <button class="catalog-reset-button" type="button" data-copy-compare-id="${product.id}">요약 복사</button>
                            <button class="catalog-reset-button" type="button" data-focus-compare-brand="${product.id}">브랜드 상품 보기</button>
                            <button class="catalog-reset-button saved-product-card__danger" type="button" data-remove-compare-id="${product.id}">비교에서 제거</button>
                        </div>
                    </details>
                </div>
            </article>
        `).join("");

        elements.compareBoardGrid.querySelectorAll("[data-remove-compare-id]").forEach((button) => {
            button.addEventListener("click", () => {
                removeCompareProduct(Number(button.dataset.removeCompareId));
            });
        });
        elements.compareBoardGrid.querySelectorAll("[data-open-compare-id]").forEach((button) => {
            button.addEventListener("click", () => openDrawer(Number(button.dataset.openCompareId)));
        });
        elements.compareBoardGrid.querySelectorAll("[data-bookmark-compare-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const product = comparedProducts.find((item) => Number(item.id) === Number(button.dataset.bookmarkCompareId));
                if (product) {
                    addProductsToBoard([product], "BOOKMARK");
                }
            });
        });
        elements.compareBoardGrid.querySelectorAll("[data-copy-compare-id]").forEach((button) => {
            button.addEventListener("click", async () => {
                const product = comparedProducts.find((item) => Number(item.id) === Number(button.dataset.copyCompareId));
                if (product) {
                    await copyTextWithFeedback(`${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · 재고 ${product.stock}개`, "비교 상품 요약을 복사했습니다.", "선택한 후보 정보를 바로 전달할 수 있습니다.");
                }
            });
        });
        elements.compareBoardGrid.querySelectorAll("[data-focus-compare-brand]").forEach((button) => {
            button.addEventListener("click", async () => {
                const product = comparedProducts.find((item) => Number(item.id) === Number(button.dataset.focusCompareBrand));
                if (!product?.brand) {
                    return;
                }
                state.brand = product.brand;
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function renderBookmarkBoard() {
        syncPersonalCounts();
        if (!elements.bookmarkBoardSection || !elements.bookmarkBoardGrid) {
            return;
        }
        const allBookmarkedProducts = readBookmarkProducts();
        syncMobileStoreNavigation();
        const bookmarkedProducts = sortedBookmarkProducts(allBookmarkedProducts);
        if (!allBookmarkedProducts.length) {
            elements.bookmarkBoardSection.hidden = true;
            return;
        }
        elements.bookmarkBoardSection.hidden = false;
        syncBoardButtons();
        setText(elements.bookmarkBoardTitle, `${bookmarkedProducts.length} / ${allBookmarkedProducts.length}개 관심 상품을 표시합니다.`);
        setText(elements.bookmarkBoardText, buildBookmarkSummary(bookmarkedProducts));
        const bookmarkTotalPrice = bookmarkedProducts.reduce((sum, product) => sum + Number(product.price || 0), 0);
        setText(elements.bookmarkAveragePrice, formatPrice(bookmarkedProducts.length ? Math.round(bookmarkTotalPrice / bookmarkedProducts.length) : 0));
        setText(elements.bookmarkTotalStock, `${bookmarkedProducts.reduce((sum, product) => sum + Number(product.stock || 0), 0)}개`);
        setText(elements.bookmarkLowStockCount, `${bookmarkedProducts.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length}개`);
        setText(elements.bookmarkFeaturedCount, `${bookmarkedProducts.filter((product) => Boolean(product.featured)).length}개`);
        setText(elements.bookmarkDominantCategory, dominantCategory(bookmarkedProducts) || "-");
        const recommendedId = recommendedBookmarkProduct(bookmarkedProducts)?.id;
        elements.bookmarkBoardGrid.innerHTML = bookmarkedProducts.length ? bookmarkedProducts.map((product) => `
            <article class="detail-related-card compare-card saved-product-card">
                ${productVisualMarkup(product, "detail-related-card__visual")}
                <span class="detail-related-card__brand">${product.brand || "-"}</span>
                <strong>${product.headline || product.name || "-"}</strong>
                <p>${product.name || "-"} · ${product.model || "-"} · ${product.category || "-"}</p>
                <div class="detail-related-card__meta">
                    <span>${product.priceLabel || formatPrice(product.price)}</span>
                    <span class="${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                    <span>${product.featured ? "Featured" : "Watchlist"}</span>
                    ${Number(product.id) === Number(recommendedId) ? "<span>우선 확인</span>" : ""}
                </div>
                <div class="compare-card__actions saved-product-card__actions">
                    <a class="catalog-card__link" href="${detailPageUrl(product.id)}">상세 보기</a>
                    <button class="catalog-reset-button" type="button" data-open-bookmark-id="${product.id}">빠른 보기</button>
                    <details class="saved-product-card__menu">
                        <summary aria-label="상품 추가 작업">•••</summary>
                        <div>
                            <button class="catalog-reset-button" type="button" data-compare-bookmark-id="${product.id}">비교 담기</button>
                            <button class="catalog-reset-button" type="button" data-copy-bookmark-id="${product.id}">요약 복사</button>
                            <button class="catalog-reset-button" type="button" data-focus-bookmark-brand="${product.id}">브랜드 상품 보기</button>
                            <button class="catalog-reset-button saved-product-card__danger" type="button" data-remove-bookmark-id="${product.id}">관심에서 제거</button>
                        </div>
                    </details>
                </div>
            </article>
        `).join("") : `
            <article class="catalog-empty">
                <strong>보드 필터에 맞는 관심 상품이 없습니다.</strong>
                <p>보드 필터를 해제해 전체 관심 상품을 다시 확인해보세요.</p>
            </article>
        `;

        elements.bookmarkBoardGrid.querySelectorAll("[data-remove-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => {
                removeBookmarkProduct(Number(button.dataset.removeBookmarkId));
            });
        });
        elements.bookmarkBoardGrid.querySelectorAll("[data-open-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => openDrawer(Number(button.dataset.openBookmarkId)));
        });
        elements.bookmarkBoardGrid.querySelectorAll("[data-compare-bookmark-id]").forEach((button) => {
            button.addEventListener("click", () => {
                const product = bookmarkedProducts.find((item) => Number(item.id) === Number(button.dataset.compareBookmarkId));
                if (product) {
                    addProductsToBoard([product], "COMPARE");
                }
            });
        });
        elements.bookmarkBoardGrid.querySelectorAll("[data-copy-bookmark-id]").forEach((button) => {
            button.addEventListener("click", async () => {
                const product = bookmarkedProducts.find((item) => Number(item.id) === Number(button.dataset.copyBookmarkId));
                if (product) {
                    await copyTextWithFeedback(`${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · 재고 ${product.stock}개`, "관심 상품 요약을 복사했습니다.", "선택한 관심 상품 정보를 바로 전달할 수 있습니다.");
                }
            });
        });
        elements.bookmarkBoardGrid.querySelectorAll("[data-focus-bookmark-brand]").forEach((button) => {
            button.addEventListener("click", async () => {
                const product = bookmarkedProducts.find((item) => Number(item.id) === Number(button.dataset.focusBookmarkBrand));
                if (!product?.brand) {
                    return;
                }
                state.brand = product.brand;
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function findKnownProduct(productId) {
        const collectionProducts = Object.values(homeCollections).flat();
        return products
            .concat(collectionProducts)
            .find((product) => Number(product.id) === Number(productId));
    }

    function toggleCompareProduct(productId) {
        const source = findKnownProduct(productId);
        if (!source) {
            return;
        }
        const current = readCompareProducts();
        const exists = current.some((product) => Number(product.id) === Number(productId));
        if (exists) {
            writeCompareProducts(current.filter((product) => Number(product.id) !== Number(productId)));
            showToast("비교 대상에서 제외했습니다.", `${source.headline || source.name}을 비교 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: source.id,
                brand: source.brand,
                name: source.name,
                headline: source.headline,
                model: source.model,
                category: source.category,
                price: source.price,
                priceLabel: source.priceLabel,
                stock: source.stock,
                stockStatus: source.stockStatus
            };
            if (current.length >= 3) {
                showToast("비교 보드는 최대 3개까지 담을 수 있습니다.", "가장 오래 담긴 상품을 교체했습니다.", true);
            } else {
                showToast("비교 보드에 담았습니다.", `${source.headline || source.name}을 비교 목록에 추가했습니다.`);
            }
            writeCompareProducts([summary].concat(current).slice(0, 3));
        }
        renderCompareBoard();
        renderFlowBoard();
        renderCatalog();
    }

    function removeCompareProduct(productId) {
        writeCompareProducts(readCompareProducts().filter((product) => Number(product.id) !== Number(productId)));
        renderCompareBoard();
        renderFlowBoard();
        renderCatalog();
    }

    function toggleBookmarkProduct(productId) {
        const source = findKnownProduct(productId);
        if (!source) {
            return;
        }
        const current = readBookmarkProducts();
        const exists = current.some((product) => Number(product.id) === Number(productId));
        if (exists) {
            writeBookmarkProducts(current.filter((product) => Number(product.id) !== Number(productId)));
            showToast("관심 상품에서 제외했습니다.", `${source.headline || source.name}을 찜 보드에서 뺐습니다.`);
        } else {
            const summary = {
                id: source.id,
                brand: source.brand,
                name: source.name,
                headline: source.headline,
                model: source.model,
                category: source.category,
                price: source.price,
                priceLabel: source.priceLabel,
                stock: source.stock,
                stockStatus: source.stockStatus,
                featured: Boolean(source.featured),
                thumbnailUrl: source.thumbnailUrl
            };
            if (current.length >= 6) {
                showToast("관심 상품은 최대 6개까지 유지합니다.", "가장 오래 담긴 상품을 교체했습니다.", true);
            } else {
                showToast("관심 상품에 담았습니다.", `${source.headline || source.name}을 나중에 다시 볼 수 있습니다.`);
            }
            writeBookmarkProducts([summary].concat(current).slice(0, 6));
        }
        renderBookmarkBoard();
        renderFlowBoard();
        renderSignalStrip();
        renderFeatured();
        renderCatalog();
    }

    function removeBookmarkProduct(productId) {
        writeBookmarkProducts(readBookmarkProducts().filter((product) => Number(product.id) !== Number(productId)));
        renderBookmarkBoard();
        renderFlowBoard();
        renderCatalog();
    }

    function readCompareProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(COMPARE_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function writeCompareProducts(productsToCompare) {
        if (window.StorefrontState) {
            window.StorefrontState.write("compare", productsToCompare);
            return;
        }
        window.localStorage.setItem(COMPARE_PRODUCTS_KEY, JSON.stringify(productsToCompare));
    }

    function isComparedProduct(productId) {
        return readCompareProducts().some((product) => Number(product.id) === Number(productId));
    }

    function isBookmarkedProduct(productId) {
        return readBookmarkProducts().some((product) => Number(product.id) === Number(productId));
    }

    function isHiddenProduct(productId) {
        return readHiddenProducts().some((product) => Number(product.id) === Number(productId));
    }

    function readBookmarkProducts() {
        try {
            const parsed = JSON.parse(window.localStorage.getItem(BOOKMARK_PRODUCTS_KEY) || "[]");
            return Array.isArray(parsed) ? parsed.filter((item) => item?.id) : [];
        } catch (error) {
            return [];
        }
    }

    function syncPersonalCounts() {
        setText(elements.utilityRecentCount, String(readRecentProducts().length));
        setText(elements.utilityBookmarkCount, String(readBookmarkProducts().length));
        setText(elements.utilityCompareCount, String(readCompareProducts().length));
    }

    async function resetPersonalData() {
        if (!window.confirm("관심, 비교, 최근 본 상품과 저장한 탐색 데이터를 모두 초기화할까요?")) {
            return;
        }
        [
            BOOKMARK_PRODUCTS_KEY,
            COMPARE_PRODUCTS_KEY,
            RECENT_VIEWED_KEY,
            SAVED_VIEWS_KEY,
            SEARCH_HISTORY_KEY,
            LAST_CATALOG_STATE_KEY,
            LAST_DRAWER_PRODUCT_KEY,
            HIDDEN_PRODUCTS_KEY
        ].forEach((key) => window.localStorage.removeItem(key));
        selectedProductIds.clear();
        resetState();
        syncControls();
        await refreshCatalog();
        announceStorefrontStatus("개인 탐색 데이터를 초기화했습니다.");
        showToast("개인 탐색 데이터를 초기화했습니다.", "화면 설정은 유지하고 저장·비교·최근 기록만 정리했습니다.");
    }

    function syncPersonalStateFromStorage(event) {
        const personalKeys = new Set([
            BOOKMARK_PRODUCTS_KEY,
            COMPARE_PRODUCTS_KEY,
            RECENT_VIEWED_KEY,
            SAVED_VIEWS_KEY,
            SEARCH_HISTORY_KEY,
            HIDDEN_PRODUCTS_KEY
        ]);
        if (event.key && !personalKeys.has(event.key)) {
            return;
        }
        renderRecentViewed();
        renderCompareBoard();
        renderBookmarkBoard();
        renderSavedViews();
        renderSearchHistory();
        renderHiddenProducts();
        renderFlowBoard();
        renderCatalog();
        syncPersonalCounts();
        announceStorefrontStatus("다른 탭에서 변경된 개인 탐색 상태를 반영했습니다.");
    }

    function writeBookmarkProducts(bookmarkedProducts) {
        if (window.StorefrontState) {
            window.StorefrontState.write("bookmark", bookmarkedProducts);
            return;
        }
        window.localStorage.setItem(BOOKMARK_PRODUCTS_KEY, JSON.stringify(bookmarkedProducts));
    }

    function toggleHiddenProduct(productId) {
        const source = products.find((product) => Number(product.id) === Number(productId));
        if (!source) {
            return;
        }
        const current = readHiddenProducts();
        const exists = current.some((product) => Number(product.id) === Number(productId));
        if (exists) {
            window.localStorage.setItem(HIDDEN_PRODUCTS_KEY, JSON.stringify(current.filter((product) => Number(product.id) !== Number(productId))));
            showToast("숨김 상품을 복구했습니다.", `${source.headline || source.name}이 다시 목록에 노출됩니다.`);
        } else {
            const summary = {
                id: source.id,
                name: source.name,
                headline: source.headline,
                brand: source.brand
            };
            window.localStorage.setItem(HIDDEN_PRODUCTS_KEY, JSON.stringify([summary].concat(current).slice(0, 12)));
            showToast("상품을 숨겼습니다.", `${source.headline || source.name}은 기본 목록에서 제외됩니다.`);
        }
        renderHiddenProducts();
        renderFlowBoard();
    }

    function buildCompareSummary(comparedProducts) {
        if (comparedProducts.length < 2) {
            return "하나를 더 담으면 가격과 재고 차이를 더 명확하게 비교할 수 있습니다.";
        }
        const prices = comparedProducts.map((product) => Number(product.price || 0));
        const stocks = comparedProducts.map((product) => Number(product.stock || 0));
        const priceGap = Math.max(...prices) - Math.min(...prices);
        const stockGap = Math.max(...stocks) - Math.min(...stocks);
        const categories = new Set(comparedProducts.map((product) => product.category).filter(Boolean));
        const categoryHint = categories.size === 1 ? "같은 카테고리 안에서 비교 중입니다." : `${categories.size}개 카테고리를 함께 보고 있습니다.`;
        return `최고가와 최저가 차이는 ${formatPrice(priceGap)}, 재고 차이는 ${stockGap}개입니다. ${categoryHint}`;
    }

    function recommendedCompareProduct(items) {
        if (!items.length) {
            return null;
        }
        const prices = items.map((item) => Number(item.price || 0));
        const stocks = items.map((item) => Number(item.stock || 0));
        const minPrice = Math.min(...prices);
        const maxPrice = Math.max(...prices);
        const minStock = Math.min(...stocks);
        const maxStock = Math.max(...stocks);
        const priceRange = maxPrice - minPrice || 1;
        const stockRange = maxStock - minStock || 1;
        return items.slice().sort((left, right) => {
            const score = (item) => {
                const priceScore = 1 - ((Number(item.price || 0) - minPrice) / priceRange);
                const stockScore = (Number(item.stock || 0) - minStock) / stockRange;
                return priceScore * 0.55 + stockScore * 0.45;
            };
            return score(right) - score(left);
        })[0];
    }

    function buildBookmarkSummary(bookmarkedProducts) {
        const featuredCount = bookmarkedProducts.filter((product) => product.featured).length;
        const lowStockCount = bookmarkedProducts.filter((product) => Number(product.stock || 0) < lowStockThresholdValue()).length;
        if (!featuredCount) {
            return lowStockCount
                ? `${lowStockCount}개 상품이 긴장 재고 구간에 있어 우선 확인용 보드로 활용할 수 있습니다.`
                : "관심 상품을 모아두고 필요할 때 상세나 비교 보드로 바로 이동할 수 있습니다.";
        }
        return `${featuredCount}개 상품이 Featured 라인에 포함되어 있고, ${lowStockCount}개 상품은 긴장 재고 구간에 있습니다.`;
    }

    function sortedCompareProducts(items) {
        const next = items.slice();
        if (boardState.compareSort === "PRICE_HIGH") {
            return next.sort((left, right) => Number(right.price || 0) - Number(left.price || 0));
        }
        if (boardState.compareSort === "STOCK_ASC") {
            return next.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (boardState.compareSort === "PRICE_LOW") {
            return next.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        if (boardState.compareSort === "NAME_ASC") {
            return next.sort((left, right) => String(left.name || left.headline || "").localeCompare(String(right.name || right.headline || ""), "ko"));
        }
        if (boardState.compareSort === "OLDEST") {
            return next.reverse();
        }
        return next;
    }

    function sortedRecentProducts(items) {
        const dominant = dominantBrand(items);
        const next = items.filter((item) => {
            if (boardState.recentFilter === "LOW_STOCK") {
                return Number(item.stock || 0) < lowStockThresholdValue();
            }
            if (boardState.recentFilter === "DOMINANT_BRAND") {
                return item.brand === dominant;
            }
            if (boardState.recentFilter === "AVAILABLE") {
                return Number(item.stock || 0) > 0;
            }
            if (boardState.recentFilter === "SOLD_OUT") {
                return Number(item.stock || 0) <= 0;
            }
            return true;
        });
        if (boardState.recentSort === "PRICE_LOW") {
            return next.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        if (boardState.recentSort === "STOCK_ASC") {
            return next.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (boardState.recentSort === "NAME_ASC") {
            return next.sort((left, right) => String(left.name || left.headline || "").localeCompare(String(right.name || right.headline || ""), "ko"));
        }
        if (boardState.recentSort === "OLDEST") {
            return next.reverse();
        }
        return next;
    }

    function recommendedRecentProduct(items) {
        if (!items.length) {
            return null;
        }
        const maxPrice = Math.max(1, ...items.map((item) => Number(item.price || 0)));
        return items.map((item, index) => ({ item, index })).sort((left, right) => {
            const score = (entry) => {
                const urgency = Number(entry.item.stock || 0) < lowStockThresholdValue() ? 0.5 : 0;
                const affordability = (1 - (Number(entry.item.price || 0) / maxPrice)) * 0.3;
                const recency = (1 - (entry.index / Math.max(1, items.length - 1))) * 0.2;
                return urgency + affordability + recency;
            };
            return score(right) - score(left);
        })[0].item;
    }

    function sortedBookmarkProducts(items) {
        const category = dominantCategory(items);
        const next = items.filter((item) => {
            if (boardState.bookmarkFilter === "LOW_STOCK") {
                return Number(item.stock || 0) < lowStockThresholdValue();
            }
            if (boardState.bookmarkFilter === "FEATURED") {
                return Boolean(item.featured);
            }
            if (boardState.bookmarkFilter === "AVAILABLE") {
                return Number(item.stock || 0) > 0;
            }
            if (boardState.bookmarkFilter === "SOLD_OUT") {
                return Number(item.stock || 0) <= 0;
            }
            if (boardState.bookmarkFilter === "DOMINANT_CATEGORY") {
                return item.category === category;
            }
            return true;
        });
        if (boardState.bookmarkSort === "FEATURED") {
            return next.sort((left, right) => Number(Boolean(right.featured)) - Number(Boolean(left.featured)));
        }
        if (boardState.bookmarkSort === "PRICE_LOW") {
            return next.sort((left, right) => Number(left.price || 0) - Number(right.price || 0));
        }
        if (boardState.bookmarkSort === "STOCK_ASC") {
            return next.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (boardState.bookmarkSort === "NAME_ASC") {
            return next.sort((left, right) => String(left.name || left.headline || "").localeCompare(String(right.name || right.headline || ""), "ko"));
        }
        if (boardState.bookmarkSort === "OLDEST") {
            return next.reverse();
        }
        return next;
    }

    function recommendedBookmarkProduct(items) {
        if (!items.length) {
            return null;
        }
        const maxPrice = Math.max(1, ...items.map((item) => Number(item.price || 0)));
        return items.slice().sort((left, right) => {
            const score = (item) => {
                const urgency = Number(item.stock || 0) < lowStockThresholdValue() ? 0.45 : 0;
                const featured = item.featured ? 0.35 : 0;
                const affordability = (1 - (Number(item.price || 0) / maxPrice)) * 0.2;
                return urgency + featured + affordability;
            };
            return score(right) - score(left);
        })[0];
    }

    function dominantCategory(items) {
        return dominantValue(items, "category");
    }

    function dominantBrand(items) {
        return dominantValue(items, "brand");
    }

    function dominantValue(items, key) {
        const counts = new Map();
        items.forEach((item) => {
            if (!item?.[key]) {
                return;
            }
            counts.set(item[key], (counts.get(item[key]) || 0) + 1);
        });
        return Array.from(counts.entries())
            .sort((left, right) => right[1] - left[1])[0]?.[0] || "";
    }

    function applyPreset(preset) {
        if (preset === "RESET") {
            resetState();
            return;
        }
        resetState();
        if (preset === "LATEST_DROP") {
            state.sort = "LATEST";
        }
        if (preset === "LOW_STOCK") {
            state.stock = "LOW";
            state.sort = "STOCK_ASC";
        }
        if (preset === "FEATURED") {
            state.featuredOnly = "FEATURED";
            state.sort = "FEATURED";
        }
        if (preset === "PREMIUM") {
            state.priceBand = "OVER_300";
            state.sort = "PRICE_HIGH";
        }
    }

    function bindEmptyStateButtons() {
        elements.catalogGrid?.querySelectorAll("[data-empty-action]").forEach((button) => {
            button.addEventListener("click", async () => {
                const action = button.dataset.emptyAction;
                if (action.startsWith("CLEAR_")) {
                    relaxEmptyCatalogState(action);
                } else if (action !== "RETRY") {
                    applyPreset(action);
                }
                syncControls();
                await refreshCatalog();
            });
        });
    }

    function relaxEmptyCatalogState(action) {
        const stateKeys = {
            CLEAR_SEARCH: "search",
            CLEAR_BRAND: "brand",
            CLEAR_CATEGORY: "category",
            CLEAR_PRICE: "priceBand",
            CLEAR_STOCK: "stock"
        };
        const key = stateKeys[action];
        if (!key) {
            return;
        }
        state[key] = DEFAULT_STATE[key];
        paginationState.page = 1;
        showToast("탐색 조건 하나를 해제했습니다.", "나머지 조건은 유지한 채 상품 결과를 다시 확인합니다.");
    }

    function announceStorefrontStatus(message) {
        setText(elements.storefrontStatus, message);
    }

    function bindInsightButtons() {
        elements.catalogInsightGrid?.querySelectorAll("[data-insight-action]").forEach((button) => {
            button.addEventListener("click", async () => {
                const action = button.dataset.insightAction;
                if (action === "SCROLL_CATALOG") {
                    document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
                    return;
                }
                if (action === "LEAD_BRAND") {
                    const brand = dominantBrand(filteredProducts());
                    if (!brand) {
                        return;
                    }
                    state.brand = brand;
                } else {
                    applyPreset(action);
                }
                syncControls();
                await refreshCatalog();
                document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            });
        });
    }

    function syncBoardButtons() {
        elements.sortRecentPriceButton?.classList.toggle("is-active", boardState.recentSort === "PRICE_LOW");
        elements.sortRecentStockButton?.classList.toggle("is-active", boardState.recentSort === "STOCK_ASC");
        elements.sortRecentNameButton?.classList.toggle("is-active", boardState.recentSort === "NAME_ASC");
        elements.filterRecentLowStockButton?.classList.toggle("is-active", boardState.recentFilter === "LOW_STOCK");
        elements.filterRecentBrandButton?.classList.toggle("is-active", boardState.recentFilter === "DOMINANT_BRAND");
        elements.filterRecentAvailableButton?.classList.toggle("is-active", boardState.recentFilter === "AVAILABLE");
        elements.filterRecentSoldOutButton?.classList.toggle("is-active", boardState.recentFilter === "SOLD_OUT");
        elements.sortRecentOldestButton?.classList.toggle("is-active", boardState.recentSort === "OLDEST");
        elements.toggleRecentExpandedButton?.classList.toggle("is-active", boardState.recentExpanded);
        elements.sortComparePriceButton?.classList.toggle("is-active", boardState.compareSort === "PRICE_HIGH");
        elements.sortCompareStockButton?.classList.toggle("is-active", boardState.compareSort === "STOCK_ASC");
        elements.sortComparePriceLowButton?.classList.toggle("is-active", boardState.compareSort === "PRICE_LOW");
        elements.sortCompareNameButton?.classList.toggle("is-active", boardState.compareSort === "NAME_ASC");
        elements.sortCompareOldestButton?.classList.toggle("is-active", boardState.compareSort === "OLDEST");
        elements.sortBookmarkRecentButton?.classList.toggle("is-active", boardState.bookmarkSort === "RECENT");
        elements.sortBookmarkFeaturedButton?.classList.toggle("is-active", boardState.bookmarkSort === "FEATURED");
        elements.sortBookmarkPriceButton?.classList.toggle("is-active", boardState.bookmarkSort === "PRICE_LOW");
        elements.sortBookmarkStockButton?.classList.toggle("is-active", boardState.bookmarkSort === "STOCK_ASC");
        elements.sortBookmarkNameButton?.classList.toggle("is-active", boardState.bookmarkSort === "NAME_ASC");
        elements.filterBookmarkLowStockButton?.classList.toggle("is-active", boardState.bookmarkFilter === "LOW_STOCK");
        elements.filterBookmarkFeaturedButton?.classList.toggle("is-active", boardState.bookmarkFilter === "FEATURED");
        elements.filterBookmarkAvailableButton?.classList.toggle("is-active", boardState.bookmarkFilter === "AVAILABLE");
        elements.filterBookmarkSoldOutButton?.classList.toggle("is-active", boardState.bookmarkFilter === "SOLD_OUT");
        elements.filterBookmarkCategoryButton?.classList.toggle("is-active", boardState.bookmarkFilter === "DOMINANT_CATEGORY");
        elements.sortBookmarkOldestButton?.classList.toggle("is-active", boardState.bookmarkSort === "OLDEST");
    }

    function syncCurationButtons() {
        elements.sortLatestPriceButton?.classList.toggle("is-active", boardState.latestSort === "PRICE_LOW");
        elements.sortLowStockPriceButton?.classList.toggle("is-active", boardState.lowStockSort === "PRICE_LOW");
        elements.sortFeaturedPriceButton?.classList.toggle("is-active", boardState.featuredSort === "PRICE_LOW");
        elements.sortFeaturedStockButton?.classList.toggle("is-active", boardState.featuredSort === "STOCK_ASC");
    }

    function syncMemoryButtons() {
        elements.reverseSavedViewsButton?.classList.toggle("is-active", memoryState.savedReversed);
        elements.sortSearchHistoryButton?.classList.toggle("is-active", memoryState.searchAlphabetical);
        elements.sortHiddenProductsButton?.classList.toggle("is-active", memoryState.hiddenAlphabetical);
    }

    function syncViewButtons() {
        elements.toggleCompactViewButton?.classList.toggle("is-active", uiState.layout === "COMFORT");
        elements.toggleTodayOnlyButton?.classList.toggle("is-active", uiState.todayOnly);
        elements.toggleHiddenViewButton?.classList.toggle("is-active", uiState.showHiddenProducts);
        elements.catalogLayoutShopButton?.classList.toggle("is-active", uiState.layout === "SHOP");
        elements.catalogLayoutStandardButton?.classList.toggle("is-active", uiState.layout === "STANDARD");
        elements.catalogLayoutComfortButton?.classList.toggle("is-active", uiState.layout === "COMFORT");
        elements.catalogLayoutListButton?.classList.toggle("is-active", uiState.layout === "LIST");
        elements.toggleCatalogDescriptionButton?.classList.toggle("is-active", uiState.hideDescriptions);
        elements.toggleCatalogSignalsButton?.classList.toggle("is-active", uiState.hideSignals);
        elements.toggleCatalogActionsButton?.classList.toggle("is-active", uiState.hideActions);
        elements.toggleCatalogMetricsButton?.classList.toggle("is-active", uiState.hideMetrics);
        elements.toggleCatalogDistributionButton?.classList.toggle("is-active", uiState.hideDistribution);
        elements.toggleCatalogDecisionButton?.classList.toggle("is-active", uiState.hideDecision);
        const analyticsCompact = uiState.hideMetrics && uiState.hideDistribution && uiState.hideDecision;
        elements.compactCatalogAnalyticsButton?.classList.toggle("is-active", analyticsCompact);
        elements.resetCatalogAnalyticsButton?.toggleAttribute("disabled", !uiState.hideMetrics && !uiState.hideDistribution && !uiState.hideDecision);
        elements.toggleReducedMotionButton?.classList.toggle("is-active", uiState.reducedMotion);
        [
            [elements.catalogLayoutShopButton, uiState.layout === "SHOP"],
            [elements.catalogLayoutStandardButton, uiState.layout === "STANDARD"],
            [elements.catalogLayoutComfortButton, uiState.layout === "COMFORT"],
            [elements.catalogLayoutListButton, uiState.layout === "LIST"],
            [elements.toggleCatalogDescriptionButton, uiState.hideDescriptions],
            [elements.toggleCatalogSignalsButton, uiState.hideSignals],
            [elements.toggleCatalogActionsButton, uiState.hideActions],
            [elements.toggleCatalogMetricsButton, uiState.hideMetrics],
            [elements.toggleCatalogDistributionButton, uiState.hideDistribution],
            [elements.toggleCatalogDecisionButton, uiState.hideDecision],
            [elements.compactCatalogAnalyticsButton, analyticsCompact],
            [elements.toggleReducedMotionButton, uiState.reducedMotion]
        ].forEach(([button, isPressed]) => button?.setAttribute("aria-pressed", String(isPressed)));
        const layoutLabels = { SHOP: "4열 쇼핑", STANDARD: "3열 표준", COMFORT: "2열 여유", LIST: "리스트" };
        const hiddenCount = [uiState.hideDescriptions, uiState.hideSignals, uiState.hideActions, uiState.hideMetrics, uiState.hideDistribution, uiState.hideDecision].filter(Boolean).length;
        setText(elements.catalogDisplayStatus, `${layoutLabels[uiState.layout]} · ${hiddenCount ? `${hiddenCount}개 정보 숨김` : "전체 정보 표시"}${uiState.reducedMotion ? " · 모션 최소화" : ""}`);
    }

    function toggleScrollTopVisibility() {
        elements.scrollTopButton?.classList.toggle("is-visible", window.scrollY > 480);
    }

    function syncScrollState() {
        toggleScrollTopVisibility();
        const scrollableHeight = document.documentElement.scrollHeight - window.innerHeight;
        const progress = scrollableHeight > 0 ? Math.min(100, Math.max(0, (window.scrollY / scrollableHeight) * 100)) : 0;
        if (elements.scrollProgress) {
            elements.scrollProgress.style.transform = `scaleX(${progress / 100})`;
        }
    }

    async function copyTextWithFeedback(text, title, body) {
        try {
            if (navigator.clipboard?.writeText) {
                await navigator.clipboard.writeText(text);
            } else {
                throw new Error("Clipboard not available");
            }
            showToast(title, body);
        } catch (error) {
            window.prompt("내용을 복사하세요.", text);
        }
    }

    async function copyProductCollection(items, heading, successTitle) {
        const text = items.length
            ? `${heading}\n${items.map((product, index) => `${index + 1}. ${product.headline || product.name} · ${product.priceLabel || formatPrice(product.price)} · 재고 ${product.stock}개`).join("\n")}`
            : `${heading} 상품이 없습니다.`;
        await copyTextWithFeedback(text, successTitle, "상품 가격과 재고 상태를 한 번에 전달할 수 있습니다.");
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

    function syncPresetButtons() {
        const activePreset = currentPreset();
        elements.catalogPresetStrip?.querySelectorAll("[data-preset]").forEach((button) => {
            button.classList.toggle("is-active", button.dataset.preset === activePreset);
        });
    }

    function currentPreset() {
        if (state.featuredOnly === "FEATURED" && state.sort === "FEATURED") {
            return "FEATURED";
        }
        if (state.stock === "LOW" && state.sort === "STOCK_ASC") {
            return "LOW_STOCK";
        }
        if (state.priceBand === "OVER_300" && state.sort === "PRICE_HIGH") {
            return "PREMIUM";
        }
        if (!Object.entries(state).some(([key, value]) => value !== DEFAULT_STATE[key])) {
            return "RESET";
        }
        if (state.sort === "LATEST") {
            return "LATEST_DROP";
        }
        return "";
    }

    function initSectionNavigation() {
        const navLinks = Array.from(document.querySelectorAll(".topbar-subnav a[href^=\"#\"]"));
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
            setText(elements.storefrontStatus, activeLink ? `${activeLink.textContent.trim()} 영역을 보고 있습니다.` : "");
        }, {
            rootMargin: "-25% 0px -55% 0px",
            threshold: [0.2, 0.45, 0.7]
        });
        sections.forEach(({ section }) => observer.observe(section));
    }

    function productVisualMarkup(product, className, options = {}) {
        product = markupSafeObject(product);
        const thumbnail = String(product.thumbnailUrl || "").trim();
        const usesFallback = !thumbnail || thumbnail === PRODUCT_IMAGE_FALLBACK_URL;
        const imageSource = thumbnail || PRODUCT_IMAGE_FALLBACK_URL;
        const visualLabel = `${product.name || product.headline || "상품"} ${usesFallback ? "대체 이미지" : "이미지"}`;
        return `
            <div class="${className} product-visual--has-image${usesFallback ? " is-image-fallback" : ""}">
                <img class="product-visual__image" src="${escapeAttribute(imageSource)}" alt="${escapeAttribute(visualLabel)}" loading="${options.eager ? "eager" : "lazy"}" decoding="async" ${options.eager ? 'fetchpriority="high"' : ""} ${usesFallback ? 'data-image-fallback="true"' : ""} data-product-image>
                <span class="${className}-badge">${brandInitials(product.brand)}</span>
                <div class="${className}-copy">
                    <strong>${product.category || "Curated"}</strong>
                    <span>${product.model || product.name || "-"}</span>
                </div>
            </div>
        `;
    }

    function handleCatalogCardNavigation(event) {
        const card = event.target.closest(".catalog-card[role=\"listitem\"]");
        if (!card || event.target !== card) {
            return;
        }
        const productId = Number(card.dataset.catalogProductId);
        const shortcutKey = event.key.toLowerCase();
        if (["b", "c", "s", "q", "l"].includes(shortcutKey) && !event.altKey && !event.ctrlKey && !event.metaKey) {
            event.preventDefault();
            if (shortcutKey === "b") {
                toggleBookmarkProduct(productId);
                restoreCatalogCardFocus(productId);
            } else if (shortcutKey === "c") {
                toggleCompareProduct(productId);
                restoreCatalogCardFocus(productId);
            } else if (shortcutKey === "s") {
                toggleSelectedProduct(productId);
                restoreCatalogCardFocus(productId);
            } else if (shortcutKey === "q") {
                openDrawer(productId);
            } else {
                copyFocusedCatalogProductLink(productId);
            }
            return;
        }
        if (event.key === "Enter") {
            const link = card.querySelector(".catalog-card__visual-link");
            if (link) {
                event.preventDefault();
                window.location.href = link.href;
            }
            return;
        }
        if (event.key === "PageUp" || event.key === "PageDown") {
            event.preventDefault();
            moveCatalogPage(paginationState.page + (event.key === "PageDown" ? 1 : -1));
            return;
        }
        if (!["ArrowLeft", "ArrowRight", "ArrowUp", "ArrowDown", "Home", "End"].includes(event.key)) {
            return;
        }
        const cards = Array.from(elements.catalogGrid.querySelectorAll(".catalog-card[role=\"listitem\"]"));
        const currentIndex = cards.indexOf(card);
        const columnCount = Math.max(1, window.getComputedStyle(elements.catalogGrid).gridTemplateColumns.split(" ").length);
        const offsets = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -columnCount, ArrowDown: columnCount };
        const nextIndex = event.key === "Home"
            ? 0
            : event.key === "End"
                ? cards.length - 1
                : Math.min(cards.length - 1, Math.max(0, currentIndex + offsets[event.key]));
        event.preventDefault();
        cards.forEach((item, index) => item.tabIndex = index === nextIndex ? 0 : -1);
        cards[nextIndex]?.focus();
        const focusedProduct = products.find((product) => Number(product.id) === Number(cards[nextIndex]?.dataset.catalogProductId));
        announceStorefrontStatus(`${nextIndex + 1} / ${cards.length}번째 ${focusedProduct?.name || "상품"} 카드입니다.`);
    }

    function restoreCatalogCardFocus(productId) {
        window.requestAnimationFrame(() => {
            const card = elements.catalogGrid?.querySelector(`[data-catalog-product-id="${productId}"]`);
            if (!card) {
                return;
            }
            elements.catalogGrid.querySelectorAll(".catalog-card[role=\"listitem\"]")
                .forEach((item) => item.tabIndex = item === card ? 0 : -1);
            card.focus();
        });
    }

    function focusCatalogCardAfterRender() {
        window.requestAnimationFrame(() => {
            const firstCard = elements.catalogGrid?.querySelector(".catalog-card[role=\"listitem\"]");
            if (firstCard) {
                firstCard.tabIndex = 0;
                firstCard.focus({ preventScroll: true });
            }
        });
    }

    function openCatalogFilterPanel() {
        if (!elements.catalogFilterPanel) {
            return;
        }
        elements.catalogFilterPanel.open = true;
        elements.catalogFilterPanel.querySelector("select")?.focus();
        announceStorefrontStatus("상품 필터를 열었습니다.");
    }

    function closeCatalogFilterPanel() {
        if (!elements.catalogFilterPanel?.open) {
            return;
        }
        elements.catalogFilterPanel.open = false;
        elements.catalogFilterPanel.querySelector("summary")?.focus();
        announceStorefrontStatus("상품 필터를 닫았습니다.");
    }

    function persistCatalogFilterPanelState() {
        try {
            window.sessionStorage.setItem(FILTER_PANEL_OPEN_KEY, String(Boolean(elements.catalogFilterPanel?.open)));
        } catch (error) {
            // 세션 저장이 제한된 환경에서는 기본 접힘 상태를 유지한다.
        }
    }

    function restoreCatalogFilterPanelState() {
        try {
            if (elements.catalogFilterPanel) {
                elements.catalogFilterPanel.open = window.sessionStorage.getItem(FILTER_PANEL_OPEN_KEY) === "true";
            }
        } catch (error) {
            // 저장 상태 복원 실패는 필터 기능 자체에 영향을 주지 않는다.
        }
    }

    function syncCatalogDocumentTitle(resultCount) {
        const context = state.search ? `“${state.search}” 검색` : state.brand !== "ALL" ? state.brand : "상품 탐색";
        document.title = `${context} ${resultCount}개 | NOREN`;
    }

    async function copyFocusedCatalogProductLink(productId) {
        const product = products.find((item) => Number(item.id) === Number(productId));
        if (!product) {
            return;
        }
        const url = new URL(detailPageUrl(productId), window.location.origin).href;
        await copyTextWithFeedback(url, "상품 링크를 복사했습니다.", `${product.headline || product.name} 상세 주소를 전달할 수 있습니다.`);
        restoreCatalogCardFocus(productId);
    }

    function warmCatalogProductDetail(event) {
        const card = event.target.closest?.(".catalog-card[role=\"listitem\"]");
        const productId = Number(card?.querySelector("[data-product-id]")?.dataset.productId);
        if (productId > 0 && !window.navigator.connection?.saveData) {
            loadProductDetail(productId).catch(() => {});
        }
    }

    function writeCatalogSessionCache(payload) {
        try {
            window.sessionStorage.setItem(CATALOG_CACHE_KEY, JSON.stringify(payload));
        } catch (error) {
            // 저장 공간이 제한된 환경에서도 최신 응답 렌더링은 계속한다.
        }
    }

    function readCatalogSessionCache() {
        try {
            const payload = JSON.parse(window.sessionStorage.getItem(CATALOG_CACHE_KEY) || "null");
            return normalizeCatalogPayload(payload, true);
        } catch (error) {
            return null;
        }
    }

    function persistCatalogScrollPosition() {
        try {
            window.sessionStorage.setItem(SCROLL_POSITION_KEY, String(Math.max(0, Math.round(window.scrollY))));
        } catch (error) {
            // 세션 저장이 차단된 브라우저에서는 기본 스크롤 동작을 유지한다.
        }
    }

    function restoreCatalogScrollPosition() {
        try {
            const savedPosition = Number(window.sessionStorage.getItem(SCROLL_POSITION_KEY));
            if (Number.isSafeInteger(savedPosition) && savedPosition > 0 && savedPosition <= 1000000) {
                window.requestAnimationFrame(() => window.scrollTo({ top: savedPosition, behavior: "instant" }));
            }
        } catch (error) {
            // 복원 실패는 화면 초기화에 영향을 주지 않는다.
        }
    }

    function handleProductImageError(event) {
        if (!event.target.matches?.("[data-product-image]")) {
            return;
        }
        const visual = event.target.closest(".product-visual--has-image");
        if (event.target.dataset.imageFallback === "true") {
            visual?.classList.add("is-image-error");
            event.target.remove();
            return;
        }
        event.target.dataset.imageFallback = "true";
        event.target.src = PRODUCT_IMAGE_FALLBACK_URL;
        event.target.alt = `${event.target.alt || "상품"} 대체 이미지`;
        visual?.classList.add("is-image-fallback");
    }

    function escapeAttribute(value) {
        return String(value)
            .replaceAll("&", "&amp;")
            .replaceAll("\"", "&quot;")
            .replaceAll("'", "&#39;")
            .replaceAll("<", "&lt;")
            .replaceAll(">", "&gt;");
    }

    function brandInitials(brand) {
        return String(brand || "GS")
            .trim()
            .split(/\s+/)
            .map((token) => token.charAt(0))
            .join("")
            .slice(0, 2)
            .toUpperCase();
    }

    function bindProductButtons(container) {
        container.querySelectorAll("[data-product-id]:not([data-card-focus])").forEach((button) => {
            if (button.dataset.previewBound === "true") {
                return;
            }
            button.dataset.previewBound = "true";
            button.addEventListener("click", () => openDrawer(Number(button.dataset.productId)));
        });
    }

    async function handleFlowAction(action) {
        const recentProducts = readRecentProducts();
        const comparedProducts = readCompareProducts();
        const bookmarkedProducts = readBookmarkProducts();
        const hiddenProducts = readHiddenProducts();

        if (action === "RECENT") {
            if (!recentProducts.length) {
                showToast("최근 본 상품이 없습니다.", "먼저 상세 페이지를 둘러본 뒤 다시 시도해주세요.", true);
                return;
            }
            openDrawer(Number(recentProducts[0].id));
            showToast("최근 본 상품을 열었습니다.", `${recentProducts[0].headline || recentProducts[0].name}부터 탐색을 다시 시작합니다.`);
            return;
        }
        if (action === "COMPARE") {
            if (!comparedProducts.length) {
                showToast("비교 보드가 비어 있습니다.", "카탈로그에서 비교 담기를 먼저 사용해주세요.", true);
                return;
            }
            document.getElementById("compareBoardSection")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("비교 보드로 이동합니다.", "담아둔 상품의 가격과 재고 차이를 바로 확인할 수 있습니다.");
            return;
        }
        if (action === "BOOKMARK") {
            if (!bookmarkedProducts.length) {
                showToast("관심 상품이 없습니다.", "카탈로그나 상세에서 찜하기를 먼저 사용해주세요.", true);
                return;
            }
            document.getElementById("bookmarkBoardSection")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("관심 상품 보드로 이동합니다.", "저장한 상품을 다시 비교하거나 상세로 이어갈 수 있습니다.");
            return;
        }
        if (action === "HIDDEN") {
            if (!hiddenProducts.length) {
                showToast("숨긴 상품이 없습니다.", "카탈로그에서 숨기기를 사용하면 이 흐름을 관리할 수 있습니다.", true);
                return;
            }
            uiState.showHiddenProducts = true;
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("숨긴 상품 보기를 켰습니다.", "카탈로그에서 제외했던 상품도 다시 확인할 수 있습니다.");
            return;
        }
        if (action === "LOW_STOCK") {
            const boardProducts = recentProducts.concat(comparedProducts, bookmarkedProducts);
            state.stock = "LOW";
            state.sort = "STOCK_ASC";
            const leadBrand = dominantBrand(boardProducts);
            if (leadBrand) {
                state.brand = leadBrand;
            }
            syncControls();
            await refreshCatalog();
            document.getElementById("catalog")?.scrollIntoView({ behavior: "smooth", block: "start" });
            showToast("개인 보드 기준 긴장 재고 탐색을 적용했습니다.", leadBrand ? `${leadBrand} 중심으로 저재고 상품을 다시 정렬했습니다.` : "저재고 상품부터 빠르게 다시 탐색할 수 있습니다.");
            return;
        }
        if (action === "RESTORE_HIDDEN") {
            if (!hiddenProducts.length) {
                showToast("복구할 숨김 상품이 없습니다.", "현재는 전체 상품이 그대로 노출되고 있습니다.", true);
                return;
            }
            window.localStorage.removeItem(HIDDEN_PRODUCTS_KEY);
            uiState.showHiddenProducts = false;
            await refreshCatalog();
            showToast("숨김 상품을 모두 복구했습니다.", "기본 카탈로그에서 다시 전체 상품을 확인할 수 있습니다.");
        }
    }

    async function openDrawer(productId) {
        if (!elements.productDrawer || !elements.drawerBody) {
            return;
        }
        productId = Number(productId);
        if (!Number.isSafeInteger(productId) || productId <= 0) {
            showToast("상품을 열 수 없습니다.", "올바른 상품 번호를 다시 선택해주세요.", true);
            return;
        }

        closeShortcutHelp();
        closeHeaderSearch();
        closeMobileMenu();

        const isNewDrawerSession = !elements.productDrawer.classList.contains("is-open");
        if (isNewDrawerSession) {
            drawerReturnFocus = document.activeElement instanceof HTMLElement ? document.activeElement : null;
        }
        const requestSequence = ++drawerRequestSequence;
        drawerRequestController?.abort();
        drawerRequestController = new AbortController();
        const requestController = drawerRequestController;
        safeStorageWrite(window.localStorage, LAST_DRAWER_PRODUCT_KEY, String(productId));
        const list = filteredProducts();
        const currentIndex = list.findIndex((item) => Number(item.id) === Number(productId));
        const previousProduct = currentIndex > 0 ? list[currentIndex - 1] : null;
        const nextProduct = currentIndex >= 0 && currentIndex < list.length - 1 ? list[currentIndex + 1] : null;
        activeDrawerProductId = Number(productId);
        previousDrawerProductId = Number(previousProduct?.id || 0);
        nextDrawerProductId = Number(nextProduct?.id || 0);
        elements.productDrawer.classList.add("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "false");
        elements.productDrawer.setAttribute("aria-busy", "true");
        setDrawerBackgroundInert(true);
        syncBodyLayerState();
        elements.drawerBody.innerHTML = `
            <p class="eyebrow">Detail</p>
            <h3 id="drawerTitle">상품 상세를 불러오는 중입니다.</h3>
            <p class="product-drawer__description" id="drawerStatus">선택한 상품 데이터를 확인하고 있습니다.</p>
        `;

        try {
            const product = markupSafeObject(await loadProductDetail(productId, requestController.signal));
            if (requestSequence !== drawerRequestSequence || !elements.productDrawer.classList.contains("is-open")) {
                return;
            }
            const filteredOptions = filteredDrawerOptions(product);
            const filteredRelatedProducts = filteredDrawerRelatedProducts(product);

            elements.drawerBody.innerHTML = `
            <p class="eyebrow">Detail</p>
            ${productVisualMarkup(product, "product-drawer__visual")}
            <div class="product-drawer__meta">
                <span class="product-drawer__pill ${stockClassName(product.stock)}">${product.stockStatus || stockLabel(product.stock)}</span>
                <span class="product-drawer__pill is-stable-stock">${product.brand}</span>
                ${product.featured ? `<span class="product-drawer__pill">Featured${product.featuredRank ? ` #${product.featuredRank}` : ''}</span>` : ''}
            </div>
            <h3 id="drawerTitle">${product.headline || product.name}</h3>
            <div class="product-drawer__meta">
                <span>${product.name}</span>
                <span>${product.model}</span>
                <span>${product.category}</span>
            </div>
            <p class="product-drawer__description" id="drawerStatus">${product.description}</p>
            <div class="product-drawer__group">
                <div class="product-drawer__overview">
                    <div class="product-drawer__overview-card">
                        <span>카테고리</span>
                        <strong>${product.category}</strong>
                    </div>
                    <div class="product-drawer__overview-card">
                        <span>모델</span>
                        <strong>${product.model}</strong>
                    </div>
                    <div class="product-drawer__overview-card">
                        <span>등록일</span>
                        <strong>${product.createdDate}</strong>
                    </div>
                    ${drawerInsightCards(product, filteredOptions, filteredRelatedProducts)}
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>발매가</strong>
                <h3>${product.priceLabel || formatPrice(product.price)}</h3>
                <p class="product-drawer__description">현재 총 재고 ${product.stock}개 · 무드 키워드 ${product.mood}</p>
                <div class="product-drawer__cta product-drawer__cta-group">
                    <a class="catalog-card__button product-drawer__cta-link" href="${detailPageUrl(product.id)}">상세 페이지 이동</a>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-share-id="${product.id}">
                        링크 공유
                    </button>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-bookmark-id="${product.id}">
                        ${isBookmarkedProduct(product.id) ? "찜 해제" : "찜하기"}
                    </button>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-compare-id="${product.id}">
                        ${isComparedProduct(product.id) ? "비교 해제" : "비교 담기"}
                    </button>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-copy-id="${product.id}">
                        요약 복사
                    </button>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-hide-id="${product.id}">
                        ${isHiddenProduct(product.id) ? "숨김 해제" : "숨기기"}
                    </button>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>빠른 이동</strong>
                <div class="product-drawer__cta product-drawer__cta-group">
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-prev-id="${previousProduct?.id || ""}" ${previousProduct ? "" : "disabled"}>
                        이전 상품
                    </button>
                    <button class="catalog-reset-button product-drawer__cta-link" type="button" data-drawer-next-id="${nextProduct?.id || ""}" ${nextProduct ? "" : "disabled"}>
                        다음 상품
                    </button>
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>사이즈별 재고</strong>
                <div class="product-drawer__toolbar">
                    <button class="catalog-reset-button ${drawerState.optionLowStockOnly ? "is-active" : ""}" type="button" data-drawer-option-low-stock-toggle="${product.id}">
                        ${drawerState.optionLowStockOnly ? "전체 옵션 보기" : "긴장 옵션만 보기"}
                    </button>
                    <button class="catalog-reset-button ${drawerState.optionAvailableOnly ? "is-active" : ""}" type="button" data-drawer-option-available-toggle="${product.id}">
                        ${drawerState.optionAvailableOnly ? "품절 포함 보기" : "구매 가능만"}
                    </button>
                    <button class="catalog-reset-button ${drawerState.optionStableOnly ? "is-active" : ""}" type="button" data-drawer-option-stable-toggle="${product.id}">
                        ${drawerState.optionStableOnly ? "전체 재고 보기" : "안정 재고만"}
                    </button>
                    <button class="catalog-reset-button ${drawerState.optionSort === "STOCK_ASC" ? "is-active" : ""}" type="button" data-drawer-option-sort="STOCK_ASC">재고 낮은 순</button>
                    <button class="catalog-reset-button ${drawerState.optionSort === "STOCK_DESC" ? "is-active" : ""}" type="button" data-drawer-option-sort="STOCK_DESC">재고 높은 순</button>
                    <button class="catalog-reset-button" type="button" data-drawer-option-recommend-id="${product.id}">추천 옵션 상세</button>
                    <button class="catalog-reset-button" type="button" data-drawer-copy-options-id="${product.id}">
                        옵션 요약 복사
                    </button>
                </div>
                <div class="product-drawer__options">
                    ${filteredOptions.length ? filteredOptions.map((option) => `
                        <div class="product-drawer__option">
                            <div>
                                <strong>${option.name}</strong>
                                <span>${stockLabel(option.stock)}</span>
                            </div>
                            <strong>${option.stock}개</strong>
                        </div>
                    `).join("") : `
                        <div class="product-drawer__option">
                            <div>
                                <strong>${drawerState.optionLowStockOnly || drawerState.optionAvailableOnly || drawerState.optionStableOnly ? "조건에 맞는 옵션이 없습니다." : "등록된 옵션이 없습니다."}</strong>
                                <span>${drawerState.optionLowStockOnly || drawerState.optionAvailableOnly || drawerState.optionStableOnly ? "옵션 필터를 해제해 전체 재고를 확인해주세요." : "세부 옵션 정보가 아직 준비되지 않았습니다."}</span>
                            </div>
                            <strong>-</strong>
                        </div>
                    `}
                </div>
            </div>
            <div class="product-drawer__group">
                <strong>연관 상품</strong>
                <div class="product-drawer__toolbar">
                    <button class="catalog-reset-button ${drawerState.relatedSort === "STOCK_ASC" ? "is-active" : ""}" type="button" data-drawer-related-sort="STOCK_ASC">
                        재고 낮은 순
                    </button>
                    <button class="catalog-reset-button ${drawerState.relatedSort === "PRICE_HIGH" ? "is-active" : ""}" type="button" data-drawer-related-sort="PRICE_HIGH">
                        가격 높은 순
                    </button>
                    <button class="catalog-reset-button ${drawerState.relatedSameBrandOnly ? "is-active" : ""}" type="button" data-drawer-related-brand-toggle="${product.id}">
                        ${drawerState.relatedSameBrandOnly ? "전체 브랜드 보기" : "같은 브랜드만"}
                    </button>
                    <button class="catalog-reset-button ${drawerState.relatedAvailableOnly ? "is-active" : ""}" type="button" data-drawer-related-available-toggle="${product.id}">
                        ${drawerState.relatedAvailableOnly ? "품절 포함 보기" : "구매 가능만"}
                    </button>
                    <button class="catalog-reset-button ${drawerState.relatedSameCategoryOnly ? "is-active" : ""}" type="button" data-drawer-related-category-toggle="${product.id}">
                        ${drawerState.relatedSameCategoryOnly ? "전체 카테고리 보기" : "같은 카테고리만"}
                    </button>
                    <button class="catalog-reset-button" type="button" data-drawer-related-cheapest-id="${product.id}">최저가 열기</button>
                    <button class="catalog-reset-button" type="button" data-drawer-related-highest-stock-id="${product.id}">최고재고 열기</button>
                    <button class="catalog-reset-button" type="button" data-drawer-related-compare-all="${product.id}">연관 비교 담기</button>
                    <button class="catalog-reset-button" type="button" data-drawer-related-random-id="${product.id}">
                        랜덤 연관 보기
                    </button>
                    <button class="catalog-reset-button" type="button" data-drawer-copy-related-id="${product.id}">
                        연관 요약 복사
                    </button>
                </div>
                ${filteredRelatedProducts.length ? `
                <div class="product-drawer__related-list">
                    ${filteredRelatedProducts.map((related) => `
                        <button class="product-drawer__related-card" type="button" data-product-id="${related.id}">
                            ${productVisualMarkup(related, "product-drawer__related-visual")}
                            <span class="product-drawer__related-brand">${related.brand}</span>
                            <strong>${related.name}</strong>
                            <span class="product-drawer__related-meta">${related.reason} · ${related.model} · ${related.priceLabel || formatPrice(related.price)} · ${related.stockStatus || stockLabel(related.stock)}</span>
                        </button>
                    `).join("")}
                </div>
                ` : `
                <div class="product-drawer__option">
                    <div>
                        <strong>${drawerState.relatedSameBrandOnly ? "같은 브랜드 기준 연관 상품이 없습니다." : "연관 상품이 없습니다."}</strong>
                        <span>${drawerState.relatedSameBrandOnly ? "브랜드 필터를 해제하면 더 많은 연관 상품을 볼 수 있습니다." : "추천 흐름이 준비되면 이 영역에 표시됩니다."}</span>
                    </div>
                    <strong>-</strong>
                </div>
                `}
            </div>
        `;
            elements.drawerBody.querySelector("[data-drawer-share-id]")?.addEventListener("click", async () => {
                const shareUrl = `${window.location.origin}${detailPageUrl(product.id)}`;
                await copyTextWithFeedback(shareUrl, "상세 링크를 복사했습니다.", "현재 보고 있는 상품 상세 링크를 바로 전달할 수 있습니다.");
            });
            elements.drawerBody.querySelector("[data-drawer-bookmark-id]")?.addEventListener("click", () => {
                toggleBookmarkProduct(product.id);
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-compare-id]")?.addEventListener("click", () => {
                toggleCompareProduct(product.id);
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-copy-id]")?.addEventListener("click", async () => {
                const text = summaryTextForDrawer(product);
                await copyTextWithFeedback(text, "상품 요약을 복사했습니다.", "드로어에서 보고 있던 상품 정보를 바로 전달할 수 있습니다.");
            });
            elements.drawerBody.querySelector("[data-drawer-option-low-stock-toggle]")?.addEventListener("click", () => {
                drawerState.optionLowStockOnly = !drawerState.optionLowStockOnly;
                drawerState.optionAvailableOnly = false;
                drawerState.optionStableOnly = false;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-option-available-toggle]")?.addEventListener("click", () => {
                drawerState.optionAvailableOnly = !drawerState.optionAvailableOnly;
                drawerState.optionLowStockOnly = false;
                drawerState.optionStableOnly = false;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-option-stable-toggle]")?.addEventListener("click", () => {
                drawerState.optionStableOnly = !drawerState.optionStableOnly;
                drawerState.optionLowStockOnly = false;
                drawerState.optionAvailableOnly = false;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelectorAll("[data-drawer-option-sort]").forEach((button) => {
                button.addEventListener("click", () => {
                    const nextSort = button.dataset.drawerOptionSort;
                    drawerState.optionSort = drawerState.optionSort === nextSort ? "DEFAULT" : nextSort;
                    openDrawer(product.id);
                });
            });
            elements.drawerBody.querySelector("[data-drawer-option-recommend-id]")?.addEventListener("click", () => {
                const recommended = filteredOptions.filter((option) => Number(option.stock || 0) > 0)
                    .sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0))[0];
                if (!recommended) {
                    showToast("추천할 옵션이 없습니다.", "옵션 필터를 넓혀 구매 가능한 재고를 확인해주세요.", true);
                    return;
                }
                const detailUrl = new URL(detailPageUrl(product.id), window.location.origin);
                detailUrl.searchParams.set("option", recommended.name);
                window.location.href = `${detailUrl.pathname}${detailUrl.search}`;
            });
            elements.drawerBody.querySelector("[data-drawer-copy-options-id]")?.addEventListener("click", async () => {
                const text = drawerOptionSummaryText(product, filteredOptions);
                await copyTextWithFeedback(text, "옵션 요약을 복사했습니다.", "현재 보이는 옵션 재고 상태를 바로 전달할 수 있습니다.");
            });
            elements.drawerBody.querySelectorAll("[data-drawer-related-sort]").forEach((button) => {
                button.addEventListener("click", () => {
                    const nextSort = button.dataset.drawerRelatedSort;
                    drawerState.relatedSort = drawerState.relatedSort === nextSort ? "DEFAULT" : nextSort;
                    openDrawer(product.id);
                });
            });
            elements.drawerBody.querySelector("[data-drawer-related-brand-toggle]")?.addEventListener("click", () => {
                drawerState.relatedSameBrandOnly = !drawerState.relatedSameBrandOnly;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-related-available-toggle]")?.addEventListener("click", () => {
                drawerState.relatedAvailableOnly = !drawerState.relatedAvailableOnly;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-related-category-toggle]")?.addEventListener("click", () => {
                drawerState.relatedSameCategoryOnly = !drawerState.relatedSameCategoryOnly;
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-related-cheapest-id]")?.addEventListener("click", () => {
                const cheapest = filteredRelatedProducts.slice().sort((left, right) => Number(left.price || 0) - Number(right.price || 0))[0];
                if (cheapest) {
                    openDrawer(Number(cheapest.id));
                }
            });
            elements.drawerBody.querySelector("[data-drawer-related-highest-stock-id]")?.addEventListener("click", () => {
                const highestStock = filteredRelatedProducts.slice().sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0))[0];
                if (highestStock) {
                    openDrawer(Number(highestStock.id));
                }
            });
            elements.drawerBody.querySelector("[data-drawer-related-compare-all]")?.addEventListener("click", () => {
                addProductsToBoard(filteredRelatedProducts, "COMPARE");
            });
            elements.drawerBody.querySelector("[data-drawer-related-random-id]")?.addEventListener("click", () => {
                if (!filteredRelatedProducts.length) {
                    showToast("열 수 있는 연관 상품이 없습니다.", "정렬이나 브랜드 조건을 바꿔 다시 시도해주세요.", true);
                    return;
                }
                const randomRelated = filteredRelatedProducts[Math.floor(Math.random() * filteredRelatedProducts.length)];
                openDrawer(Number(randomRelated.id));
                showToast("랜덤 연관 상품을 열었습니다.", `${randomRelated.name} 상세를 이어서 확인할 수 있습니다.`);
            });
            elements.drawerBody.querySelector("[data-drawer-copy-related-id]")?.addEventListener("click", async () => {
                const text = drawerRelatedSummaryText(product, filteredRelatedProducts);
                await copyTextWithFeedback(text, "연관 상품 요약을 복사했습니다.", "현재 보이는 추천 상품 흐름을 바로 전달할 수 있습니다.");
            });
            elements.drawerBody.querySelector("[data-drawer-hide-id]")?.addEventListener("click", async () => {
                toggleHiddenProduct(product.id);
                await refreshCatalog();
                if (isHiddenProduct(product.id) && !uiState.showHiddenProducts) {
                    closeDrawer();
                    return;
                }
                openDrawer(product.id);
            });
            elements.drawerBody.querySelector("[data-drawer-prev-id]")?.addEventListener("click", () => {
                if (previousProduct) {
                    openDrawer(previousProduct.id);
                }
            });
            elements.drawerBody.querySelector("[data-drawer-next-id]")?.addEventListener("click", () => {
                if (nextProduct) {
                    openDrawer(nextProduct.id);
                }
            });
            bindProductButtons(elements.drawerBody);
            elements.productDrawer.setAttribute("aria-busy", "false");
            if (isNewDrawerSession) {
                elements.productDrawer.querySelector(".product-drawer__panel")?.focus();
            }
        } catch (error) {
            if (error?.name === "AbortError") {
                return;
            }
            if (requestSequence !== drawerRequestSequence || !elements.productDrawer.classList.contains("is-open")) {
                return;
            }
            elements.drawerBody.innerHTML = `
                <p class="eyebrow">Detail</p>
                <h3 id="drawerTitle">상품 상세를 불러오지 못했습니다.</h3>
                <p class="product-drawer__description" id="drawerStatus">잠시 후 다시 시도해주세요.</p>
                <button class="catalog-reset-button" type="button" data-drawer-retry-id="${productId}">다시 시도</button>
            `;
            elements.drawerBody.querySelector("[data-drawer-retry-id]")?.addEventListener("click", () => openDrawer(productId));
            elements.productDrawer.setAttribute("aria-busy", "false");
            elements.drawerBody.querySelector("[data-drawer-retry-id]")?.focus();
        } finally {
            if (drawerRequestController === requestController) {
                drawerRequestController = null;
            }
        }
    }

    function setDrawerBackgroundInert(isInert) {
        [document.querySelector(".page-shell"), elements.mobileStoreNav, elements.scrollTopButton]
            .filter(Boolean)
            .forEach((element) => {
                element.inert = isInert;
            });
    }

    function normalizeRelatedProducts(value, productId) {
        if (!Array.isArray(value) || value.length > 8) throw new Error("연관 상품 목록이 올바르지 않습니다.");
        const ids = new Set();
        return value.map((item) => {
            const id = homeInteger(item?.id, "연관 상품 번호", 1);
            if (id === productId || ids.has(id)) throw new Error("연관 상품이 중복되었거나 자기 자신을 참조합니다.");
            ids.add(id);
            const stock = homeInteger(item.stock, "연관 상품 재고");
            const price = homeInteger(item.price, "연관 상품 가격");
            return {
                id,
                brand: homeText(item.brand, 100, true),
                category: homeText(item.category, 100, true),
                name: homeText(item.name, 200, true),
                reason: homeText(item.reason, 300, true),
                model: homeText(item.model, 100),
                price,
                stock,
                stockStatus: homeText(item.stockStatus, 40) || stockLabel(stock),
                priceLabel: formatPrice(price),
                thumbnailUrl: homeImage(item.thumbnailUrl)
            };
        });
    }

    function normalizeHomeProductDetail(value, expectedId) {
        const product = normalizeHomeProduct(value);
        if (product.id !== expectedId) throw new Error("요청 상품과 상세 응답이 일치하지 않습니다.");
        return { ...product, relatedProducts: normalizeRelatedProducts(value.relatedProducts, expectedId) };
    }

    async function loadProductDetail(productId, signal) {
        if (detailCache.has(productId)) {
            return detailCache.get(productId);
        }
        const response = await fetch(`/api/front/products/${productId}`, { signal });
        if (!response.ok) {
            throw new Error("상품 상세를 불러오지 못했습니다.");
        }
        const product = normalizeHomeProductDetail(await response.json(), productId);
        if (detailCache.size >= 20) {
            detailCache.delete(detailCache.keys().next().value);
        }
        detailCache.set(productId, product);
        return product;
    }

    function closeDrawer() {
        if (!elements.productDrawer?.classList.contains("is-open")) {
            return;
        }
        drawerRequestSequence += 1;
        drawerRequestController?.abort();
        drawerRequestController = null;
        elements.productDrawer.classList.remove("is-open");
        elements.productDrawer.setAttribute("aria-hidden", "true");
        elements.productDrawer.setAttribute("aria-busy", "false");
        setDrawerBackgroundInert(false);
        syncBodyLayerState();
        if (drawerReturnFocus?.isConnected) {
            drawerReturnFocus.focus();
        }
        drawerReturnFocus = null;
        activeDrawerProductId = 0;
        previousDrawerProductId = 0;
        nextDrawerProductId = 0;
    }

    function handleDrawerKeyboardShortcut(event) {
        if (!elements.productDrawer?.classList.contains("is-open")) {
            return false;
        }
        const isEditable = ["INPUT", "TEXTAREA", "SELECT"].includes(event.target?.tagName) || event.target?.isContentEditable;
        if (isEditable || event.altKey || event.ctrlKey || event.metaKey) {
            return false;
        }
        const shortcutKey = event.key.toLowerCase();
        if (event.key === "ArrowLeft" && previousDrawerProductId) {
            event.preventDefault();
            openDrawer(previousDrawerProductId);
            return true;
        }
        if (event.key === "ArrowRight" && nextDrawerProductId) {
            event.preventDefault();
            openDrawer(nextDrawerProductId);
            return true;
        }
        if (!["b", "c", "l"].includes(shortcutKey) || !activeDrawerProductId) {
            return false;
        }
        event.preventDefault();
        if (shortcutKey === "b") {
            toggleBookmarkProduct(activeDrawerProductId);
            openDrawer(activeDrawerProductId);
        } else if (shortcutKey === "c") {
            toggleCompareProduct(activeDrawerProductId);
            openDrawer(activeDrawerProductId);
        } else {
            const product = products.find((item) => Number(item.id) === activeDrawerProductId);
            const url = new URL(detailPageUrl(activeDrawerProductId), window.location.origin).href;
            copyTextWithFeedback(url, "상품 링크를 복사했습니다.", `${product?.headline || product?.name || "현재 상품"} 상세 주소입니다.`);
        }
        return true;
    }

    function keepFocusInsideDrawer(event) {
        const focusable = Array.from(elements.productDrawer.querySelectorAll(
            'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), [tabindex]:not([tabindex="-1"])'
        )).filter((element) => !element.hidden && element.getClientRects().length);
        if (!focusable.length) {
            event.preventDefault();
            elements.productDrawer.querySelector(".product-drawer__panel")?.focus();
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

    function stockLabel(stock) {
        return stock < lowStockThresholdValue() ? "품절 임박" : "재고 안정";
    }

    function stockClassName(stock) {
        return stock < lowStockThresholdValue() ? "is-low-stock" : "is-stable-stock";
    }

    function lowStockThresholdValue() {
        return Number(state.lowStockThreshold || 20);
    }

    function formatPrice(price) {
        return `${Number(price).toLocaleString("ko-KR")}원`;
    }

    function summaryTextForDrawer(product) {
        return [
            product.headline || product.name,
            compactProductContext(product),
            stockPressureDetail(product.stock),
            product.description
        ].filter(Boolean).join("\n");
    }

    function drawerOptionSummaryText(product, options) {
        if (!options.length) {
            return `${product.headline || product.name}\n등록된 옵션이 없습니다.`;
        }
        return [
            `${product.headline || product.name} 옵션 요약`,
            options.map((option, index) => `${index + 1}. ${option.name} · 재고 ${option.stock}개 · ${stockLabel(option.stock)}`).join("\n")
        ].join("\n");
    }

    function drawerRelatedSummaryText(product, relatedProducts) {
        if (!relatedProducts.length) {
            return `${product.headline || product.name}\n연관 상품이 없습니다.`;
        }
        return [
            `${product.headline || product.name} 연관 상품 요약`,
            relatedProducts.map((related, index) => `${index + 1}. ${related.name} · ${related.brand} · ${related.priceLabel || formatPrice(related.price)} · 재고 ${related.stock}개`).join("\n")
        ].join("\n");
    }

    function drawerInsightCards(product, options, relatedProducts) {
        const lowStockOptions = options.filter((option) => Number(option.stock || 0) < lowStockThresholdValue()).length;
        const sameBrandRelated = relatedProducts.filter((related) => related.brand === product.brand).length;
        const averageRelatedPrice = relatedProducts.length
            ? Math.round(relatedProducts.reduce((sum, related) => sum + Number(related.price || 0), 0) / relatedProducts.length)
            : 0;
        return [
            {
                label: "옵션 수",
                value: `${options.length}개`,
                description: lowStockOptions ? `긴장 옵션 ${lowStockOptions}개 포함` : "전체 옵션 흐름 확인"
            },
            {
                label: "연관 상품",
                value: `${relatedProducts.length}개`,
                description: sameBrandRelated ? `같은 브랜드 ${sameBrandRelated}개 포함` : "연관 추천 흐름 확인"
            },
            {
                label: "연관 평균가",
                value: relatedProducts.length ? formatPrice(averageRelatedPrice) : "-",
                description: relatedProducts.length ? "연관 상품 가격 레벨 비교" : "연관 상품 없음"
            },
            {
                label: "재고 압력",
                value: stockPressureLabel(product.stock),
                description: stockPressureDetail(product.stock)
            }
        ].map((item) => `
            <div class="product-drawer__overview-card product-drawer__overview-card--metric">
                <span>${item.label}</span>
                <strong>${item.value}</strong>
                <em>${item.description}</em>
            </div>
        `).join("");
    }

    function filteredDrawerOptions(product) {
        let options = Array.isArray(product.options) ? product.options.slice() : [];
        if (drawerState.optionLowStockOnly) {
            options = options.filter((option) => Number(option.stock || 0) < lowStockThresholdValue());
        }
        if (drawerState.optionAvailableOnly) {
            options = options.filter((option) => Number(option.stock || 0) > 0);
        }
        if (drawerState.optionStableOnly) {
            options = options.filter((option) => Number(option.stock || 0) >= lowStockThresholdValue());
        }
        if (drawerState.optionSort === "STOCK_ASC") {
            options.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (drawerState.optionSort === "STOCK_DESC") {
            options.sort((left, right) => Number(right.stock || 0) - Number(left.stock || 0));
        }
        return options;
    }

    function filteredDrawerRelatedProducts(product) {
        let relatedProducts = Array.isArray(product.relatedProducts) ? product.relatedProducts.slice() : [];
        if (drawerState.relatedSameBrandOnly) {
            relatedProducts = relatedProducts.filter((related) => related.brand === product.brand);
        }
        if (drawerState.relatedAvailableOnly) {
            relatedProducts = relatedProducts.filter((related) => Number(related.stock || 0) > 0);
        }
        if (drawerState.relatedSameCategoryOnly) {
            relatedProducts = relatedProducts.filter((related) => related.category === product.category);
        }
        if (drawerState.relatedSort === "STOCK_ASC") {
            relatedProducts.sort((left, right) => Number(left.stock || 0) - Number(right.stock || 0));
        }
        if (drawerState.relatedSort === "PRICE_HIGH") {
            relatedProducts.sort((left, right) => Number(right.price || 0) - Number(left.price || 0));
        }
        return relatedProducts;
    }

    function setText(element, text) {
        if (element) {
            element.textContent = text;
        }
    }

    init();
})();
