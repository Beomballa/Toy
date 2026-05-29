package com.section.admin.system.service;

import com.section.common.base.entity.type.ProductHistoryActionType;
import com.section.common.base.entity.type.YN;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.entity.DisplayBanner;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.ProductChangeHistory;
import com.section.common.commerce.entity.ProductOption;
import com.section.common.commerce.repository.BannerRepository;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.commerce.repository.ProductChangeHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.content.entity.Document;
import com.section.common.content.repository.DocumentRepository;
import com.section.common.system.entity.Account;
import com.section.common.system.entity.AdminActivityLog;
import com.section.common.system.entity.AdminOperationNotice;
import com.section.common.system.entity.AdminOperationTask;
import com.section.common.system.entity.AdminOperationTaskComment;
import com.section.common.system.entity.AdminSystemSetting;
import com.section.common.system.entity.AdminSystemSettingHistory;
import com.section.common.system.entity.AdminUser;
import com.section.common.system.entity.ApprovalDocument;
import com.section.common.system.repository.AccountRepository;
import com.section.common.system.repository.AdminActivityLogRepository;
import com.section.common.system.repository.AdminOperationNoticeRepository;
import com.section.common.system.repository.AdminOperationTaskCommentRepository;
import com.section.common.system.repository.AdminOperationTaskRepository;
import com.section.common.system.repository.AdminSystemSettingHistoryRepository;
import com.section.common.system.repository.AdminSystemSettingRepository;
import com.section.common.system.repository.AdminUserRepository;
import com.section.common.system.repository.ApprovalDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Profile("local")
@RequiredArgsConstructor
@Transactional
public class AdminLocalDataSeeder implements ApplicationRunner {

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductOptionRepository productOptionRepository;
    private final ProductChangeHistoryRepository productChangeHistoryRepository;
    private final BannerRepository bannerRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final DocumentRepository documentRepository;
    private final AccountRepository accountRepository;
    private final AdminUserRepository adminUserRepository;
    private final AdminOperationNoticeRepository adminOperationNoticeRepository;
    private final AdminOperationTaskRepository adminOperationTaskRepository;
    private final AdminOperationTaskCommentRepository adminOperationTaskCommentRepository;
    private final AdminSystemSettingRepository adminSystemSettingRepository;
    private final AdminSystemSettingHistoryRepository adminSystemSettingHistoryRepository;
    private final AdminActivityLogRepository adminActivityLogRepository;
    private final ApprovalDocumentRepository approvalDocumentRepository;

    @Override
    public void run(ApplicationArguments args) {
        seedAdminUsers();
        seedAccounts();
        seedBrands();
        seedCategories();
        seedProducts();
        seedBanners();
        seedDocuments();
        seedOrders();
        seedNotices();
        seedTasks();
        seedSystemSettings();
        seedApprovalDocuments();
        seedActivityLogs();
    }

    private void seedAdminUsers() {
        if (adminUserRepository.count() > 0) {
            return;
        }

        adminUserRepository.saveAll(List.of(
                AdminUser.builder().loginId("superadmin").password("admin1234").name("김운영").role("ROLE_SUPER").status("ACTIVE").lastLoginDtm(LocalDateTime.now().minusHours(1)).build(),
                AdminUser.builder().loginId("ops.jin").password("admin1234").name("박지은").role("ROLE_ADMIN").status("ACTIVE").lastLoginDtm(LocalDateTime.now().minusHours(5)).build(),
                AdminUser.builder().loginId("md.min").password("admin1234").name("이민수").role("ROLE_ADMIN").status("ACTIVE").lastLoginDtm(LocalDateTime.now().minusDays(1)).build(),
                AdminUser.builder().loginId("cs.han").password("admin1234").name("한소라").role("ROLE_ADMIN").status("SUSPENDED").lastLoginDtm(LocalDateTime.now().minusDays(7)).build()
        ));
    }

