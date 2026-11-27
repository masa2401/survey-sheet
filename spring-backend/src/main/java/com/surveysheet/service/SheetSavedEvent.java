package com.surveysheet.service;

import java.util.UUID;

/** シートの保存を表すイベント。PDF 生成をトランザクションのコミット後に始めるために使う */
public record SheetSavedEvent(UUID sheetId, String userName) {
}
