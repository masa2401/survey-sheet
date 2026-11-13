# 引き継ぎ

最終更新：2026-11-12

## 現在地

- 段階2（設問の定義の形と、JSON Schema の作成）は完了し、`main` にマージ済み（#3）
  - スキーマは `schema/`、スキルシートのサンプルは `surveys/skill-sheet/` にある
  - VS Code でのスキーマのキーワードの効き方も確認済み
- 段階3と4をまとめて検討している。実装は、バックエンドの定義の API を先に作る
- 定義の API の形は、アイコンと印のキーの名前を除いて決定済み（`DECISIONS.md` の「定義の API」）
- シートの API は、グループの識別子と版の扱いまで決定済み（`DECISIONS.md` の「シートの API」）
- PDF の構成は、描画役・保存先・状態の持ち方・生成のきっかけまで決定済み（`DECISIONS.md` の「PDF 生成」）
- アプリの中身は、移行前のスキルシート作成アプリのまま
- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）
- Dependabot は alerts と security updates を有効にしている。version updates は GitHub Actions だけ（`dependabot.yml`）

## 完成形のイメージ（段階4を終えた時点）

- バックエンドは起動時に `SURVEY_DIR` の `survey.json` と `display.json` を読んで検証し、番号を振って版を計算し、メモリに持つ
- 定義の API は、メモリに持った内容を返すだけ
- シートの API は、名前と、選ばれたグループの回答（番号と値）を保存・読み出しする
- DB には回答の番号と値だけを保存する
- フロントは画面を開いたときに定義の API を読み、定義から画面を組み立てる。定義と回答の結合（`useMergedSurvey.ts`）と CSV の作成はフロントで行う

## 次の作業：段階3・4の検討の続き

1つずつ、上から順に決める。

1. PDF の残り
   1. 二重の生成の防止と、描画役に同時に頼む数の上限
      - 今は `LambdaPdfService` の `lastInvokedAt` で、直近の Invoke から15秒以内の `regenerate` を止めている
   2. `/status` でシートの有無・期限・版を確かめるか
      - 今は確かめず、PDF があれば返している（`PdfController.status`）
   3. PDF 用のコンテナの描画役を何にするか（既製のものか、`aws-lambda/` の処理をもとに自前で作るか）
2. シートの保存・読み出しの API
   1. 保存時に、送られてきた番号が定義にあるかを確かめるか（今は確かめていない）
   2. DTO の件数の上限（今はカテゴリ10・設問30・回答30）を、定義の上限（グループ10・設問30・項目20）に合わせるか
   3. 読み出しの順序
      - 未決事項の案は「定義の順で返す」
      - `SheetCategoryRepository.findWithAnswersBySheetId` に `ORDER BY` が無く、`SkillSheetService.buildQuestions` は `Collectors.groupingBy`（`HashMap`）でまとめている
      - 一方で、共有ビューは `ResultPage.vue:55` で `normalizeSurveyState` を通し、定義の順に並べ直している。バックエンドで順序を保証する必要があるかを確かめる
   4. API のキーの名前（`categoryId`・`answerId` などを `groups`・`items` に合わせるか）
3. フロントの読み込みと状態
   - 読み込みを待つ場所（`main.ts` で待ってから `app.mount()` するか、マウントしてから `App.vue` で読み込み中を出すか）
   - 読み込みに失敗したときの表示
   - ストアの初期化（今はストアを作るときに `buildInitialSelections()` で組み立て、`pinia-plugin-persistedstate` でブラウザに保存している。定義が届いてから組み立てる形と、保存された状態の版の確認をどこに置くか）
   - アプリの中の型（API の形をそのまま使うか、今の `CategoryMaster`・`Merged*` に変換するか）
   - `index.html` の `<title>`：読み込むまでの間に出す文字列
   - PDF：描画役は結果ページの `data-pdf-ready` を待って印刷している（`aws-lambda/src/index.ts:105`）。定義の読み込みより後に付くかを確かめる
4. 定義の API の残り
   - 全員が回答するグループかの印のキーの名前
   - アイコンの渡し方と配信（Lucide の SVG をどこから配信するか、API が返すのは名前か URL か）
5. バックエンドの内部
   - Spring Boot 版で `schema/` を取り込む方法（`spring-backend/` の外にある。Maven のリソースにできるか、Docker のビルドの `context`（`./spring-backend`）から見えるかは未確認）
   - 開発環境で `surveys/` を読ませる方法（`docker-compose.yml` のバックエンドは `./spring-backend` だけをマウントしている。`surveys/` のマウントと `SURVEY_DIR` の設定が要る）
   - Java の検証ライブラリ（未決事項の「スキーマのライブラリを選ぶとき」）

次のものは、フロントの作りに影響しないため、段階4の実装のときに決める。

- エンドポイントのパス
- エラーの応答の形（今は `ProblemDetail`）。版が違うときの応答のステータスコードと本文も、ここで決める
- `shareToken` の列の扱い
- 保存の DB との往復の削減

実装のときに確かめること。

- `ddl-auto=validate` で、`String` の項目と `text` の列の照合が通るか
- 描画役を待つ時間の上限（SDK と HTTP クライアントの読み取りのタイムアウト）を、Lambda のタイムアウト（30秒）より長くする
- PDF の失敗の記録を消す時期（再試行のときと、残った記録の定期削除）
- フロントの60秒の打ち切りは、`/status` が failed を返すようになっても、受け皿として残す

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
- 全員が回答するグループが0件の定義を使うテストを足す

## 段階4以降で行うこと

- 起動時の検証に、`alwaysShownGroups` が0件で `selectableGroups` が1件の定義をエラーにする規則を足す（段階4）
- `SaveRateLimiter.java:50` の文から「スキルシート」を外す（段階4）
- V1 を作り直すときに、ほかの文字列の列（`user_name` の `varchar(255)` など）の型も見直す（段階4）
- PDF のファイル名の指定（`Content-Disposition`）を、Lambda（`aws-lambda/src/index.ts:58`）からバックエンドのダウンロード用のエンドポイントに移し、`filename="skillsheet.pdf"` を汎用の名前にする（段階5）
- 描画役を作り直すときに、`page.pdf()` の `printBackground: true` を外す（段階5）
- 開発環境の `docker-compose.yml` に PDF 用のコンテナを足す（段階5）
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
  - フロントが定義を API から読むようになると、バックエンドなしでは画面を組み立てられなくなる。その時点で、`isBackendEnabled()` の整理とあわせて扱いを決める
  - `isBackendEnabled()` は、PDF ボタンの表示（`ShareButton.vue`）や `api.ts` でも使っている
  - 削除の対象：`shareUtils.ts` の圧縮・展開、`lz-string`、`ResultPage.vue` と `router/index.ts` の `data=`、関連するテスト、e2e の `share.spec.ts` と `e2e/pages/ResultPage.ts` の `data=` の扱い
- Spring Boot 版と Go 版で検証の規則を揃える方法
  - 案：正しい定義と、間違いを1つずつ入れた定義を JSON で用意し、期待するエラーと一緒にリポジトリに置く
  - Spring Boot と Go の両方のテストで同じファイルを読み、スキーマのファイルも共用する
  - 規則を1つずつすべて試す（`alwaysShownGroups` 0件・`selectableGroups` 1件の形も入れる）
  - 期待するエラーは順序によらずに比べる
  - 置き場所は作るときに決める（`SURVEY_DIR` で切り替えて読む案）
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