    private void seedAccounts() {
        if (accountRepository.count() > 0) {
            return;
        }

        List<Account> accounts = new ArrayList<>();
        accounts.add(account("mina.kim@example.com", "미나 김", "mina_k", YN.Y));
        accounts.add(account("jun.park@example.com", "박준호", "jun_pick", YN.N));
        accounts.add(account("seo.yi@example.com", "이서윤", "seoy_style", YN.N));
        accounts.add(account("do.hyun@example.com", "김도현", "runner_do", YN.N));
        accounts.add(account("ji.min@example.com", "정지민", "jimin_daily", YN.N));
        accounts.add(account("tae.woo@example.com", "최태우", "tw_sneaker", YN.N));
        accounts.add(account("ha.neul@example.com", "강하늘", "haneul_fit", YN.N));
        accounts.add(account("so.hee@example.com", "유소희", "sohee_live", YN.N));
        accountRepository.saveAll(accounts);
    }

    private Account account(String email, String name, String nickname, YN masterYn) {
        Account account = new Account();
        account.setEmail(email);
        account.setPassword("user1234");
        account.setName(name);
        account.setNickname(nickname);
        account.setMasterYn(masterYn);
        account.setInitYn(YN.N);
        account.setDelYn(YN.N);
        account.setProfileImgPath("/images/profiles/");
        account.setProfileImgName(nickname + ".png");
        account.setTmpPwIssueDt(LocalDateTime.now().minusDays(30));
        return account;
    }

    private void seedBrands() {
        if (brandRepository.count() > 0) {
            return;
        }

        brandRepository.saveAll(List.of(
                brand("나이키", "Nike", "/images/brand/nike.svg"),
                brand("아디다스", "Adidas", "/images/brand/adidas.svg"),
                brand("뉴발란스", "New Balance", "/images/brand/newbalance.svg"),
                brand("아식스", "Asics", "/images/brand/asics.svg"),
                brand("슈프림", "Supreme", "/images/brand/supreme.svg"),
                brand("스투시", "Stussy", "/images/brand/stussy.svg")
        ));
    }

    private Brand brand(String nameKo, String nameEn, String logoUrl) {
        return Brand.builder()
                .nameKo(nameKo)
                .nameEn(nameEn)
                .logoUrl(logoUrl)
                .isActive("Y")
                .build();
    }

    private void seedCategories() {
        if (categoryRepository.count() > 0) {
            return;
        }

        Category sneakers = categoryRepository.save(Category.builder().name("스니커즈").depth(1).isActive("Y").build());
        Category apparel = categoryRepository.save(Category.builder().name("의류").depth(1).isActive("Y").build());
        Category accessories = categoryRepository.save(Category.builder().name("액세서리").depth(1).isActive("Y").build());

        categoryRepository.saveAll(List.of(
                Category.builder().parentNo(sneakers.getCategoryNo()).name("러닝화").depth(2).isActive("Y").build(),
                Category.builder().parentNo(sneakers.getCategoryNo()).name("라이프스타일").depth(2).isActive("Y").build(),
                Category.builder().parentNo(apparel.getCategoryNo()).name("후디/스웨트").depth(2).isActive("Y").build(),
                Category.builder().parentNo(apparel.getCategoryNo()).name("아우터").depth(2).isActive("Y").build(),
                Category.builder().parentNo(accessories.getCategoryNo()).name("캡/비니").depth(2).isActive("Y").build(),
                Category.builder().parentNo(accessories.getCategoryNo()).name("가방").depth(2).isActive("Y").build()
        ));
    }

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        Map<String, Brand> brands = brandRepository.findAll().stream().collect(Collectors.toMap(Brand::getNameKo, item -> item));
        Map<String, Category> categories = categoryRepository.findAll().stream().collect(Collectors.toMap(Category::getName, item -> item));

