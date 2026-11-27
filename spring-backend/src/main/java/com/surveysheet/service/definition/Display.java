package com.surveysheet.service.definition;

import java.util.List;
import java.util.Map;

/** display.json に既定値を当てた表示設定。値が無い項目（看板が無いときなど）は null にする */
public record Display(
    String appName,
    Scale scale,
    Map<String, String> groupIcons,
    Top top,
    Result result,
    Csv csv) {

  public record Scale(String name, List<String> levels) {
  }

  public record Top(List<String> banner, String heading) {
  }

  public record Result(String heading) {
  }

  public record Csv(String fileName, String itemHeading) {
  }
}
