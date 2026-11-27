package com.surveysheet.dto.request;

import java.util.List;

import com.surveysheet.dto.CategoryDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * POST /api/sheets のリクエスト。
 * カテゴリはマスタの大分類（現在は3件）にあたるため、マスタの追加に余裕を持たせて上限を10件としている。
 * 設問・回答の上限は CategoryDto・QuestionDto を参照。
 */
public record SaveSheetRequest(
    @NotBlank @Size(max = 100) String userName,
    @NotNull @Size(max = 10) @Valid List<CategoryDto> categories) {
}
