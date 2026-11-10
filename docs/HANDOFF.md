# 引き継ぎ

最終更新：2026-11-10

## 現在地

段階2（設問の定義の形と、JSON Schema の作成）の検討は完了し、スキーマとサンプルに反映済み。\
残りは、VS Code でスキーマのキーワードが効くかの確認だけ。\
最近決めたもの：固定の文言と表示設定の項目（`docs/DECISIONS.md` の 2026-11-09 の節）。\
`terms`・`respondent.nameLabel`・`top.hint` を削除し、`csv.itemHeading` と `top.banner` を足した。\
スキーマは `schema/`、スキルシートのサンプルは `surveys/skill-sheet/` に置いている。

- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定。
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）。
- アプリの中身は、移行前のスキルシート作成アプリのまま。
- Dependabot は alerts と security updates を有効にしている。\
  version updates は GitHub Actions だけ（`dependabot.yml`）。

## 次の作業

1. VS Code でスキーマのキーワードが効くかを確かめる。\
   確かめること：`$ref` と並べたキーワード（`maxLength`）、`additionalProperties`、`uniqueItems`、\
   `propertyNames`、`maxLength` の数え方（`𠮷` や絵文字を1文字と数えるか）、配列の `minItems`・`maxItems`（`top.banner`）。\
   Ajv（Draft 2020-12、strict モード）では、サンプルが通ることと、削除した項目・看板の上限の違反がエラーになることを確認済み。
2. 段階3に進む。

## 段階3で行うこと（決定済み）

- 固定の文言を `src/data/texts.ts`（文面の一覧）に移し、`composables/useTexts.ts` で表示設定の差し込みと既定値の適用を行う。\
  対象は画面に出る文字と `aria-label`（ログと画面に出ない `Error` の文は除く）。\
  `constants.ts` の `LEVEL_LABELS` の説明は `scale.levels` に置き換わる（星の文字列は残る）。\
  `csvUtils.ts` は、完成したファイル名・見出しを引数で受け取る形にする（`csvUtils.test.ts` の呼び出しの修正が要る）。
- 文面の変更とテストへの影響（文面は DECISIONS の 2026-11-09 の節）
  - 「お名前」→「名前」：`EditableNameHeading.vue:23, 119`、`useGuestGate.ts:6`。\
    テスト：`EditableNameHeading.test.ts:12, 14`、e2e の `share.spec.ts:175`、`pages/ResultPage.ts:20`（ロケーター）。
  - 共有ビューのボタン「自分もアンケートに答える」（`ResultPage.vue:254`）。\
    テスト：`App.test.ts:107`、`ResultPage.test.ts:213, 218, 277, 284`。e2e への影響は無い。
  - リンク切れの文（`ResultPage.vue:124`）、CSV の1行目「名前」（`csvUtils.ts:11`）。テストの修正は不要。
  - スキルシートのサンプルは結果の見出し・ファイル名の文面が変わらないので、`様のスキルシート` を見ているテストはそのまま。
- `TopPage.vue`：グループの `description` が無いときは、説明の `<p>` と `aria-describedby` を出さない。\
  `selectableGroups` の `description` を任意にしたため（今は常に出している）。
- TOP：`selectableGroups` が0件なら選ぶ欄を出さない。選ぶ欄の見出しの既定値と補足は、件数で切り替える。
- TOP：`alwaysShownGroups` が0件なら、1つ以上選ぶまで開始ボタンを `aria-disabled` にする（`AnimatedIconButton.vue` の `inactive`）。\
  `ShareButton.vue` の名前が未入力のときと同じく、ツールチップで理由を示し、押したら1枚目のカードへフォーカスを移す。\
  カードのフォーカスの枠は `has-[:focus-visible]` なので、マウスやタップの後にプログラムからフォーカスすると出ない可能性がある。\
  実機で確かめ、出なければ押したときにカードを短く強調するなどで補う。
- TOP：`top.banner` を書いたときに看板を出す。看板か選ぶ欄の、どちらかは必ず表示する\
  （看板を書かなかったときの扱いは、実装のときに決める）。\
  本文を `text-sm`（14px）にすると、上限いっぱいでも iPhone SE でカードの先頭が最初の画面に入る（測定値）。
