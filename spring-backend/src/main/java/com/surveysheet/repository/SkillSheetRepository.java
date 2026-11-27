package com.surveysheet.repository;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.surveysheet.entity.SkillSheet;

public interface SkillSheetRepository extends JpaRepository<SkillSheet, UUID> {

  /** カテゴリと回答は、外部キーの on delete cascade で DB が削除する（V3） */
  @Modifying
  @Query("delete from SkillSheet s where s.expiresAt < :now")
  int deleteExpiredBefore(@Param("now") LocalDateTime now);
}
