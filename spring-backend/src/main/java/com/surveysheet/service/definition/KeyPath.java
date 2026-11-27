package com.surveysheet.service.definition;

import java.util.ArrayList;
import java.util.List;

/** 定義ファイルの中の場所。要素はキー（String）か、配列の番号（Integer） */
record KeyPath(List<Object> parts) {

  static final KeyPath ROOT = new KeyPath(List.of());

  KeyPath {
    parts = List.copyOf(parts);
  }

  KeyPath key(String key) {
    return append(key);
  }

  KeyPath index(int index) {
    return append(index);
  }

  boolean isRoot() {
    return parts.isEmpty();
  }

  /** 番号は0から数え、`a[0].b` の形で書く */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Object part : parts) {
      if (part instanceof Integer index) {
        sb.append('[').append(index).append(']');
      } else {
        if (!sb.isEmpty()) {
          sb.append('.');
        }
        sb.append(DefinitionError.visible((String) part));
      }
    }
    return sb.toString();
  }

  private KeyPath append(Object part) {
    List<Object> next = new ArrayList<>(parts);
    next.add(part);
    return new KeyPath(next);
  }
}
