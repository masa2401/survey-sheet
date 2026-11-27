package com.surveysheet.service.definition;

import java.util.ArrayList;
import java.util.List;

import tools.jackson.databind.JsonNode;

/** 検証を通った survey.json から、番号を振った設問のデータを作る */
final class SurveyBuilder {

  private SurveyBuilder() {
  }

  static Definition.Survey build(JsonNode survey) {
    List<Definition.Group> groups = new ArrayList<>();
    for (String array : SurveyRules.GROUP_ARRAYS) {
      boolean selectable = array.equals("selectableGroups");
      for (JsonNode group : survey.get(array)) {
        JsonNode description = group.get("description");
        groups.add(new Definition.Group(
            group.get("id").stringValue(),
            group.get("label").stringValue(),
            selectable,
            description == null ? null : description.stringValue(),
            questions(group.get("questions"))));
      }
    }
    return new Definition.Survey(List.copyOf(groups));
  }

  private static List<Definition.Question> questions(JsonNode nodes) {
    List<Definition.Question> questions = new ArrayList<>();
    for (int i = 0; i < nodes.size(); i++) {
      JsonNode question = nodes.get(i);
      List<Definition.Item> items = new ArrayList<>();
      JsonNode labels = question.get("items");
      for (int j = 0; j < labels.size(); j++) {
        items.add(new Definition.Item(j + 1, labels.get(j).stringValue()));
      }
      questions.add(new Definition.Question(
          i + 1,
          question.get("title").stringValue(),
          question.get("prompt").stringValue(),
          List.copyOf(items)));
    }
    return List.copyOf(questions);
  }
}
