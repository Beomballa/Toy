package com.section.common.content.repository;

import com.section.common.content.entity.DocumentStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DocumentStatsRepository extends JpaRepository<DocumentStats, Long> {

    @Query("SELECT ds FROM DocumentStats ds " +
            "WHERE ds.document.id = :no")
    Optional<DocumentStats> insertCheck(@Param("no") Long no);


    @Modifying(clearAutomatically = true)
    @Query("UPDATE DocumentStats d " +
            "SET d.viewCnt = d.viewCnt + :quantity " +
            "WHERE d.document.id = :id and d.stdDt = CURRENT_DATE")
    void addViewCnt(@Param("id") Long id, @Param("quantity") int quantity);
}
