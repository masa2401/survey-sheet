# 引き継ぎ

最終更新：2026-11-10

## 現在地

- 段階2（設問の定義の形と、JSON Schema の作成）の検討は完了し、スキーマとサンプルに反映済み
- スキーマは `schema/`、スキルシートのサンプルは `surveys/skill-sheet/` にある
- アプリの中身は、移行前のスキルシート作成アプリのまま
- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）
- Dependabot は alerts と security updates を有効にしている。version updates は GitHub Actions だけ（`dependabot.yml`）

## 次の作業

1. VS Code でスキーマのキーワードが効くかを確かめる
   - `$ref` と並べたキーワード（`maxLength`）
   - `additionalProperties`、`uniqueItems`、`propertyNames`
   - `maxLength` の数え方（`𠮷` や絵文字を1文字と数えるか）
   - 配列の `minItems`・`maxItems`（`top.banner`）
   - Ajv（Draft 2020-12、strict モード）では確認済み
2. 段階3に進む

## 段階3で行うこと

- 固定の文言を `src/data/texts.ts` に移し、`composables/useTexts.ts` を作る
  - `constants.ts` の `LEVEL_LABELS` の説明は `scale.levels` に置き換える（星の文字列は残す）
  - `csvUtils.ts` を、完成したファイル名と見出しを引数で受け取る形にする（`csvUtils.test.ts` の呼び出しも直す）
- 文面の変更と、直すテスト
  - 「お名前」→「名前」：`EditableNameHeading.vue:23, 119`、`useGuestGate.ts:6`
    - テスト：`EditableNameHeading.test.ts:12, 14`、e2e の `share.spec.ts:175`、`pages/ResultPage.ts:20`
  - 共有ビューのボタン（`ResultPage.vue:254`）
    - テスト：`App.test.ts:107`、`ResultPage.test.ts:213, 218, 277, 284`
  - リンク切れの文（`ResultPage.vue:124`）、CSV の1行目（`csvUtils.ts:11`）：テストの修正は不要
- TOP（`TopPage.vue`）
  - グループの `description` が無いときは、説明の `<p>` と `aria-describedby` を出さない
  - `selectableGroups` が0件なら選ぶ欄を出さない
  - 選ぶ欄の見出しの既定値と補足を、件数で切り替える
  - 全員が回答するグループが0件のときの開始ボタン（ツールチップと、押したときのフォーカス）
    - カードの枠は `has-[:focus-visible]` のため、プログラムからのフォーカスで出ない可能性がある。実機で確かめ、出なければカードを短く強調するなどで補う
  - `top.banner` の看板。本文は `text-sm`（14px）にする案
  - 看板か選ぶ欄の、どちらかは必ず表示する（看板が無いときの扱いは実装時に決める）
- 回答ページ（`/survey`）：表示するグループが無いときは TOP へ移すガードを足す（`router/index.ts`）
- グループの並び順を、全画面・CSV・PDF で揃える
- `index.html` の `<title>`：表示設定を読み込むまでの間に出す文字列を決める
- 全員が回答するグループが0件の定義を使うテストを足す
- フロントが定義をどう読むかを決める（バックエンドの API ができる前の段階）
  - `surveys/` を読む場合は、コンテナへのマウント（今の `docker-compose.yml` は `./vue-frontend` だけ）と、Vite の設定（`vue-frontend/` の外のファイルを読めるか）を確かめる

## 段階4以降で行うこと

- 起動時の検証に、`alwaysShownGroups` が0件で `selectableGroups` が1件の定義をエラーにする規則を足す（段階4）
- `SaveRateLimiter.java:50` の文から「スキルシート」を外す（段階4）
- `aws-lambda/src/index.ts:58` の `filename="skillsheet.pdf"` を汎用の名前にする（段階5）
- `TheFooter.vue` の GitHub のリンク先（旧リポジトリ `FullStack-CreateSkillSheet`）を差し替える（段階8）

## 未着手の整理

- `DESIGN_NOTES.md` を削除し、`README.md` の「設計ノート」節を直す
- `README.md` を、利用者向けの導入手順書に書き換える（時期は未定）
  - スキーマの解説もここに書く（`additionalProperties` で書き間違いを検出していること、グループの合計10件・項目の総数500件の上限など）

## 未決事項

- スキーマのライブラリを選ぶとき（段階4の Java、段階7の Go）に確かめること
  - 正規表現の `\p{Cc}`・`\p{Zs}` が使えるか（Ajv・VS Code・Go の標準の正規表現は確認済み。Java は未確認）
  - `maxLength` を Unicode の文字数（コードポイントの数）で数えるか
  - キーの重複のエラーに、行番号などの位置の情報が付くか（Jackson の `StreamReadException`、Go の v2 のエラー）
  - JSON として読み込めなかったときのエラーの文に、ファイルの中身が含まれるか（含まれるなら、制御文字の置き換えの対象にする）
