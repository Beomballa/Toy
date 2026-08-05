package com.section.common.commerce.repository;

import com.section.common.commerce.entity.FrontMemberActivityType;
import com.section.common.commerce.entity.FrontMemberProductActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FrontMemberProductActivityRepository extends JpaRepository<FrontMemberProductActivity, Long> {

    List<FrontMemberProductActivity> findAllByMemberNoOrderByLastInteractedAtDescIdDesc(Long memberNo);

    List<FrontMemberProductActivity> findAllByMemberNoAndActivityTypeOrderByLastInteractedAtDescIdDesc(
            Long memberNo,
            FrontMemberActivityType activityType
    );

    void deleteAllByMemberNoAndActivityType(Long memberNo, FrontMemberActivityType activityType);

    void deleteAllByMemberNo(Long memberNo);
}
