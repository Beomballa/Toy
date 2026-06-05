package com.section.admin.order.service;

import com.section.admin.order.res.OrderDetailResponse;
import com.section.admin.order.res.OrderHistoryListResponse;
import com.section.admin.order.res.OrderListResponse;
import com.section.admin.order.support.OrderHistoryExportCsvWriter;
import com.section.admin.order.support.OrderListPagePolicy;
import com.section.admin.order.support.OrderExportCsvWriter;
import com.section.common.base.entity.type.OrderStatus;
import com.section.common.base.exception.BusinessException;
import com.section.common.base.exception.ErrorCode;
import com.section.common.commerce.dto.OrderListItemDto;
import com.section.common.commerce.dto.OrderHistoryListQuery;
import com.section.common.commerce.dto.OrderListReqDto;
import com.section.common.commerce.dto.OrderListResDto;
import com.section.common.commerce.dto.OrderItemResDto;
import com.section.admin.order.req.OrderHistoryListRequest;
import com.section.common.commerce.entity.Orders;
import com.section.common.commerce.entity.OrderStatusHistory;
import com.section.common.commerce.entity.OrderItem;
import com.section.common.commerce.repository.OrderItemRepository;
import com.section.common.commerce.repository.OrderRepository;
import com.section.common.commerce.repository.OrderStatusHistoryRepository;
import com.section.common.commerce.repository.ProductOptionRepository;
import com.section.common.commerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminOrderService {
    private static final int ORDER_EXPORT_MAX_SIZE = 1000;
    private static final int ORDER_HISTORY_EXPORT_MAX_SIZE = 2000;
    private static final int ORDER_ADMIN_MEMO_MAX_LENGTH = 1000;

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderService orderService;

    /**
     * 화면용 주문 목록 조회
     */
    public OrderListResponse getOrderList(OrderListReqDto reqDto, Pageable pageable) {
        Page<OrderListItemDto> result = orderService.getOrderList(reqDto, OrderListPagePolicy.normalize(pageable));
        return OrderListResponse.of(result, orderService.getOrderStatusSummaries(reqDto));
    }

    public byte[] exportOrderListCsv(OrderListReqDto reqDto) {
        Page<OrderListItemDto> result = orderService.getOrderList(reqDto, PageRequest.of(0, ORDER_EXPORT_MAX_SIZE));
        return OrderExportCsvWriter.write(result.getContent());
    }

    /**
     * 화면용 주문 상세 조회
     */
    public OrderDetailResponse getOrderDetail(Long orderNo) {
        OrderListResDto master = orderService.getOrderDetail(orderNo);
        if (master == null) throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        
        List<OrderItemResDto> items = orderService.getOrderItems(orderNo);
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findTop20ByOrderNoOrderByCrtDtmDescIdDesc(orderNo);
        return OrderDetailResponse.from(master, items, histories);
    }

    public OrderHistoryListResponse getOrderHistoryList(OrderHistoryListRequest request, Pageable pageable) {
        OrderHistoryListQuery query = request.toQuery();
        return OrderHistoryListResponse.of(
                orderStatusHistoryRepository.getOrderHistoryList(query, OrderListPagePolicy.normalize(pageable)),
                query
        );
    }

    public byte[] exportOrderHistoryListCsv(OrderHistoryListRequest request) {
        OrderHistoryListQuery query = request.toQuery();
        return OrderHistoryExportCsvWriter.write(
                orderStatusHistoryRepository.getOrderHistoryList(
                        query,
                        PageRequest.of(0, ORDER_HISTORY_EXPORT_MAX_SIZE)
                ).getContent()
        );
    }

    /**
     * 주문 상태 변경
     */
    @Transactional
    public void updateOrderStatus(Long orderNo, OrderStatus status, String reason) {
        Orders order = findOrder(orderNo);
        String beforeStatus = order.getStatus();
        order.changeStatus(status);
        saveHistory(order, "STATUS_CHANGE", beforeStatus, order.getStatus(), reason);
    }

    @Transactional
    public void startDelivery(Long orderNo, String deliveryCompany, String trackingNum, String reason) {
        Orders order = findOrder(orderNo);
        String beforeStatus = order.getStatus();
        order.startDelivery(deliveryCompany, trackingNum);
        saveHistory(order, "DELIVERY_START", beforeStatus, order.getStatus(), reason);
    }

    @Transactional
    public void completeDelivery(Long orderNo, String reason) {
        Orders order = findOrder(orderNo);
        String beforeStatus = order.getStatus();
        order.completeDelivery();
        saveHistory(order, "DELIVERY_COMPLETE", beforeStatus, order.getStatus(), reason);
    }

    @Transactional
    public void cancelOrder(Long orderNo, String reason) {
        Orders order = findOrder(orderNo);
        String beforeStatus = order.getStatus();
        
        // 주문 상태 변경 (상태 전이 유효성 검사는 엔티티 내에서 수행)
        order.cancel();

        // 재고 복구 로직 연동
        List<OrderItem> items = orderItemRepository.findByOrderNo(orderNo);
        for (OrderItem item : items) {
            if (item.getOptionNo() != null) {
                productOptionRepository.findById(item.getOptionNo()).ifPresent(option -> {
                    option.addStock(item.getCount());
                });
            }
        }

        saveHistory(order, "CANCEL", beforeStatus, order.getStatus(), reason);
    }

    @Transactional
    public void saveAdminMemo(Long orderNo, String adminMemo) {
        Orders order = findOrder(orderNo);
        String normalizedMemo = normalizeAdminMemo(adminMemo);
        String currentMemo = normalizeAdminMemo(order.getAdminMemo());
        if (java.util.Objects.equals(currentMemo, normalizedMemo)) {
            return;
        }
        order.updateAdminMemo(normalizedMemo);
        saveHistory(order, "ADMIN_MEMO", order.getStatus(), order.getStatus(), "관리 메모 저장");
    }

    private Orders findOrder(Long orderNo) {
        return orderRepository.findById(orderNo)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));
    }

    private void saveHistory(Orders order, String actionType, String beforeStatus, String afterStatus, String reason) {
        orderStatusHistoryRepository.save(OrderStatusHistory.create(
                order.getId(),
                actionType,
                beforeStatus,
                afterStatus,
                reason,
                order.getAdminMemo(),
                order.getDeliveryCompany(),
                order.getTrackingNum()
        ));
    }

    private String normalizeAdminMemo(String adminMemo) {
        if (adminMemo == null) {
            return null;
        }
        String normalized = adminMemo.trim().replaceAll("\\s+", " ");
        if (normalized.length() > ORDER_ADMIN_MEMO_MAX_LENGTH) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return normalized.isBlank() ? null : normalized;
    }
}
