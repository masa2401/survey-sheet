package com.surveysheet.service.definition;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefinitionLoaderTest {

  private static final DefinitionLoader LOADER = new DefinitionLoader();

  // 全員が回答するグループ1件・選べるグループ2件（説明あり1件・なし1件）
  private static final String SURVEY = """
      {
        "alwaysShownGroups": [
          { "id": "common", "label": "共通", "questions": [
            { "title": "Q1", "prompt": "案内", "items": ["a", "b"] }
          ] }
        ],
        "selectableGroups": [
          { "id": "eng", "label": "エンジニア", "description": "開発者向け", "questions": [
            { "title": "Q1", "prompt": "案内", "items": ["x"] },
            { "title": "Q2", "prompt": "案内", "items": ["y", "z"] }
          ] },
          { "id": "des", "label": "デザイナー", "questions": [
            { "title": "Q1", "prompt": "案内", "items": ["w"] }
          ] }
        ]
      }
      """;

  private static Definition loaded(String survey, String display) {
    DefinitionLoader.Result result = LOADER.load(bytes(survey), bytes(display));
    assertThat(result).isInstanceOf(DefinitionLoader.Loaded.class);
    return ((DefinitionLoader.Loaded) result).definition();
  }

  private static List<DefinitionError> errors(String survey, String display) {
    return errors(bytes(survey), bytes(display));
  }

  private static List<DefinitionError> errors(byte[] survey, byte[] display) {
    DefinitionLoader.Result result = LOADER.load(survey, display);
    assertThat(result).isInstanceOf(DefinitionLoader.Invalid.class);
    return ((DefinitionLoader.Invalid) result).errors();
  }

  private static byte[] bytes(String text) {
    return text.getBytes(StandardCharsets.UTF_8);
  }

  private static byte[] withBom(String text) {
    byte[] content = bytes(text);
    byte[] result = new byte[content.length + 3];
    result[0] = (byte) 0xEF;
    result[1] = (byte) 0xBB;
    result[2] = (byte) 0xBF;
    System.arraycopy(content, 0, result, 3, content.length);
    return result;
  }

  private static String groups(String alwaysShown, String selectable) {
    return "{ \"alwaysShownGroups\": [" + alwaysShown + "], \"selectableGroups\": [" + selectable + "] }";
  }

  private static String group(String id, String label) {
    return "{ \"id\": \"" + id + "\", \"label\": \"" + label
        + "\", \"questions\": [{ \"title\": \"Q\", \"prompt\": \"案内\", \"items\": [\"a\"] }] }";
  }

  @Test
  @DisplayName("同梱のスキルシートのサンプルを読み込める")
  void loadsBundledSample() throws IOException {
    Path dir = Path.of("../surveys/skill-sheet");
    DefinitionLoader.Result result = LOADER.load(
        Files.readAllBytes(dir.resolve("survey.json")),
        Files.readAllBytes(dir.resolve("display.json")));

    assertThat(result).isInstanceOf(DefinitionLoader.Loaded.class);
    Definition definition = ((DefinitionLoader.Loaded) result).definition();
    assertThat(definition.display().appName()).isEqualTo("スキルシート作成ページ");
    assertThat(definition.survey().groups()).extracting(Definition.Group::id).contains("common");
  }

  @Nested
  @DisplayName("設問のデータ")
  class SurveyData {

    @Test
    @DisplayName("グループは alwaysShownGroups → selectableGroups の順に並び、selectable の印が付く")
    void ordersGroupsAndMarksSelectable() {
      List<Definition.Group> groups = loaded(SURVEY, "{}").survey().groups();

      assertThat(groups).extracting(Definition.Group::id).containsExactly("common", "eng", "des");
      assertThat(groups).extracting(Definition.Group::selectable).containsExactly(false, true, true);
    }

    @Test
    @DisplayName("設問と項目に、親の中で1から振り直した番号が付く")
    void numbersQuestionsAndItems() {
      Definition.Group eng = loaded(SURVEY, "{}").survey().groups().get(1);

      assertThat(eng.questions()).extracting(Definition.Question::id).containsExactly(1, 2);
      assertThat(eng.questions().get(1).items())
          .containsExactly(new Definition.Item(1, "y"), new Definition.Item(2, "z"));
    }

    @Test
    @DisplayName("description は書いたグループにだけ入る")
    void keepsDescriptionOnlyWhenWritten() {
      List<Definition.Group> groups = loaded(SURVEY, "{}").survey().groups();

      assertThat(groups).extracting(Definition.Group::description).containsExactly(null, "開発者向け", null);
    }
  }

  @Nested
  @DisplayName("表示設定の既定値")
  class DisplayDefaultValues {

    @Test
    @DisplayName("何も書かなければ、すべての項目に既定値が入る")
    void fillsEveryDefault() {
      Display display = loaded(SURVEY, "{}").display();

      assertThat(display.appName()).isEqualTo("アンケート");
      assertThat(display.scale()).isEqualTo(new Display.Scale("評価", DisplayDefaults.SCALE_LEVELS));
      assertThat(display.groupIcons()).isEmpty();
      assertThat(display.result().heading()).isEqualTo("{name}のアンケート");
      assertThat(display.csv()).isEqualTo(new Display.Csv("{name}_アンケート_{date}", "評価項目"));
    }

    @Test
    @DisplayName("オブジェクトはキーごとに、配列は丸ごと置き換える")
    void mergesObjectsByKeyAndReplacesArrays() {
      Display display = loaded(SURVEY, """
          { "scale": { "name": "習熟度" }, "top": { "banner": ["案内"] } }
          """).display();

      assertThat(display.scale().name()).isEqualTo("習熟度");
      assertThat(display.scale().levels()).isEqualTo(DisplayDefaults.SCALE_LEVELS);
      assertThat(display.top().banner()).containsExactly("案内");
    }

    @Test
    @DisplayName("選べるグループが2件以上なら、複数選択の見出しになり、看板は無い")
    void multipleSelectableGroups() {
      Display.Top top = loaded(SURVEY, "{}").display().top();

      assertThat(top.heading()).isEqualTo(DisplayDefaults.TOP_HEADING_MULTIPLE);
      assertThat(top.banner()).isNull();
    }

    @Test
    @DisplayName("選べるグループが1件なら、単数の見出しになる")
    void singleSelectableGroup() {
      Display.Top top = loaded(groups(group("a", "A"), group("b", "B")), "{}").display().top();

      assertThat(top.heading()).isEqualTo(DisplayDefaults.TOP_HEADING_SINGLE);
    }

    @Test
    @DisplayName("選べるグループが0件なら、見出しは無く、既定の看板が入る")
    void noSelectableGroups() {
      Display.Top top = loaded(groups(group("a", "A"), ""), "{}").display().top();

      assertThat(top.heading()).isNull();
      assertThat(top.banner()).isEqualTo(DisplayDefaults.TOP_BANNER_WITHOUT_SELECTION);
    }

    @Test
    @DisplayName("管理者が書いた見出しと看板は、件数によらずそのまま使う")
    void keepsWrittenTopTexts() {
      Display.Top top = loaded(groups(group("a", "A"), ""), """
          { "top": { "heading": "選んでください", "banner": ["ようこそ"] } }
          """).display().top();

      assertThat(top.heading()).isEqualTo("選んでください");
      assertThat(top.banner()).containsExactly("ようこそ");
    }
  }

  @Nested
  @DisplayName("版")
  class Version {

    @Test
    @DisplayName("survey.json のバイト列の SHA-256 を、16進の小文字で表す")
    void isSha256Hex() {
      assertThat(DefinitionLoader.version(bytes("abc")))
          .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("BOM の有無では変わらない")
    void ignoresBom() {
      assertThat(loaded(SURVEY, "{}").version()).isEqualTo(loaded2(withBom(SURVEY)).version());
    }

    @Test
    @DisplayName("display.json を変えても変わらない")
    void ignoresDisplay() {
      assertThat(loaded(SURVEY, "{}").version())
          .isEqualTo(loaded(SURVEY, "{ \"appName\": \"別の名前\" }").version());
    }

    private Definition loaded2(byte[] survey) {
      DefinitionLoader.Result result = LOADER.load(survey, bytes("{}"));
      assertThat(result).isInstanceOf(DefinitionLoader.Loaded.class);
      return ((DefinitionLoader.Loaded) result).definition();
    }
  }

  @Nested
  @DisplayName("JSON として読む前のエラー")
  class ReadErrors {

    @Test
    @DisplayName("UTF-8 として正しくないバイトは、行と列で示す")
    void rejectsInvalidUtf8() {
      byte[] survey = { '{', '\n', ' ', ' ', (byte) 0xFF, '}' };

      assertThat(errors(survey, bytes("{}"))).containsExactly(DefinitionError.at("survey.json",
          "2行目 3列目",
          "UTF-8 として読めない文字があります。ファイルを UTF-8 で保存し直してください。"));
    }

    @Test
    @DisplayName("同じオブジェクトの中のキーの重複は、行と列とキーの並びで示す")
    void rejectsDuplicateKeys() {
      String display = "{\n  \"scale\": {\n    \"name\": \"a\",\n    \"name\": \"b\"\n  }\n}";

      assertThat(errors(SURVEY, display)).containsExactly(DefinitionError.at("display.json",
          "4行目 11列目（scale.name）", "同じキーが重複して書かれています。"));
    }

    @Test
    @DisplayName("JSON の書き方の間違いは、行と列で示す")
    void rejectsSyntaxErrors() {
      List<DefinitionError> errors = errors(SURVEY, "{\n  \"appName\": \"a\",\n}");

      assertThat(errors).singleElement().satisfies(error -> {
        assertThat(error.locations()).singleElement().asString().startsWith("3行目");
        assertThat(error.message()).startsWith("JSON として読み込めません。");
      });
    }

    @Test
    @DisplayName("空のファイルはエラーにする")
    void rejectsEmptyFile() {
      assertThat(errors(SURVEY, "")).containsExactly(
          DefinitionError.at("display.json", "", "ファイルが空です。"));
    }

    @Test
    @DisplayName("2つのファイルのエラーを、まとめて返す")
    void reportsBothFiles() {
      assertThat(errors("", "")).extracting(DefinitionError::file).containsExactly("survey.json", "display.json");
    }
  }

  @Nested
  @DisplayName("スキーマのエラー")
  class SchemaErrors {

    @Test
    @DisplayName("場所に、グループの名前と設問の見出しを添える")
    void addsNamesToLocation() {
      String items = IntStream.rangeClosed(1, 21).mapToObj(i -> "\"項目" + i + "\"")
          .collect(Collectors.joining(","));
      String survey = groups("{ \"id\": \"common\", \"label\": \"共通スキル\", \"questions\": ["
          + "{ \"title\": \"PC・IT基礎スキル\", \"prompt\": \"案内\", \"items\": [" + items + "] }] }", "");

      assertThat(errors(survey, "{}")).containsExactly(DefinitionError.at("survey.json",
          "alwaysShownGroups[0].questions[0].items（共通スキル ＞ PC・IT基礎スキル）",
          "項目が21件あります。20件までにしてください。"));
    }

    @Test
    @DisplayName("その値自体にエラーがある名前は、括弧に添えない")
    void omitsInvalidNames() {
      String survey = groups("{ \"id\": \"common\", \"label\": \" 先頭\", \"questions\": ["
          + "{ \"title\": \"見出し\", \"prompt\": \"案内\", \"items\": [] }] }", "");

      assertThat(errors(survey, "{}")).extracting(error -> error.locations().getFirst())
          .containsExactlyInAnyOrder(
              "alwaysShownGroups[0].label",
              "alwaysShownGroups[0].questions[0].items（見出し）");
    }

    @Test
    @DisplayName("pattern の違反は、スキーマの patternErrorMessage を使う")
    void usesPatternErrorMessage() {
      assertThat(errors(SURVEY, "{ \"result\": { \"heading\": \"様{name}\" } }")).containsExactly(
          DefinitionError.at("display.json", "result.heading",
              "{name} で始め、{name} はちょうど1つにしてください。ほかに { } や、改行・タブなどの制御文字は使えません。"));
    }

    @Test
    @DisplayName("キーの名前の違反は、親のオブジェクトの場所で示す")
    void reportsPropertyNames() {
      assertThat(errors(SURVEY, "{ \"groupIcons\": { \"Bad_Key\": \"monitor\" } }")).singleElement()
          .satisfies(error -> {
            assertThat(error.locations()).containsExactly("groupIcons");
            assertThat(error.message()).startsWith("キー「Bad_Key」：小文字英字で始め");
          });
    }

    @Test
    @DisplayName("文字数はコードポイントで数える")
    void countsCodePoints() {
      String survey = groups(group("a", "😀".repeat(30)), "");
      String tooLong = groups(group("a", "😀".repeat(31)), "");

      assertThat(LOADER.load(bytes(survey), bytes("{}"))).isInstanceOf(DefinitionLoader.Loaded.class);
      assertThat(errors(tooLong, "{}")).extracting(DefinitionError::message)
          .containsExactly("31文字あります。30文字までにしてください。");
    }

    @Test
    @DisplayName("必須のキーが無いときは、キーの名前を示す")
    void reportsMissingKey() {
      assertThat(errors("{ \"alwaysShownGroups\": [] }", "{}")).containsExactly(
          DefinitionError.at("survey.json", "", "「selectableGroups」のキーがありません。"));
    }

    @Test
    @DisplayName("使えないキーは、制御文字を見える表記にして示す")
    void reportsUnknownKeyVisibly() {
      assertThat(errors(SURVEY, "{ \"app\\nName\": \"a\" }")).extracting(DefinitionError::message)
          .containsExactly("「app\\nName」というキーは使えません。書き間違いがないか確かめてください。");
    }

    @Test
    @DisplayName("型の違いは、$ref の先と元の両方で違反になっても1件にまとめる")
    void deduplicatesTypeErrors() {
      assertThat(errors(SURVEY, "{ \"appName\": 1 }")).containsExactly(
          DefinitionError.at("display.json", "appName", "文字列で書いてください。"));
    }

    @Test
    @DisplayName("同じ設問の中の項目の重複は、重複した文言を示す")
    void reportsDuplicateItems() {
      String survey = groups("{ \"id\": \"a\", \"label\": \"A\", \"questions\": ["
          + "{ \"title\": \"Q\", \"prompt\": \"案内\", \"items\": [\"x\", \"y\", \"x\"] }] }", "");

      assertThat(errors(survey, "{}")).extracting(DefinitionError::message)
          .containsExactly("同じ文言が重複しています（「x」）。");
    }
  }

  @Nested
  @DisplayName("コードの規則")
  class CodeRules {

    @Test
    @DisplayName("グループが1つも無い定義はエラーにする")
    void rejectsNoGroups() {
      assertThat(errors(groups("", ""), "{}")).extracting(DefinitionError::message)
          .containsExactly("グループが1つもありません。alwaysShownGroups か selectableGroups に、1つ以上書いてください。");
    }

    @Test
    @DisplayName("全員が回答するグループが0件で、選べるグループが1件の定義はエラーにする")
    void rejectsSingleSelectableOnly() {
      assertThat(errors(groups("", group("a", "A")), "{}")).containsExactly(DefinitionError.at("survey.json",
          "selectableGroups",
          "全員が回答するグループが無く、選べるグループが1つだけです。このグループは alwaysShownGroups に書いてください。"));
    }

    @Test
    @DisplayName("全員が回答するグループが0件でも、選べるグループが2件なら読み込める")
    void acceptsTwoSelectableOnly() {
      assertThat(LOADER.load(bytes(groups("", group("a", "A") + "," + group("b", "B"))), bytes("{}")))
          .isInstanceOf(DefinitionLoader.Loaded.class);
    }

    @Test
    @DisplayName("グループは2つの配列の合計で10件まで")
    void limitsTotalGroups() {
      String alwaysShown = IntStream.range(0, 6).mapToObj(i -> group("a" + i, "A" + i))
          .collect(Collectors.joining(","));
      String selectable = IntStream.range(0, 5).mapToObj(i -> group("s" + i, "S" + i))
          .collect(Collectors.joining(","));

      assertThat(errors(groups(alwaysShown, selectable), "{}")).extracting(DefinitionError::message)
          .containsExactly("グループが2つの配列の合計で11件あります。合計10件までにしてください。");
    }

    @Test
    @DisplayName("項目は定義全体で500件まで")
    void limitsTotalItems() {
      String items = IntStream.range(0, 20).mapToObj(i -> "\"x" + i + "\"").collect(Collectors.joining(","));
      String question = IntStream.range(0, 26)
          .mapToObj(q -> "{ \"title\": \"Q" + q + "\", \"prompt\": \"案内\", \"items\": [" + items + "] }")
          .collect(Collectors.joining(","));
      String survey = groups("{ \"id\": \"a\", \"label\": \"A\", \"questions\": [" + question + "] }", "");

      assertThat(errors(survey, "{}")).extracting(DefinitionError::message)
          .containsExactly("項目が定義全体で520件あります。500件までにしてください。");
    }

    @Test
    @DisplayName("グループの id の重複は、2つの配列をまたいで、すべての場所を示す")
    void rejectsDuplicateIds() {
      assertThat(errors(groups(group("a", "A"), group("a", "B") + "," + group("c", "C")), "{}"))
          .containsExactly(new DefinitionError("survey.json",
              List.of("alwaysShownGroups[0].id（A）", "selectableGroups[0].id（B）"),
              "グループの id「a」が重複しています。"));
    }

    @Test
    @DisplayName("グループの名前の重複は、2つの配列をまたいで確かめる")
    void rejectsDuplicateLabels() {
      assertThat(errors(groups(group("a", "A"), group("b", "A") + "," + group("c", "C")), "{}"))
          .containsExactly(new DefinitionError("survey.json",
              List.of("alwaysShownGroups[0].label（A）", "selectableGroups[0].label（A）"),
              "グループの名前が重複しています。"));
    }

    @Test
    @DisplayName("設問の見出しの重複は、同じグループの中で確かめる")
    void rejectsDuplicateTitlesInGroup() {
      String survey = groups("{ \"id\": \"a\", \"label\": \"A\", \"questions\": ["
          + "{ \"title\": \"Q\", \"prompt\": \"案内\", \"items\": [\"x\"] },"
          + "{ \"title\": \"Q\", \"prompt\": \"案内\", \"items\": [\"y\"] }] }", group("b", "B") + "," + group("c", "C"));

      assertThat(errors(survey, "{}")).containsExactly(new DefinitionError("survey.json",
          List.of("alwaysShownGroups[0].questions[0].title（A ＞ Q）", "alwaysShownGroups[0].questions[1].title（A ＞ Q）"),
          "同じグループの中で、設問の見出しが重複しています。"));
    }
  }

  @Test
  @DisplayName("エラーの一覧の文は、すべての場所を「ファイル の 場所」の形にし、見つけた順に並べる")
  void formatsReport() {
    String report = DefinitionError.report(List.of(
        DefinitionError.at("survey.json", "", "「selectableGroups」のキーがありません。"),
        new DefinitionError("survey.json",
            List.of("alwaysShownGroups[0].label（A）", "selectableGroups[0].label（A）"),
            "グループの名前が重複しています。"),
        DefinitionError.at("display.json", "4行目 11列目（scale.name）", "同じキーが重複して書かれています。")));

    assertThat(report).isEqualTo("""
        定義ファイルに 3 件の間違いがあります。

        survey.json
          「selectableGroups」のキーがありません。

        survey.json の alwaysShownGroups[0].label（A）
        survey.json の selectableGroups[0].label（A）
          グループの名前が重複しています。

        display.json の 4行目 11列目（scale.name）
          同じキーが重複して書かれています。""");
  }

  @Test
  @DisplayName("エラーは survey.json → display.json の順に並ぶ")
  void ordersErrorsBySurveyThenDisplay() {
    assertThat(errors("{}", "{ \"appName\": 1 }")).extracting(DefinitionError::file)
        .containsExactly("survey.json", "survey.json", "display.json");
  }

  @Test
  @DisplayName("制御文字は、見える表記に置き換える")
  void makesControlCharactersVisible() {
    assertThat(DefinitionError.visible("a\nb\tc\u001bd")).isEqualTo("a\\nb\\tc\\u001bd");
  }
}
