package com.section.admin.product.service;

import com.section.admin.product.res.OrderDetailResponse;
import com.section.admin.product.res.OrderListResponse;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.entity.Product;
import com.section.common.commerce.entity.Brand;
import com.section.common.commerce.entity.Category;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.ProductRepository;
import com.section.common.commerce.repository.BrandRepository;
import com.section.common.commerce.repository.CategoryRepository;
import com.section.common.commerce.service.OrderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final OrderService orderService;

    /**
     * 테스트용 더미 데이터 초기화 (상품, 브랜드, 카테고리 포함)
     */
    @PostConstruct
    @Transactional
    public void initDummyData() {
        if (orderRepository.count() == 0) {
            // 1. 브랜드 및 카테고리 생성 (필요시)
            if (brandRepository.count() == 0) {
                brandRepository.save(Brand.builder().nameKo("나이키").nameEn("Nike").build());
                brandRepository.save(Brand.builder().nameKo("아디다스").nameEn("Adidas").build());
            }
            if (categoryRepository.count() == 0) {
                categoryRepository.save(Category.builder().name("신발").depth(1).isActive("Y").build());
                categoryRepository.save(Category.builder().name("의류").depth(1).isActive("Y").build());
            }

            List<Brand> brands = brandRepository.findAll();
            List<Category> categories = categoryRepository.findAll();

            // 2. 상품 생성 (필요시)
            if (productRepository.count() == 0 && !brands.isEmpty() && !categories.isEmpty()) {
                productRepository.save(Product.builder()
                        .nameKo("에어 포스 1 '07").brandNo(brands.get(0).getBrandNo()).categoryNo(categories.get(0).getCategoryNo())
                        .releasePrice(139000).status("ACTIVE").thumbnailUrl("https://static.nike.com/a/images/t_PDP_1728_v1/f_auto,q_auto:eco/b7d9211c-26e7-431a-ac24-b0540fb3c00f/AIR+FORCE+1+%2707.png").build());
                productRepository.save(Product.builder()
                        .nameKo("슈퍼스타").brandNo(brands.get(1).getBrandNo()).categoryNo(categories.get(0).getCategoryNo())
                        .releasePrice(109000).status("ACTIVE").thumbnailUrl("https://assets.adidas.com/images/h_840,f_auto,q_auto,fl_lossy,c_fill,g_auto/7ed0852433934c529815af3e017286df_9366/Superstar_Shoes_White_EG4958_01_standard.jpg").build());
            }

            List<Product> products = productRepository.findAll();

            // 3. 주문 생성
            Orders o1 = Orders.createOrder("ORD-20260401-001", "김철수", "010-1234-5678", 139000);
            Orders o2 = Orders.createOrder("ORD-20260402-001", "이영희", "010-9876-5432", 248000);
            Orders o3 = Orders.createOrder("ORD-20260403-001", "박지민", "010-5555-4444", 139000);
            
            orderRepository.saveAll(List.of(o1, o2, o3));
            o1.pay();
            o3.cancel();

            // 4. 주문 상세 연결
            if (!products.isEmpty()) {
                // o1: 상품 1개
                orderItemRepository.save(OrderItem.builder()
                        .orderNo(o1.getId()).productNo(products.get(0).getId()).productName(products.get(0).getNameKo())
                        .orderPrice(products.get(0).getReleasePrice()).count(1).build());

                // o2: 상품 2개
                if (products.size() > 1) {
                    orderItemRepository.save(OrderItem.builder()
                            .orderNo(o2.getId()).productNo(products.get(0).getId()).productName(products.get(0).getNameKo())
                            .orderPrice(products.get(0).getReleasePrice()).count(1).build());
                    orderItemRepository.save(OrderItem.builder()
                            .orderNo(o2.getId()).productNo(products.get(1).getId()).productName(products.get(1).getNameKo())
                            .orderPrice(products.get(1).getReleasePrice()).count(1).build());
                }

                // o3: 상품 1개
                orderItemRepository.save(OrderItem.builder()
                        .orderNo(o3.getId()).productNo(products.get(0).getId()).productName(products.get(0).getNameKo())
                        .orderPrice(products.get(0).getReleasePrice()).count(1).build());
            }
        }
    }

    /**
     * 화면용 주문 목록 조회
     */
    public OrderListResponse getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        Page<OrderListResDto> result = orderService.getOrderList(reqDto, pageable);
        return OrderListResponse.of(result);
    }

    /**
     * 화면용 주문 상세 조회
     */
    public OrderDetailResponse getOrderDetail(Long orderNo) {
        OrderListResDto master = orderService.getOrderDetail(orderNo);
        if (master == null) throw new IllegalArgumentException("존재하지 않는 주문입니다.");
        
        List<OrderItemResDto> items = orderService.getOrderItems(orderNo);
        return OrderDetailResponse.from(master, items);
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public void updateOrderStatus(Long orderNo, String status) {
        orderService.updateOrderStatus(orderNo, OrderStatus.valueOf(status));
    }
}
