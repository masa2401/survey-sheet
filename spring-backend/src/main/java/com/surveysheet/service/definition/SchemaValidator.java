package com.surveysheet.service.definition;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.dialect.Dialect;
import com.networknt.schema.dialect.Dialects;
import com.networknt.schema.keyword.NonValidationKeyword;
import com.networknt.schema.path.NodePath;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** クラスパスの schema/ にある JSON Schema で検証し、違反を日本語の文にする */
final class SchemaValidator {

  private static final SchemaRegistry REGISTRY = SchemaRegistry.withDialect(
      Dialect.builder(Dialects.getDraft202012())
          .keyword(new NonValidationKeyword("patternErrorMessage"))
          .build());

  private static final Map<String, String> COUNT_NOUNS = Map.of(
      "alwaysShownGroups", "グループ",
      "selectableGroups", "グループ",
      "questions", "設問",
      "items", "項目",
      "levels", "段階",
      "banner", "段落");

  private static final Map<String, String> TYPE_NAMES = Map.of(
      "string", "文字列",
      "array", "配列",
      "object", "オブジェクト",
      "integer", "整数",
      "number", "数値",
      "boolean", "true か false");

  private final JsonNode schemaNode;
  private final Schema schema;

  private SchemaValidator(JsonNode schemaNode, Schema schema) {
    this.schemaNode = schemaNode;
    this.schema = schema;
  }

  static SchemaValidator load(String schemaFile) {
    String resource = "schema/" + schemaFile;
    try (InputStream in = SchemaValidator.class.getClassLoader().getResourceAsStream(resource)) {
      if (in == null) {
        throw new IllegalStateException("クラスパスに " + resource + " がありません");
      }
      JsonNode node = JsonMapper.builder().build().readTree(in);
      // スキーマの $id は相対の名前なので、置き場所を基準として渡す
      Schema schema = REGISTRY.getSchema(SchemaLocation.of("classpath:" + resource), node);
      return new SchemaValidator(node, schema);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  List<Violation> validate(JsonNode instance) {
    // 同じ規則が $ref の先と元の両方で違反になることがあるので、場所と文が同じものはまとめる
    Set<Violation> violations = new LinkedHashSet<>();
    for (Error error : schema.validate(instance)) {
      violations.add(Violation.at(toKeyPath(error.getInstanceLocation()), message(error)));
    }
    return new ArrayList<>(violations);
  }

  private String message(Error error) {
    Object[] args = error.getArguments();
    JsonNode instance = error.getInstanceNode();
    return switch (error.getKeyword()) {
      case "required" -> "「" + DefinitionError.visible(error.getProperty()) + "」のキーがありません。";
      case "additionalProperties" -> "「" + DefinitionError.visible(error.getProperty())
          + "」というキーは使えません。書き間違いがないか確かめてください。";
      case "type" -> TYPE_NAMES.getOrDefault(String.valueOf(args[1]), String.valueOf(args[1])) + "で書いてください。";
      case "minItems" -> countNoun(error) + "が" + instance.size() + "件です。" + args[0] + "件以上にしてください。";
      case "maxItems" -> countNoun(error) + "が" + instance.size() + "件あります。" + args[0] + "件までにしてください。";
      case "minLength" -> Integer.parseInt(String.valueOf(args[0])) <= 1
          ? "空欄にはできません。"
          : length(instance) + "文字です。" + args[0] + "文字以上にしてください。";
      case "maxLength" -> length(instance) + "文字あります。" + args[0] + "文字までにしてください。";
      case "pattern" -> patternMessage(parentOf(error.getSchemaLocation()));
      case "propertyNames" -> "キー「" + DefinitionError.visible(error.getProperty()) + "」："
          + patternMessage(nodeAt(error.getSchemaLocation()));
      case "uniqueItems" -> "同じ文言が重複しています（" + duplicatesOf(instance) + "）。";
      default -> "スキーマの規則（" + error.getKeyword() + "）に合いません。";
    };
  }

  private static String countNoun(Error error) {
    NodePath location = error.getInstanceLocation();
    int count = location.getNameCount();
    Object last = count == 0 ? null : location.getElement(count - 1);
    return last instanceof String key ? COUNT_NOUNS.getOrDefault(key, "要素") : "要素";
  }

  // スキーマの文字数の数え方（コードポイント）に合わせる
  private static int length(JsonNode instance) {
    String value = instance.stringValue();
    return value.codePointCount(0, value.length());
  }

  private static String patternMessage(JsonNode schemaObject) {
    JsonNode message = schemaObject.path("patternErrorMessage");
    return message.isString() ? message.stringValue() : "書き方が正しくありません。";
  }

  private JsonNode nodeAt(SchemaLocation location) {
    return schemaNode.at(JsonPointer.compile(location.getFragment().toString()));
  }

  private JsonNode parentOf(SchemaLocation location) {
    JsonPointer pointer = JsonPointer.compile(location.getFragment().toString()).head();
    return pointer == null ? schemaNode : schemaNode.at(pointer);
  }

  private static String duplicatesOf(JsonNode array) {
    Map<JsonNode, Integer> counts = new HashMap<>();
    array.forEach(value -> counts.merge(value, 1, Integer::sum));
    Set<String> duplicates = new LinkedHashSet<>();
    array.forEach(value -> {
      if (counts.get(value) > 1) {
        duplicates.add("「" + DefinitionError.visible(value.isString() ? value.stringValue() : value.toString()) + "」");
      }
    });
    return duplicates.stream().collect(Collectors.joining("、"));
  }

  private static KeyPath toKeyPath(NodePath nodePath) {
    List<Object> parts = new ArrayList<>();
    for (int i = 0; i < nodePath.getNameCount(); i++) {
      parts.add(nodePath.getElement(i));
    }
    return new KeyPath(parts);
  }
}
