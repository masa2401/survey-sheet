package com.surveysheet.service.definition;

import java.util.List;

/** 起動時の検証を通り、番号・版・表示設定の既定値を当てた定義。値が無い項目は、キーを残して null にする */
public record Definition(String version, Survey survey, Display display) {

  /** グループは alwaysShownGroups → selectableGroups の順に、1つの配列にまとめる */
  public record Survey(List<Group> groups) {
  }

  /** description の値が入るのは、選べるグループで説明を書いたときだけ */
  public record Group(String id, String label, boolean selectable, String description, List<Question> questions) {
  }

  /** 設問と項目の id は、配列の順に振った番号（1始まり、親の中で振り直す） */
  public record Question(int id, String title, String prompt, List<Item> items) {
  }

  public record Item(int id, String label) {
  }
}
