package com.surveysheet.dto.response;

import com.surveysheet.service.definition.Definition;
import com.surveysheet.service.definition.Display;

/** 定義の API（GET /api/survey）の応答 */
public record DefinitionResponse(String version, Definition.Survey survey, Display display, Limits limits) {

  /** 入力の規則のうち、管理者が環境変数で設定する値 */
  public record Limits(int userNameMaxLength) {
  }

  public static DefinitionResponse of(Definition definition, int userNameMaxLength) {
    return new DefinitionResponse(definition.version(), definition.survey(), definition.display(),
        new Limits(userNameMaxLength));
  }
}
