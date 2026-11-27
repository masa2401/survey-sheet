package com.surveysheet.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * カテゴリ1件と、その中で回答された設問。
 * 設問の件数の上限は、API を直接呼ばれた場合に過大なデータを保存しないための備え。
 * 現在のマスタで最も多いカテゴリでも設問は10件のため、マスタの追加に余裕を持たせて30件としている。
 */
public record CategoryDto(
    @NotNull Integer categoryId,
    @NotNull @Size(max = 30) @Valid List<QuestionDto> questions) {
}
