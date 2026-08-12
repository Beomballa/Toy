package com.section.common.commerce.repository;

import com.section.common.commerce.entity.Orders;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface OrderRepository extends JpaRepository<Orders, Long>, CustomOrderRepository {
    Optional<Orders> findByOrderNum(String orderNum);

    Optional<Orders> findByOrderNumAndMemberNo(String orderNum, Long memberNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orders o WHERE o.orderNum = :orderNumber AND o.memberNo = :memberNo")
    Optional<Orders> findByOrderNumAndMemberNoForUpdate(
            @Param("orderNumber") String orderNumber,
            @Param("memberNo") Long memberNo
    );

    Page<Orders> findByMemberNoOrderByIdDesc(Long memberNo, Pageable pageable);

    Page<Orders> findByMemberNoAndStatusOrderByIdDesc(Long memberNo, String status, Pageable pageable);

    @Query("select o from Orders o where o.memberNo = :memberNo and o.status = :status "
            + "and exists (select 1 from OrderItem item where item.orderNo = o.id and item.productNo = :productNo) "
            + "and not exists (select 1 from FrontProductReview review where review.memberNo = :memberNo "
            + "and review.orderNo = o.id and review.productNo = :productNo) order by o.id desc")
    List<Orders> findReviewEligibleOrders(
            @Param("memberNo") long memberNo,
            @Param("productNo") long productNo,
            @Param("status") String status
    );

    @Query("SELECT orders.status AS status, COUNT(orders) AS count FROM Orders orders "
            + "WHERE orders.memberNo = :memberNo GROUP BY orders.status")
    List<MemberOrderStatusCount> countByMemberNoGroupByStatus(@Param("memberNo") Long memberNo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Orders o WHERE o.id = :orderNo")
    Optional<Orders> findByIdForUpdate(@Param("orderNo") Long orderNo);
}