        List<ProductSeed> seeds = List.of(
                new ProductSeed("에어 포스 1 로우 화이트", "AF1-LOW-WHT", "나이키", "라이프스타일", 139000, LocalDate.now().minusMonths(10), "/images/product/af1-white.png", "ACTIVE", List.of(option("225", 14, 0), option("230", 12, 0), option("240", 9, 0))),
                new ProductSeed("에어 조던 1 하이 시카고", "AJ1-CHI-RED", "나이키", "라이프스타일", 219000, LocalDate.now().minusMonths(7), "/images/product/aj1-chicago.png", "ACTIVE", List.of(option("255", 5, 0), option("260", 4, 0), option("270", 3, 0))),
                new ProductSeed("삼바 OG 블랙", "SAMBA-BLK", "아디다스", "라이프스타일", 139000, LocalDate.now().minusMonths(5), "/images/product/samba-black.png", "ACTIVE", List.of(option("240", 8, 0), option("250", 7, 0), option("260", 6, 0))),
                new ProductSeed("가젤 인도어 블루", "GAZELLE-BLU", "아디다스", "라이프스타일", 149000, LocalDate.now().minusMonths(6), "/images/product/gazelle-blue.png", "HIDDEN", List.of(option("245", 11, 0), option("255", 8, 0))),
                new ProductSeed("990v6 그레이", "NB-990V6-GRY", "뉴발란스", "러닝화", 299000, LocalDate.now().minusMonths(8), "/images/product/990v6-grey.png", "ACTIVE", List.of(option("260", 12, 0), option("270", 10, 0), option("280", 7, 0))),
                new ProductSeed("젤 카야노 14 크림 블랙", "ASICS-KAYANO14", "아식스", "러닝화", 189000, LocalDate.now().minusMonths(4), "/images/product/kayano14.png", "ACTIVE", List.of(option("255", 6, 0), option("265", 5, 0), option("275", 2, 0))),
                new ProductSeed("박스 로고 후디 헤더 그레이", "SUP-BOX-HDY", "슈프림", "후디/스웨트", 328000, LocalDate.now().minusMonths(3), "/images/product/supreme-hoodie.png", "SOLD_OUT", List.of(option("M", 0, 0), option("L", 0, 0), option("XL", 0, 10000))),
                new ProductSeed("8볼 피그먼트 후디", "STUSSY-8BALL-HDY", "스투시", "후디/스웨트", 198000, LocalDate.now().minusMonths(2), "/images/product/stussy-hoodie.png", "ACTIVE", List.of(option("M", 15, 0), option("L", 10, 0), option("XL", 6, 0)))
        );