- `index.html` の `<title>`：表示設定を読み込むまでの間に出す文字列を決める。
- 全員が回答するグループが0件の定義を使うテストを足す（開始ボタンの無効とツールチップ、補足を出さないこと）。
- 回答ページ（`/survey`）：表示するグループが無いときは TOP へ移すガードを足す（`router/index.ts`）。
- グループの並び順：`alwaysShownGroups` → `selectableGroups`、それぞれ定義の順。全画面・CSV・PDF で揃える。
- フロントが定義をどう読むかを決める（バックエンドの API ができる前の段階）。\
  `surveys/` を読む場合は、コンテナへのマウント（今の `docker-compose.yml` は `./vue-frontend` だけ）と、\
  Vite の設定（`vue-frontend/` の外のファイルを読めるか）を確かめる。

## 段階4以降で行うこと（決定済み）

- 起動時の検証に、`alwaysShownGroups` が0件で `selectableGroups` が1件の定義をエラーにする規則を足す（段階4）。\
  両方0件の規則と同じく、配列をまたぐのでコードで確かめる。共用するテスト用の定義にも、この形を入れる。
- `SaveRateLimiter.java:50` の文から「スキルシート」を外す（段階4で作り直すとき。今のフロントはこの本文を表示していない）。
- `aws-lambda/src/index.ts:58` の `filename="skillsheet.pdf"` を汎用の名前にする（段階5）。
- `TheFooter.vue` の GitHub のリンク先が旧リポジトリ（`FullStack-CreateSkillSheet`）のまま。配布時に差し替える（段階8）。

## 未着手の整理

- `DESIGN_NOTES.md` を削除する。\
  `README.md` の「設計ノート」節がこのファイルを参照しているので、同時に直す。
- `README.md` を、利用者向けの導入手順書に書き換える（時期は未定）。\
  スキーマの解説もここに書く（`additionalProperties` で書き間違いを検出していること、\
  配列をまたぐ上限（グループの合計10件、項目の総数500件）など）。

## 未決事項

- スキーマのライブラリを選ぶとき（段階4の Java、段階7の Go）に確かめること。
  - 正規表現の `\p{Cc}`・`\p{Zs}` が使えるか（Ajv・VS Code・Go の標準の正規表現では確認済み。Java は未確認）
  - `maxLength` を、仕様どおり Unicode の文字数（コードポイントの数）で数えるか
  - キーの重複のエラーに、行番号などの位置の情報が付くか（Jackson の `StreamReadException`、Go の v2 のエラー）
  - JSON として読み込めなかったときのエラーの文に、ファイルの中身が含まれるか\
    （含まれるなら、制御文字の置き換えの対象にする）
- クエリ方式の共有を削除した後、保存に失敗したときの共有ボタンの挙動。\
  今は代わりにクエリ方式の URL をコピーしている（`ShareUrlButton.vue`）。\
  案：ボタンにエラーを表示し、やり直せるようにする。
- クエリ方式の共有を削除する時期。\
  案：段階4（e2e をバックエンドありの形に変えるとき）で削除し、\
  `isBackendEnabled()` の整理は段階5で行う。\
  `isBackendEnabled()` は PDF ボタンの表示（`ShareButton.vue`）や `api.ts` でも使っており、\
  同一デプロイにすると「PDF が有効か」などの判定に変わるため。\
  削除の対象：`shareUtils.ts` の圧縮・展開、`lz-string`、`ResultPage.vue` と `router/index.ts` の `data=`、\
  関連するテスト、e2e の `share.spec.ts` と `e2e/pages/ResultPage.ts` の `data=` の扱い。
- Spring Boot 版から Go 版へ置き換えるときに、検証の規則が食い違わないようにする方法。\
  案：正しい定義と、間違いを1つずつ入れた定義を JSON で用意し、期待するエラーと一緒にリポジトリに置く。\
  Spring Boot と Go の両方のテストで同じファイルを読む。スキーマのファイルも共用する。\
  規則を1つずつすべて試す（スキーマのキーワードの書き間違いも、ここで検出する）。\
  エラーの表示順は保証しないので、期待するエラーは順序によらずに比べる。\
  置き場所は作るときに決める（`SURVEY_DIR` で切り替えて読めるようにする案）。
