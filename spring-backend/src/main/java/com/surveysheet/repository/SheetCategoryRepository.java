package com.surveysheet.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.surveysheet.entity.SheetCategory;

public interface SheetCategoryRepository extends JpaRepository<SheetCategory, Long> {
  /**
   * シートのカテゴリを、回答ごと1本のクエリで取得する。
   * categories と answers はどちらも List のため、シートから2段まとめて fetch join すると
   * MultipleBagFetchException になる。カテゴリを起点にして1段にしている
   */
  @Query("select c from SheetCategory c left join fetch c.answers where c.skillSheet.id = :sheetId")
  List<SheetCategory> findWithAnswersBySheetId(@Param("sheetId") UUID sheetId);
}