- クエリ方式の共有を削除した後、保存に失敗したときの共有ボタンの挙動
  - 今は代わりにクエリ方式の URL をコピーしている（`ShareUrlButton.vue`）
  - 案：ボタンにエラーを表示し、やり直せるようにする
- クエリ方式の共有を削除する時期
  - 案：段階4（e2e をバックエンドありの形に変えるとき）で削除し、`isBackendEnabled()` の整理は段階5で行う
  - `isBackendEnabled()` は、PDF ボタンの表示（`ShareButton.vue`）や `api.ts` でも使っている
  - 削除の対象：`shareUtils.ts` の圧縮・展開、`lz-string`、`ResultPage.vue` と `router/index.ts` の `data=`、関連するテスト、e2e の `share.spec.ts` と `e2e/pages/ResultPage.ts` の `data=` の扱い
- Spring Boot 版と Go 版で検証の規則を揃える方法
  - 案：正しい定義と、間違いを1つずつ入れた定義を JSON で用意し、期待するエラーと一緒にリポジトリに置く
  - Spring Boot と Go の両方のテストで同じファイルを読み、スキーマのファイルも共用する
  - 規則を1つずつすべて試す（`alwaysShownGroups` 0件・`selectableGroups` 1件の形も入れる）
  - 期待するエラーは順序によらずに比べる
  - 置き場所は作るときに決める（`SURVEY_DIR` で切り替えて読む案）
- 保存したシートを読み出すとき、グループ・設問・項目を定義の順で返す（段階4で作り直すとき）
  - フロントは API の順序のまま並べる（`sheetMapper.ts` の `toSurveyState`）
  - グループ：`SheetCategoryRepository.findWithAnswersBySheetId` のクエリに `ORDER BY` が無い
  - 設問：`SkillSheetService.buildQuestions` が `Collectors.groupingBy`（内部は `HashMap`）でまとめている
  - 案：順序を保つ形（`ORDER BY`、`LinkedHashMap`、番号で並べ替える）にするか、読み込んだ定義の順で並べ直して返す
- 保存の DB との往復の削減（JDBC のバッチ、連番をまとめて取得する設定）
  - 必要になったら段階4で検討する
  - Go 版では、最初から複数行をまとめて INSERT する書き方にする
- CI でスキーマを Ajv の strict モードで読み込むか（CI を検討するときに決める）
  - 使う場合は、`ajv.addKeyword("patternErrorMessage")` で登録する
- 2つ目のサンプル定義のテーマ（機能の改修が終わってから選ぶ）
  - `selectableGroups` が0件の構成にすると、看板の例も示せる
  - 候補：研修・イベントの満足度（参加したセッションをチェックする）、社内ツールの利用状況、店舗・サービスの評価
- Go 版で `schema/` をどう取り込むか（段階7）
  - `//go:embed` は `..` を含むパスを書けないので、モジュールを `go-backend/` などに置くと、直下の `schema/` を直接埋め込めない
- 配布物の組み方（段階8）
  - 案：リリースごとに、イメージの版と設定ファイル一式を組にして配る
- ライセンス（開発が進んでから決める）
- レンタルサーバーで動く PHP 版を作るか
- 版のハッシュの計算方法（段階4で実装するときに決める）
  - 候補：定義ファイルのバイト列から計算する案と、JSON を整えてから計算する案
  - バイト列から計算する場合は、BOM を取り除く前と後のどちらで計算するかも決める
- `vue-frontend` の TypeScript 6 への移行
  - 上げる場合は `typescript@~6.0.3` と `@vue/tsconfig@^0.9.1` を揃えて上げる
  - typescript-eslint の対応範囲が `<6.1.0` のため、`^` ではなく `~` にする
  - npm の `latest` は 7 系なので、バージョンは明示して入れる

## 依存関係

- `vue-frontend` には moderate の脆弱性が25件残っている。上流の更新を待つ
  - `minimatch`（<11.0.1）と `glob`（<14.0.2）。どちらも開発用の依存（ESLint 9 系、prettier のプラグイン、`@vue/test-utils`）
- `aws-lambda` の脆弱性は0件
- `@vitest/eslint-plugin` はロックファイル上 1.6.27
  - 1.6.28 は TypeScript 6 以上が前提のため、引数なしの `npm update` は衝突で止まる
  - 更新は、対象のパッケージを指定して行う

## 環境の注意

- macOS では、`npm ci` のたびに fsevents の install script がブロックされた警告が出る。動作には影響しないため、`allowScripts` には追加していない
- Apple Silicon の Mac で、次の点は未確認
  - `eclipse-temurin:25-jre-alpine` が arm64 で動くか
  - PDF 生成の Lambda イメージ（Puppeteer）がローカルでビルドできるか（本番用のイメージは CI の amd64 でビルドしている）
