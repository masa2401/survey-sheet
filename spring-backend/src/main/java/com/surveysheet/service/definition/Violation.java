package com.surveysheet.service.definition;

import java.util.List;

/** スキーマとコードの規則の違反。重複のエラーは、重複する値のすべての場所を持つ */
record Violation(List<KeyPath> paths, String message) {

  Violation {
    paths = List.copyOf(paths);
  }

  static Violation at(KeyPath path, String message) {
    return new Violation(List.of(path), message);
  }
}