- 保存したシートを読み出すとき、グループ・設問・項目を定義の順で返す（段階4で作り直すとき）。\
  今は2か所で順序が保証されない。フロントは API の順序のまま並べる（`sheetMapper.ts` の `toSurveyState`）ので、\
  崩れると共有 URL で開いた結果ページだけ並びが変わる。
  - グループ：`SheetCategoryRepository.findWithAnswersBySheetId` のクエリに `ORDER BY` が無い。
  - 設問：`SkillSheetService.buildQuestions` が `Collectors.groupingBy`（内部は `HashMap`）でまとめている\
    （今は番号が小さい整数なので、たまたま番号順に並んでいる）。

  案：順序を保つ形（`ORDER BY`、`LinkedHashMap`、番号で並べ替える）にするか、\
  バックエンドが読み込んだ定義の順で並べ直して返す。
- 保存の DB との往復の削減（JDBC のバッチ、連番をまとめて取得する設定）。\
  項目の総数を500件までにしたので、今の設定でも問題にならない。必要になったら段階4で検討する。\
  Go 版では、最初から複数行をまとめて INSERT する書き方にする。
- CI でスキーマを Ajv の strict モードで読み込むか（CI を検討するときに決める）。\
  知らないキーワードがエラーになるので、スキーマの書き間違いに気づけるが、\
  共用するテスト用の定義が規則をすべて試していれば、そこで検出できるので、必要ない可能性が高い。\
  使う場合、strict モードは `patternErrorMessage` もエラーにするので、`ajv.addKeyword("patternErrorMessage")` で登録する。
- 2つ目のサンプル定義のテーマ（機能の改修が終わってから選ぶ）。\
  選ぶときの材料：スキルシート以外にも使えると示すこと、管理者が写して使う雛形になること。\
  `selectableGroups` が0件の構成にすると、看板の例も示せる。\
  前に挙げた候補：研修・イベントの満足度（参加したセッションをチェックする）、社内ツールの利用状況、店舗・サービスの評価。
- Go 版で `schema/` をどう取り込むか（段階7）。\
  `//go:embed` は `..` を含むパスを書けないので、モジュールを `go-backend/` などに置くと、直下の `schema/` を直接埋め込めない。
- 配布物の組み方（段階8）。設定ファイル一式の `schema/`（エディタ用の写し）と、イメージに埋め込んだスキーマが食い違わないよう、\
  リリースごとにイメージの版と設定ファイル一式を組にして配る案。
- ライセンス（開発が進んでから決める）
- レンタルサーバーで動く PHP 版を作るか
- 版のハッシュの計算方法（判断の材料が揃っていないので、段階4で実装するときに細かな仕様を決める）。\
  候補は、定義ファイルのバイト列から計算する案と、JSON を整えてから計算する案。\
  後者は、ライブラリの更新などで書き出し方が変わるとハッシュも変わる。\
  バイト列から計算する場合は、BOM を取り除く前と後のどちらで計算するかも決める\
  （後なら、BOM の有無で版は変わらない）。
- `vue-frontend` の TypeScript 6 への移行。\
  上げる場合は `typescript@~6.0.3` と `@vue/tsconfig@^0.9.1` をそろえて上げる。\
  typescript-eslint の対応範囲が `<6.1.0` のため `^` ではなく `~` にする。\
  npm の `latest` は 7 系なので、バージョンは明示して入れる。

## 依存関係

- `vue-frontend` には moderate の脆弱性が25件残っている。\
  `minimatch`（<11.0.1）と `glob`（<14.0.2）で、どちらも開発用の依存
  （ESLint 9 系、prettier のプラグイン、`@vue/test-utils`）。上流の更新を待つ。
- `aws-lambda` の脆弱性は0件。
- `@vitest/eslint-plugin` はロックファイル上 1.6.27。\
  1.6.28 は TypeScript 6 以上が前提のため、引数なしの `npm update` は衝突で止まる。\
  更新は対象のパッケージを指定して行う。

## 環境の注意

- macOS では、`npm ci` のたびに fsevents の install script がブロックされた警告が出る。\
  動作には影響しないため、`allowScripts` には追加していない。
- Apple Silicon の Mac で、次の点は未確認。
  - `eclipse-temurin:25-jre-alpine` が arm64 で動くか
  - PDF 生成の Lambda イメージ（Puppeteer）がローカルでビルドできるか\
    （本番用のイメージは CI の amd64 でビルドしている）
