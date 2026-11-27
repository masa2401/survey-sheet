package com.surveysheet.service.definition;

import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** 検証を通った display.json に、既定値を当てる */
final class DisplayDefaults {

  static final String APP_NAME = "アンケート";
  static final String SCALE_NAME = "評価";
  static final List<String> SCALE_LEVELS = List.of("低い", "やや低い", "ふつう", "やや高い", "高い");
  static final String RESULT_HEADING = "{name}のアンケート";
  static final String CSV_FILE_NAME = "{name}_アンケート_{date}";
  static final String CSV_ITEM_HEADING = "評価項目";

  static final String TOP_HEADING_MULTIPLE = "該当するカテゴリを選択してください(複数選択可)";
  static final String TOP_HEADING_SINGLE = "該当するカテゴリを選択してください";
  static final List<String> TOP_BANNER_WITHOUT_SELECTION = List.of("下のボタンからアンケートを始めてください。");

  private static final JsonMapper MAPPER = JsonMapper.builder().build();

  private DisplayDefaults() {
  }

  static Display apply(JsonNode display, int selectableGroups) {
    ObjectNode merged = MAPPER.valueToTree(Map.of(
        "appName", APP_NAME,
        "scale", Map.of("name", SCALE_NAME, "levels", SCALE_LEVELS),
        "groupIcons", Map.of(),
        "top", Map.of(),
        "result", Map.of("heading", RESULT_HEADING),
        "csv", Map.of("fileName", CSV_FILE_NAME, "itemHeading", CSV_ITEM_HEADING)));
    ObjectNode written = (ObjectNode) display.deepCopy();
    written.remove("$schema");
    mergeInto(merged, written);

    ObjectNode top = (ObjectNode) merged.get("top");
    if (!top.has("heading")) {
      top.put("heading", topHeading(selectableGroups));
    }
    if (!top.has("banner") && selectableGroups == 0) {
      top.set("banner", MAPPER.valueToTree(TOP_BANNER_WITHOUT_SELECTION));
    }
    return MAPPER.treeToValue(merged, Display.class);
  }

  // 選ぶ欄が無い（0件）ときは、見出しを出さないので null にする
  private static String topHeading(int selectableGroups) {
    return switch (selectableGroups) {
      case 0 -> null;
      case 1 -> TOP_HEADING_SINGLE;
      default -> TOP_HEADING_MULTIPLE;
    };
  }

  // オブジェクトはキーごとに、配列と値は丸ごと置き換える
  private static void mergeInto(ObjectNode target, ObjectNode source) {
    source.properties().forEach(entry -> {
      JsonNode current = target.get(entry.getKey());
      if (current instanceof ObjectNode currentObject && entry.getValue() instanceof ObjectNode sourceObject) {
        mergeInto(currentObject, sourceObject);
      } else {
        target.set(entry.getKey(), entry.getValue());
      }
    });
  }
}
