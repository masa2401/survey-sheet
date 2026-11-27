package com.surveysheet.service.definition;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import tools.jackson.databind.JsonNode;

/** survey.json の、スキーマで表せない規則。スキーマの検証を通った定義に使う */
final class SurveyRules {

  static final List<String> GROUP_ARRAYS = List.of("alwaysShownGroups", "selectableGroups");

  private static final int MAX_GROUPS = 10;
  private static final int MAX_ITEMS = 500;

  private SurveyRules() {
  }

  static List<Violation> check(JsonNode survey) {
    List<Violation> violations = new ArrayList<>();
    int alwaysShown = survey.get("alwaysShownGroups").size();
    int selectable = survey.get("selectableGroups").size();

    if (alwaysShown == 0 && selectable == 0) {
      violations.add(Violation.at(KeyPath.ROOT,
          "グループが1つもありません。alwaysShownGroups か selectableGroups に、1つ以上書いてください。"));
    }
    if (alwaysShown == 0 && selectable == 1) {
      violations.add(Violation.at(new KeyPath(List.of("selectableGroups")),
          "全員が回答するグループが無く、選べるグループが1つだけです。このグループは alwaysShownGroups に書いてください。"));
    }
    if (alwaysShown + selectable > MAX_GROUPS) {
      violations.add(Violation.at(KeyPath.ROOT, "グループが2つの配列の合計で" + (alwaysShown + selectable)
          + "件あります。合計" + MAX_GROUPS + "件までにしてください。"));
    }

    int items = 0;
    for (String array : GROUP_ARRAYS) {
      for (JsonNode group : survey.get(array)) {
        for (JsonNode question : group.get("questions")) {
          items += question.get("items").size();
        }
      }
    }
    if (items > MAX_ITEMS) {
      violations.add(Violation.at(KeyPath.ROOT,
          "項目が定義全体で" + items + "件あります。" + MAX_ITEMS + "件までにしてください。"));
    }

    List<Located> groups = new ArrayList<>();
    for (String array : GROUP_ARRAYS) {
      JsonNode nodes = survey.get(array);
      for (int i = 0; i < nodes.size(); i++) {
        groups.add(new Located(new KeyPath(List.of(array)).index(i), nodes.get(i)));
      }
    }
    duplicates(groups, "id", value -> "グループの id「" + DefinitionError.visible(value) + "」が重複しています。",
        violations);
    duplicates(groups, "label", value -> "グループの名前が重複しています。", violations);

    for (Located group : groups) {
      List<Located> questions = new ArrayList<>();
      JsonNode nodes = group.node().get("questions");
      for (int i = 0; i < nodes.size(); i++) {
        questions.add(new Located(group.path().key("questions").index(i), nodes.get(i)));
      }
      duplicates(questions, "title", value -> "同じグループの中で、設問の見出しが重複しています。", violations);
    }
    return violations;
  }

  // 比べ方は完全一致。重複する値のすべての場所を、1つのエラーにまとめる
  private static void duplicates(List<Located> entries, String key, Function<String, String> message,
      List<Violation> violations) {
    Map<String, List<KeyPath>> byValue = new LinkedHashMap<>();
    for (Located entry : entries) {
      byValue.computeIfAbsent(entry.node().get(key).stringValue(), v -> new ArrayList<>())
          .add(entry.path().key(key));
    }
    byValue.forEach((value, paths) -> {
      if (paths.size() > 1) {
        violations.add(new Violation(paths, message.apply(value)));
      }
    });
  }

  private record Located(KeyPath path, JsonNode node) {
  }
}
