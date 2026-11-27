package com.surveysheet.service.definition;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import tools.jackson.databind.JsonNode;

/**
 * survey.json と display.json のバイト列から、定義を作る。
 * 起動時・テストから同じ処理を呼ぶ
 */
public final class DefinitionLoader {

  public static final String SURVEY_FILE = "survey.json";
  public static final String DISPLAY_FILE = "display.json";

  private final DefinitionFileParser parser = new DefinitionFileParser();
  private final SchemaValidator surveySchema = SchemaValidator.load("survey.schema.json");
  private final SchemaValidator displaySchema = SchemaValidator.load("display.schema.json");

  public sealed interface Result permits Loaded, Invalid {
  }

  public record Loaded(Definition definition) implements Result {
  }

  public record Invalid(List<DefinitionError> errors) implements Result {
    public Invalid {
      errors = List.copyOf(errors);
    }
  }

  public Result load(byte[] surveyBytes, byte[] displayBytes) {
    List<DefinitionError> errors = new ArrayList<>();

    JsonNode survey = parser.parse(SURVEY_FILE, surveyBytes, errors);
    if (survey != null) {
      // コードの規則は、スキーマで形が確かめられた定義にだけ使う
      List<Violation> schemaViolations = surveySchema.validate(survey);
      List<Violation> violations = schemaViolations.isEmpty() ? SurveyRules.check(survey) : schemaViolations;
      errors.addAll(toErrors(SURVEY_FILE, violations, survey, pathsOf(schemaViolations)));
    }

    JsonNode display = parser.parse(DISPLAY_FILE, displayBytes, errors);
    if (display != null) {
      errors.addAll(toErrors(DISPLAY_FILE, displaySchema.validate(display), null, Set.of()));
    }

    if (!errors.isEmpty()) {
      return new Invalid(errors);
    }
    int selectableGroups = survey.get("selectableGroups").size();
    return new Loaded(new Definition(
        version(surveyBytes),
        SurveyBuilder.build(survey),
        DisplayDefaults.apply(display, selectableGroups)));
  }

  /** 版は、BOM を取り除いた後の survey.json のバイト列の SHA-256（16進の小文字） */
  static String version(byte[] surveyBytes) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(DefinitionFileParser.stripBom(surveyBytes)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Set<KeyPath> pathsOf(List<Violation> violations) {
    return violations.stream()
        .flatMap(v -> v.paths().stream())
        .collect(Collectors.toSet());
  }

  /** invalid は、スキーマのエラーがある値の場所。その値は、名前として括弧に添えない */
  private static List<DefinitionError> toErrors(String file, List<Violation> violations, JsonNode survey,
      Set<KeyPath> invalid) {
    return violations.stream()
        .map(v -> new DefinitionError(file, v.paths()
            .stream()
            .map(path -> location(path, survey, invalid))
            .toList(), v.message()))
        .toList();
  }

  // 名前（グループの名前・設問の見出し）は、survey.json のエラーにだけ添える。
  // 値が文字列で、その値自体にスキーマのエラーが無い階層だけを括弧に入れる
  private static String location(KeyPath path, JsonNode survey, Set<KeyPath> invalid) {
    if (path.isRoot()) {
      return "";
    }
    if (survey == null) {
      return path.toString();
    }
    List<Object> parts = path.parts();
    List<String> names = new ArrayList<>();
    if (parts.size() >= 2 && parts.get(1) instanceof Integer) {
      KeyPath group = new KeyPath(parts.subList(0, 2));
      addName(names, survey, group.key("label"), invalid);
      if (parts.size() >= 4 && "questions".equals(parts.get(2)) && parts.get(3) instanceof Integer) {
        addName(names, survey, new KeyPath(parts.subList(0, 4)).key("title"), invalid);
      }
    }
    return names.isEmpty() ? path.toString() : path + "（" + String.join(" ＞ ", names) + "）";
  }

  private static void addName(List<String> names, JsonNode survey, KeyPath namePath, Set<KeyPath> invalid) {
    JsonNode node = survey;
    for (Object part : namePath.parts()) {
      node = part instanceof Integer index ? node.path(index) : node.path((String) part);
    }
    if (node.isString() && !invalid.contains(namePath)) {
      names.add(DefinitionError.visible(node.stringValue()));
    }
  }
}
