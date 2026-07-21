package com.section.common.content.repository;

import com.section.common.content.entity.DocumentDailyStatsSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DocumentDailyStatsSnapshotRepository extends JpaRepository<DocumentDailyStatsSnapshot, Long> {

    List<DocumentDailyStatsSnapshot> findAllBySnapshotDateOrderByScopeAsc(LocalDate snapshotDate);

    Optional<DocumentDailyStatsSnapshot> findTopByOrderBySnapshotDateDesc();
}
