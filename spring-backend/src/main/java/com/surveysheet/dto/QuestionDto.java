package com.surveysheet.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 設問1件と、その設問で選ばれた回答。
 * 回答の件数の上限は、API を直接呼ばれた場合に過大なデータを保存しないための備え。
 * 現在のマスタで最も多い設問でも回答は8件のため、マスタの追加に余裕を持たせて30件としている。
 */
public record QuestionDto(
    @NotNull Integer questionId,
    @NotNull @Size(max = 30) @Valid List<AnswerDto> answers) {
}
