package com.surveysheet.service.definition;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 定義ファイルの間違い1件。場所はファイルの名前を含めず、キーの並びと名前（例：{@code alwaysShownGroups[0].label（共通スキル）}）か、
 * 行と列（例：{@code 12行目 5列目}）で示す。ファイル全体の間違いは、場所を空にする
 */
public record DefinitionError(String file, List<String> locations, String message) {

  public DefinitionError {
    locations = List.copyOf(locations);
  }

  static DefinitionError at(String file, String location, String message) {
    return new DefinitionError(file, List.of(location), message);
  }

  /** 定義の値をエラーの文に入れるときは、必ずここを通す。制御文字を {@code \n} や {@code \u001b} の表記に置き換える */
  static String visible(String value) {
    StringBuilder sb = new StringBuilder();
    value.codePoints().forEach(cp -> {
      if (Character.getType(cp) != Character.CONTROL) {
        sb.appendCodePoint(cp);
        return;
      }
      switch (cp) {
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> sb.append(String.format("\\u%04x", cp));
      }
    });
    return sb.toString();
  }

  /** 起動を止めるときに出す、エラーの一覧の文。場所はすべて「ファイル の 場所」の形にする */
  public static String report(List<DefinitionError> errors) {
    String body = errors.stream()
        .map(DefinitionError::format)
        .collect(Collectors.joining("\n\n"));
    return "定義ファイルに " + errors.size() + " 件の間違いがあります。\n\n" + body;
  }

  private String format() {
    String places = locations.stream()
        .map(location -> location.isEmpty() ? file : file + " の " + location)
        .collect(Collectors.joining("\n"));
    return places + "\n  " + message;
  }
}
