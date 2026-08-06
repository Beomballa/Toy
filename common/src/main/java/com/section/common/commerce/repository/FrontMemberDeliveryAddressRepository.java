package com.section.common.commerce.repository;
import com.section.common.commerce.entity.FrontMemberDeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface FrontMemberDeliveryAddressRepository extends JpaRepository<FrontMemberDeliveryAddress, Long> {
    List<FrontMemberDeliveryAddress> findAllByMemberNoOrderByDefaultYnDescIdDesc(Long memberNo);
    Optional<FrontMemberDeliveryAddress> findByIdAndMemberNo(Long id, Long memberNo);
}
