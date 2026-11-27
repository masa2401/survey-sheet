package com.surveysheet.dto.response;

import java.util.List;

import com.surveysheet.dto.CategoryDto;

/**
 * GET /api/sheets/{id} のレスポンス。
 * JSONの形は保存時のリクエスト（SaveSheetRequest）と同じだが、入力の検証を持たない出力用として分けている。
 */
public record SheetResponse(
    String userName,
    List<CategoryDto> categories) {
}
