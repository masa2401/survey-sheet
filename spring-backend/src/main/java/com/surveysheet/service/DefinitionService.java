package com.surveysheet.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.surveysheet.exception.StartupException;
import com.surveysheet.service.definition.Definition;
import com.surveysheet.service.definition.DefinitionError;
import com.surveysheet.service.definition.DefinitionLoader;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** 起動時に定義ファイルを読んで検証し、結果をメモリに持つ。変更は再起動で反映する */
@Slf4j
@Service
public class DefinitionService {

  private static final int USER_NAME_MAX_LENGTH_LIMIT = 100;

  @Getter
  private final Definition definition;

  @Getter
  private final int userNameMaxLength;

  public DefinitionService(
      @Value("${survey.dir:}") String surveyDir,
      @Value("${sheet.user-name-max-length:20}") String userNameMaxLength) {
    this.userNameMaxLength = parseUserNameMaxLength(userNameMaxLength);
    this.definition = load(surveyDir);
    log.info("定義ファイルを読み込みました（{}、版 {}）", surveyDir, definition.version());
  }

  private static Definition load(String surveyDir) {
    if (surveyDir.isBlank()) {
      throw new StartupException("環境変数 SURVEY_DIR が設定されていません。定義ファイルを置いたフォルダを指定してください。");
    }
    Path dir = Path.of(surveyDir);
    if (!Files.isDirectory(dir)) {
      throw new StartupException("SURVEY_DIR のフォルダが見つかりません（" + surveyDir + "）。");
    }

    List<String> missing = new ArrayList<>();
    for (String file : List.of(DefinitionLoader.SURVEY_FILE, DefinitionLoader.DISPLAY_FILE)) {
      if (!Files.isRegularFile(dir.resolve(file))) {
        missing.add(file);
      }
    }
    if (!missing.isEmpty()) {
      throw new StartupException(String.join("・", missing) + " がありません（" + surveyDir + "）。");
    }

    DefinitionLoader.Result result = new DefinitionLoader().load(
        read(dir.resolve(DefinitionLoader.SURVEY_FILE)),
        read(dir.resolve(DefinitionLoader.DISPLAY_FILE)));
    return switch (result) {
      case DefinitionLoader.Loaded loaded -> loaded.definition();
      case DefinitionLoader.Invalid invalid -> throw new StartupException(DefinitionError.report(invalid.errors()));
    };
  }

  private static byte[] read(Path file) {
    try {
      return Files.readAllBytes(file);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static int parseUserNameMaxLength(String value) {
    try {
      int parsed = Integer.parseInt(value.trim());
      if (parsed >= 1 && parsed <= USER_NAME_MAX_LENGTH_LIMIT) {
        return parsed;
      }
    } catch (NumberFormatException e) {
      // 下の例外にまとめる
    }
    throw new StartupException("USER_NAME_MAX_LENGTH は 1〜" + USER_NAME_MAX_LENGTH_LIMIT
        + " の整数にしてください（今の値：" + value + "）。");
  }
}
