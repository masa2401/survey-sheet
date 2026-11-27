package com.surveysheet.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.surveysheet.exception.StartupException;

class DefinitionServiceTest {

  private static final String SAMPLE_DIR = "../surveys/skill-sheet";

  @Test
  @DisplayName("SURVEY_DIR の定義ファイルを読み込み、名前の上限を持つ")
  void loadsDefinition() {
    DefinitionService service = new DefinitionService(SAMPLE_DIR, "20");

    assertThat(service.getDefinition().version()).hasSize(64);
    assertThat(service.getUserNameMaxLength()).isEqualTo(20);
  }

  @Test
  @DisplayName("SURVEY_DIR が未設定なら、起動を止める")
  void rejectsMissingSurveyDir() {
    assertThatThrownBy(() -> new DefinitionService("", "20"))
        .isInstanceOf(StartupException.class)
        .hasMessageContaining("SURVEY_DIR が設定されていません");
  }

  @Test
  @DisplayName("SURVEY_DIR のフォルダが無ければ、起動を止める")
  void rejectsMissingDirectory(@TempDir Path dir) {
    String missing = dir.resolve("missing").toString();

    assertThatThrownBy(() -> new DefinitionService(missing, "20"))
        .isInstanceOf(StartupException.class)
        .hasMessage("SURVEY_DIR のフォルダが見つかりません（" + missing + "）。");
  }

  @Test
  @DisplayName("無いファイルを、まとめて示す")
  void rejectsMissingFiles(@TempDir Path dir) {
    assertThatThrownBy(() -> new DefinitionService(dir.toString(), "20"))
        .isInstanceOf(StartupException.class)
        .hasMessage("survey.json・display.json がありません（" + dir + "）。");
  }

  @Test
  @DisplayName("定義ファイルに間違いがあれば、エラーの一覧で起動を止める")
  void rejectsInvalidDefinition(@TempDir Path dir) throws IOException {
    Files.writeString(dir.resolve("survey.json"), "{}");
    Files.writeString(dir.resolve("display.json"), "{}");

    assertThatThrownBy(() -> new DefinitionService(dir.toString(), "20"))
        .isInstanceOf(StartupException.class)
        .hasMessageStartingWith("定義ファイルに 2 件の間違いがあります。");
  }

  @ParameterizedTest
  @ValueSource(strings = { "0", "101", "abc", "" })
  @DisplayName("名前の上限が 1〜100 の整数でなければ、起動を止める")
  void rejectsInvalidUserNameMaxLength(String value) {
    assertThatThrownBy(() -> new DefinitionService(SAMPLE_DIR, value))
        .isInstanceOf(StartupException.class)
        .hasMessageStartingWith("USER_NAME_MAX_LENGTH は 1〜100 の整数にしてください");
  }

  @ParameterizedTest
  @ValueSource(strings = { "1", "100" })
  @DisplayName("名前の上限は、1 と 100 を受け付ける")
  void acceptsBoundaryUserNameMaxLength(String value) {
    assertThat(new DefinitionService(SAMPLE_DIR, value).getUserNameMaxLength()).isEqualTo(Integer.parseInt(value));
  }
}
