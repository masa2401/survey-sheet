package com.surveysheet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 回答1件。value は習熟度（星の数、1〜5） */
public record AnswerDto(
    @NotNull Integer answerId,
    @Min(1) @Max(5) Integer value) {
}