        for (ProductSeed seed : seeds) {
            Product product = productRepository.save(Product.builder()
                    .brandNo(brands.get(seed.brandName()).getBrandNo())
                    .categoryNo(categories.get(seed.categoryName()).getCategoryNo())
                    .nameKo(seed.productName())
                    .modelNum(seed.modelNum())
                    .releasePrice(seed.releasePrice())
                    .releaseDt(seed.releaseDate())
                    .thumbnailUrl(seed.thumbnailUrl())
                    .status(seed.status())
                    .build());

            List<ProductOption> options = seed.options().stream()
                    .map(option -> ProductOption.builder()
                            .productNo(product.getId())
                            .optionName(option.optionName())
                            .stockCnt(option.stockCnt())
                            .additionalPrice(option.additionalPrice())
                            .build())
                    .toList();
            productOptionRepository.saveAll(options);

            long totalStock = options.stream().mapToLong(ProductOption::getStockCnt).sum();
            productChangeHistoryRepository.save(ProductChangeHistory.of(
                    product.getId(),
                    ProductHistoryActionType.CREATED,
                    seed.productName() + " 상품이 초기 운영 데이터로 등록되었습니다.",
                    product.getStatus(),
                    options.size(),
                    totalStock
            ));
        }
    }

    private void seedBanners() {
        if (bannerRepository.count() > 0) {
            return;
        }

        Long adminNo = resolvePrimaryAdminNo();
        LocalDateTime now = LocalDateTime.now();
        bannerRepository.saveAll(List.of(
                banner("주간 인기 스니커즈 모음전", "/images/banner/top-sneakers.png", "/admin/products?keyword=인기", now.minusDays(1), now.plusDays(10), 1, adminNo),
                banner("저재고 긴급 보충 대상 확인", "/images/banner/low-stock.png", "/admin/products?lowStockOnly=Y", now.minusHours(12), now.plusDays(7), 2, adminNo),
                banner("커뮤니티 스타일 피드 운영 가이드", "/images/banner/style-guide.png", "/admin/content/list?boardType=STYLE", now.minusDays(2), now.plusDays(14), 3, adminNo),
                banner("월간 주문 리뷰 점검", "/images/banner/order-review.png", "/admin/orders", now.minusDays(3), now.plusDays(21), 4, adminNo)
        ));
    }

    private DisplayBanner banner(
            String title,
            String imageUrl,
            String targetUrl,
            LocalDateTime startDtm,
            LocalDateTime endDtm,
            int sortOrder,
            Long crtAdminNo
    ) {
        return DisplayBanner.builder()
                .title(title)
                .imageUrl(imageUrl)
                .targetUrl(targetUrl)
                .startDtm(startDtm)
                .endDtm(endDtm)
                .sortOrder(sortOrder)
                .isActive("Y")
                .crtAdminNo(crtAdminNo)
                .build();
    }

    private void seedDocuments() {
        if (documentRepository.count() > 0) {
            return;
        }

        Long firstProductNo = productRepository.findAll().stream().findFirst().map(Product::getId).orElse(null);
        List<Document> documents = new ArrayList<>();
        documents.add(document(Document.BoardType.NOTICE, "5월 말 정산 일정 안내", "운영팀 정산 일정과 출고 마감 시간을 안내합니다.", YN.Y, YN.Y, null));
        documents.add(document(Document.BoardType.NOTICE, "검수 기준 업데이트", "상품 검수 기준이 일부 업데이트되었습니다. 이미지 업로드 규격을 확인해 주세요.", YN.Y, YN.N, null));
        documents.add(document(Document.BoardType.STYLE, "주말 데일리 룩 제안", "오프화이트 톤의 러닝화와 후디 조합이 높은 반응을 얻고 있습니다.", YN.Y, YN.N, firstProductNo));
        documents.add(document(Document.BoardType.STYLE, "여름 샌드톤 코디 큐레이션", "샌드 베이지 계열 스니커즈와 경량 아우터 매칭 포인트를 정리합니다.", YN.Y, YN.N, firstProductNo));
        documents.add(document(Document.BoardType.DISCUSS, "삼바 OG 리오더 수요 체크", "삼바 OG의 사이즈별 수요와 리오더 희망 수량을 정리합니다.", YN.Y, YN.N, firstProductNo));
        documents.add(document(Document.BoardType.DISCUSS, "990v6 가격 방어력 토론", "리셀가와 실거래량을 기준으로 다음 입고 전략을 검토합니다.", YN.Y, YN.N, firstProductNo));
        documents.add(document(Document.BoardType.QNA, "배송 지연 문의 응대 템플릿", "출고 지연 시 고객에게 안내할 기본 응대 문구를 저장합니다.", YN.Y, YN.N, null));
        documents.add(document(Document.BoardType.QNA, "사이즈 교환 요청 처리 기준", "사이즈 교환은 미사용 상품에 한해 접수 가능하며 검수 완료 후 처리합니다.", YN.N, YN.N, null));
        documentRepository.saveAll(documents);
    }

    private Document document(Document.BoardType boardType, String title, String content, YN publicYn, YN pinnedYn, Long productNo) {
        Document document = new Document();
        document.applyEditorValues(boardType, Document.PublishStatus.PUBLISHED, publicYn, pinnedYn, title, content, productNo);
        document.setViewCnt((int) (Math.random() * 500));
        return document;
    }

    private void seedOrders() {
        if (orderRepository.count() > 0) {
            return;
        }

        List<Product> products = productRepository.findAll();
        if (products.size() < 4) {
            return;
        }
        Map<Long, List<ProductOption>> optionMap = productOptionRepository.findAll().stream()
                .collect(Collectors.groupingBy(ProductOption::getProductNo));

        List<Orders> orders = new ArrayList<>();
        orders.add(Orders.createOrder("ORD-20260529-001", "최민지", "010-2201-1234", 139000));
        orders.add(Orders.createOrder("ORD-20260529-002", "김도윤", "010-2201-5678", 219000));
        orders.add(Orders.createOrder("ORD-20260529-003", "박서연", "010-3210-7777", 299000));
        orders.add(Orders.createOrder("ORD-20260529-004", "정하준", "010-8877-3321", 198000));
        orders.add(Orders.createOrder("ORD-20260529-005", "오지우", "010-9988-1122", 149000));
        orders.add(Orders.createOrder("ORD-20260529-006", "윤시현", "010-1123-4567", 378000));
        orders.add(Orders.createOrder("ORD-20260529-007", "강예린", "010-5533-7788", 139000));
        orders.add(Orders.createOrder("ORD-20260529-008", "이준혁", "010-7744-2211", 189000));
        orderRepository.saveAll(orders);

        Orders paid = orders.get(1);
        paid.pay();
        Orders preparing = orders.get(2);
        preparing.pay();
        preparing.changeStatus(com.section.common.base.entity.type.OrderStatus.PREPARING);
        Orders shipped = orders.get(3);
        shipped.pay();
        shipped.startDelivery("CJ대한통운", "650012341234");
        Orders delivered = orders.get(4);
        delivered.pay();
        delivered.startDelivery("한진택배", "550012341111");
        delivered.completeDelivery();
        Orders cancelled = orders.get(5);
        cancelled.pay();
        cancelled.cancel();
        Orders paidTwo = orders.get(6);
        paidTwo.pay();
        Orders shippedTwo = orders.get(7);
        shippedTwo.pay();
        shippedTwo.startDelivery("롯데택배", "770012341111");

        orderItemRepository.saveAll(List.of(
                orderItem(orders.get(0), products.get(0), optionMap),
                orderItem(orders.get(1), products.get(1), optionMap),
                orderItem(orders.get(2), products.get(2), optionMap),
                orderItem(orders.get(3), products.get(3), optionMap),
                orderItem(orders.get(4), products.get(4), optionMap),
                orderItem(orders.get(5), products.get(5), optionMap),
                orderItem(orders.get(6), products.get(0), optionMap),
                orderItem(orders.get(7), products.get(5), optionMap)
        ));

        orderStatusHistoryRepository.saveAll(List.of(
                OrderStatusHistory.create(orders.get(0).getId(), "ORDER_CREATE", null, "ORDERED", "신규 주문 생성", null, null, null),
                OrderStatusHistory.create(orders.get(1).getId(), "PAYMENT_COMPLETE", "ORDERED", "PAID", "결제 완료", null, null, null),
                OrderStatusHistory.create(preparing.getId(), "STATUS_CHANGE", "PAID", "PREPARING", "출고 준비", null, null, null),
                OrderStatusHistory.create(shipped.getId(), "DELIVERY_START", "PAID", "SHIPPED", "출고 완료", null, shipped.getDeliveryCompany(), shipped.getTrackingNum()),
                OrderStatusHistory.create(delivered.getId(), "DELIVERY_COMPLETE", "SHIPPED", "DELIVERED", "배송 완료", null, delivered.getDeliveryCompany(), delivered.getTrackingNum()),
                OrderStatusHistory.create(cancelled.getId(), "CANCEL", "PAID", "CANCELLED", "고객 요청 취소", null, null, null),
                OrderStatusHistory.create(paidTwo.getId(), "PAYMENT_COMPLETE", "ORDERED", "PAID", "결제 완료", null, null, null),
                OrderStatusHistory.create(shippedTwo.getId(), "DELIVERY_START", "PAID", "SHIPPED", "출고 완료", null, shippedTwo.getDeliveryCompany(), shippedTwo.getTrackingNum())
        ));
    }

    private OrderItem orderItem(Orders order, Product product, Map<Long, List<ProductOption>> optionMap) {
        List<ProductOption> options = optionMap.getOrDefault(product.getId(), List.of());
        ProductOption option = options.isEmpty() ? null : options.getFirst();
        return OrderItem.builder()
                .orderNo(order.getId())
                .productNo(product.getId())
                .optionNo(option == null ? null : option.getId())
                .productName(product.getNameKo())
                .orderPrice(product.getReleasePrice())
                .count(1)
                .build();
    }

    private void seedNotices() {
        if (adminOperationNoticeRepository.count() > 0) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        adminOperationNoticeRepository.saveAll(List.of(
                notice("주문 마감 시간 변경 안내", "당일 출고 마감 시간이 오후 3시로 조정되었습니다.", "Y", "Y", now.minusDays(3), now.plusDays(7)),
                notice("저재고 상품 긴급 발주 요청", "젤 카야노 14와 삼바 OG 일부 사이즈의 재고가 부족합니다.", "Y", "N", now.minusHours(12), now.plusDays(5)),
                notice("6월 초 프로모션 배너 검수", "홈 배너와 카테고리 큐레이션 이미지를 오전 중 확인해 주세요.", "Y", "N", now.plusDays(1), now.plusDays(10)),
                notice("시스템 점검 사전 공지", "주말 새벽에 로그 정리 배치와 설정 이력 백업 작업이 예정되어 있습니다.", "N", "N", now.plusDays(2), now.plusDays(12))
        ));
    }

    private AdminOperationNotice notice(String title, String content, String isActive, String isPinned, LocalDateTime startDtm, LocalDateTime endDtm) {
        return AdminOperationNotice.builder()
                .title(title)
                .content(content)
                .isActive(isActive)
                .isPinned(isPinned)
                .startDtm(startDtm)
                .endDtm(endDtm)
                .build();
    }

    private void seedTasks() {
        if (adminOperationTaskRepository.count() > 0) {
            return;
        }

        List<AdminUser> admins = adminUserRepository.findAll();
        Long opsAdminNo = admins.size() > 1 ? admins.get(1).getAdminNo() : resolvePrimaryAdminNo();
        Long mdAdminNo = admins.size() > 2 ? admins.get(2).getAdminNo() : resolvePrimaryAdminNo();

        List<AdminOperationTask> tasks = adminOperationTaskRepository.saveAll(List.of(
                task("주간 베스트셀러 배너 교체", "메인 배너 1, 2번 슬롯을 주간 판매 데이터를 기준으로 교체합니다.", "TODO", "HIGH", mdAdminNo, LocalDate.now().plusDays(1), "Y"),
                task("저재고 SKU 발주 수량 확정", "상품별 판매 속도와 리드타임을 기준으로 발주 수량을 정리합니다.", "IN_PROGRESS", "HIGH", opsAdminNo, LocalDate.now().plusDays(2), "N"),
                task("배송 지연 문의 템플릿 점검", "반복 문의 응답 템플릿을 최신 정책으로 갱신합니다.", "DONE", "MEDIUM", opsAdminNo, LocalDate.now().minusDays(1), "N"),
                task("스타일 피드 노출 기준 정리", "상품 태그와 노출 우선순위 기준을 문서화합니다.", "TODO", "MEDIUM", mdAdminNo, LocalDate.now().plusDays(5), "N"),
                task("운영 공지 6월 일정 업데이트", "정산/휴무/배너 검수 일정을 공지에 반영합니다.", "IN_PROGRESS", "LOW", null, LocalDate.now().plusDays(3), "N"),
                task("미사용 카테고리 정리 후보 검토", "최근 90일 미사용 카테고리를 확인하고 병합 후보를 추립니다.", "TODO", "LOW", null, null, "N")
        ));

        adminOperationTaskCommentRepository.saveAll(List.of(
                comment(tasks.get(0), "배너 시안은 오전 11시까지 공유 예정입니다."),
                comment(tasks.get(1), "러닝화 카테고리만 우선 발주 수량을 확정해 주세요."),
                comment(tasks.get(1), "삼바 OG 250~260 사이즈는 안전재고를 2배로 잡는 방향입니다."),
                comment(tasks.get(2), "CS팀 피드백 반영 완료했습니다."),
                comment(tasks.get(4), "담당자 미지정 상태라 우선 운영팀 공용 큐에 남겨둡니다."),
                comment(tasks.get(5), "하위 카테고리 사용량 리포트가 필요합니다.")
        ));
    }

    private AdminOperationTask task(
            String title,
            String description,
            String status,
            String priority,
            Long assigneeAdminNo,
            LocalDate dueDate,
            String isPinned
    ) {
        return AdminOperationTask.builder()
                .title(title)
                .description(description)
                .status(status)
                .priority(priority)
                .assigneeAdminNo(assigneeAdminNo)
                .dueDate(dueDate)
                .isPinned(isPinned)
                .build();
    }

    private AdminOperationTaskComment comment(AdminOperationTask task, String content) {
        return AdminOperationTaskComment.builder()
                .taskNo(task.getTaskNo())
                .content(content)
                .build();
    }

    private void seedSystemSettings() {
        if (adminSystemSettingRepository.count() == 0) {
            adminSystemSettingRepository.saveAll(List.of(
                    AdminSystemSetting.builder().settingKey("SYSTEM_MAINTENANCE_MODE").settingValue("false").description("관리자 서비스 유지보수 모드").build(),
                    AdminSystemSetting.builder().settingKey("COMMUNITY_WRITE_ENABLED").settingValue("true").description("커뮤니티 글쓰기 허용").build(),
                    AdminSystemSetting.builder().settingKey("ORDER_EXPORT_ENABLED").settingValue("true").description("주문 export 허용").build(),
                    AdminSystemSetting.builder().settingKey("LOW_STOCK_DEFAULT_THRESHOLD").settingValue("8").description("저재고 기본 임계값").build()
            ));
        }

        if (adminSystemSettingHistoryRepository.count() == 0) {
            adminSystemSettingHistoryRepository.saveAll(List.of(
                    settingHistory("SYSTEM_MAINTENANCE_MODE", "유지보수 모드", "true", "false", "유지보수 모드가 비활성으로 전환되었습니다."),
                    settingHistory("COMMUNITY_WRITE_ENABLED", "커뮤니티 작성 허용", "false", "true", "커뮤니티 작성 기능을 재오픈했습니다."),
                    settingHistory("ORDER_EXPORT_ENABLED", "주문 Export 허용", "false", "true", "주문 CSV 내보내기 기능을 다시 허용했습니다."),
                    settingHistory("LOW_STOCK_DEFAULT_THRESHOLD", "기본 저재고 임계값", "10", "8", "저재고 임계값을 최근 판매 속도에 맞춰 조정했습니다.")
            ));
        }
    }

    private AdminSystemSettingHistory settingHistory(String key, String name, String beforeValue, String afterValue, String summary) {
        return AdminSystemSettingHistory.builder()
                .settingKey(key)
                .settingName(name)
                .beforeValue(beforeValue)
                .afterValue(afterValue)
                .changeSummary(summary)
                .changedIpAddress("127.0.0.1")
                .build();
    }

    private void seedApprovalDocuments() {
        if (approvalDocumentRepository.count() > 0) {
            return;
        }

        List<ApprovalDocument> documents = new ArrayList<>();
        documents.add(approvalDocument("6월 메인 배너 교체 승인", "BANNER", "APPROVED", "1", "101", "201", LocalDateTime.now().minusDays(2)));
        documents.add(approvalDocument("저재고 긴급 발주 승인", "PURCHASE", "REQUESTED", "1", "102", "202", LocalDateTime.now().minusHours(8)));
        documents.add(approvalDocument("커뮤니티 운영 가이드 개정", "CONTENT", "APPROVED", "2", "103", "203", LocalDateTime.now().minusDays(5)));
        approvalDocumentRepository.saveAll(documents);
    }

    private ApprovalDocument approvalDocument(
            String title,
            String contentTypeCode,
            String status,
            String depth,
            String managerNo,
            String altManagerNo,
            LocalDateTime approvalDtm
    ) {
        ApprovalDocument document = new ApprovalDocument();
        document.setTitle(title);
        document.setContentTypeCode(contentTypeCode);
        document.setStatus(status);
        document.setDepth(depth);
        document.setRenewYn(YN.N);
        document.setDelYn(YN.N);
        document.setManagerNo(managerNo);
        document.setAltManagerNo(altManagerNo);
        document.setApprovalDtm(approvalDtm);
        return document;
    }

    private void seedActivityLogs() {
        if (adminActivityLogRepository.count() > 0) {
            return;
        }

        Long adminNo = resolvePrimaryAdminNo();
        Long firstNoticeNo = adminOperationNoticeRepository.findAll().stream().findFirst().map(AdminOperationNotice::getNoticeNo).orElse(null);
        Long firstTaskNo = adminOperationTaskRepository.findAll().stream().findFirst().map(AdminOperationTask::getTaskNo).orElse(null);
        Long firstOrderNo = orderRepository.findAll().stream().findFirst().map(Orders::getId).orElse(null);
        Long firstProductNo = productRepository.findAll().stream().findFirst().map(Product::getId).orElse(null);

        adminActivityLogRepository.saveAll(List.of(
                log(adminNo, "NOTICE_CREATE", firstNoticeNo, "127.0.0.1", LocalDateTime.now().minusDays(4)),
                log(adminNo, "NOTICE_UPDATE", firstNoticeNo, "127.0.0.1", LocalDateTime.now().minusDays(3)),
                log(adminNo, "TASK_CREATE", firstTaskNo, "127.0.0.1", LocalDateTime.now().minusDays(2)),
                log(adminNo, "TASK_COMMENT_CREATE", firstTaskNo, "127.0.0.1", LocalDateTime.now().minusDays(2).plusHours(3)),
                log(adminNo, "ORDER_STATUS_CHANGE", firstOrderNo, "127.0.0.1", LocalDateTime.now().minusDays(1)),
                log(adminNo, "PRODUCT_CREATE", firstProductNo, "127.0.0.1", LocalDateTime.now().minusDays(7)),
                log(adminNo, "PRODUCT_UPDATE", firstProductNo, "127.0.0.1", LocalDateTime.now().minusDays(5)),
                log(adminNo, "SETTING_UPDATE", 1L, "127.0.0.1", LocalDateTime.now().minusHours(12))
        ));
    }

    private AdminActivityLog log(Long adminNo, String actionType, Long targetId, String ipAddress, LocalDateTime actionDtm) {
        return AdminActivityLog.builder()
                .adminNo(adminNo)
                .actionType(actionType)
                .targetId(targetId)
                .ipAddress(ipAddress)
                .actionDtm(actionDtm)
                .build();
    }

    private Long resolvePrimaryAdminNo() {
        return adminUserRepository.findAll().stream().findFirst().map(AdminUser::getAdminNo).orElse(1L);
    }

    private record ProductSeed(
            String productName,
            String modelNum,
            String brandName,
            String categoryName,
            int releasePrice,
            LocalDate releaseDate,
            String thumbnailUrl,
            String status,
            List<ProductOptionSeed> options
    ) {
    }

    private record ProductOptionSeed(String optionName, int stockCnt, int additionalPrice) {
    }

    private static ProductOptionSeed option(String optionName, int stockCnt, int additionalPrice) {
        return new ProductOptionSeed(optionName, stockCnt, additionalPrice);
    }
}
