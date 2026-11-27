# 引き継ぎ

最終更新：2026-11-27

## 現在地

- 段階2（設問の定義の形と、JSON Schema の作成）は完了し、`main` にマージ済み（#3）
  - スキーマは `schema/`、スキルシートのサンプルは `surveys/skill-sheet/` にある
  - VS Code でのスキーマのキーワードの効き方も確認済み
- 段階3と4の、API の形とデータの持ち方に関わる検討は、ひととおり終わった
- 役割分担の方針（フロントは表示と状態の管理だけ。`ROADMAP.md` の「役割分担」）が決まった
- 定義の API（`GET /api/survey`）を、Spring Boot 版で作った（作業ブランチ、未コミット）
  - 起動時の読み込み・検証・番号・版・表示設定の既定値と、間違いがあれば起動を止める処理
  - 処理は `service/definition/` の普通のクラス、Spring とつなぐのは `DefinitionService`、エラーの表示は `StartupFailureAnalyzer`
  - `schema/` は `pom.xml` のリソースでクラスパスに取り込み、開発の compose は `schema/`・`surveys/` をマウントする
  - 本番の `Dockerfile` のビルドの `context` は、リポジトリ直下（`deploy.yml` も同じ）
  - テストは `./mvnw test` で通ることを確認済み
- バックエンドのパッケージは `com.surveysheet` に改名した
- 決定済み（`DECISIONS.md`）
  - 定義の API の形（パス・`selectable` の印・`limits`・値が無い項目は `null`。アイコンの配信を除く）。表示設定は既定値を当てて返す
  - シートの API と DB の形（組み立て・URL 共有・PDF 生成・CSV・読み出し、冪等キー、エラーの見分け方、回数制限）
  - 自分の結果ページの流れ（組み立ての API で表示し、URL 共有・PDF・CSV を押したときに保存する）
  - PDF の構成。描画役は Gotenberg（Lambda は仮決定）。生成は PDF のボタンを押したときに始める
  - フロントの読み込みと状態（読み込みの場所、失敗したときの表示、定義の API の応答の置き場所、回答の状態の形、版の確認）
  - CSV はバックエンドで作る。星の表示と凡例はフロントで作る
  - 名前の上限は管理者が設定する。管理者に渡す設定ファイルは `config.json`
  - 定義ファイルの検証とエラーの一覧の形
  - 共用するテスト用の定義の形
- コードの名前の見直しが終わった（「実装のときに変える名前」）
- フロントと、シートの API は、移行前のスキルシート作成アプリのまま
- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）
- Dependabot は alerts と security updates を有効にしている。version updates は GitHub Actions だけ（`dependabot.yml`）

## 進め方

- 改修のコードは、すべて終わってから `main` に取り込む。途中の区切りで CI を通す必要はない
- API の仕様を先に確定させる。手戻りが少なければ、作業の順番は問わない

## 完成形のイメージ（段階4を終えた時点）

- バックエンドは起動時に `SURVEY_DIR` の `survey.json` と `display.json` を読んで検証し、番号を振り、版を計算し、表示設定に既定値を当てて、メモリに持つ（済み）
- 定義の API は、メモリに持った内容と、名前の上限を返すだけ（済み）
- フロントは `main.ts` で定義の API を読み、`utils/definition.ts` に入れる。版を確かめてから、ルーターを使い始めてマウントする
- 回答の状態は、選んだグループと、チェックした項目と評価と、冪等キーだけを持つ
- 回答ページは、設問のデータを並べ、各項目のチェックと評価をストアから読む（結合した形を作らない）。設問の部品（`QuestionSection.vue`）は、ストアを直接書き換える。評価が未選択の項目のエラーは、ストアの状態から出す
- 自分の結果ページは、組み立ての API の応答を表示する。名前を確定しても保存しない
- URL 共有・PDF・CSV を押すと、冪等キーつきで保存してから、それぞれの操作を行う。保存した時点で名前を固定する
- 共有ビューと PDF は、保存したシートをそのまま表示する
- CSV は、バックエンドが保存したシートから作る

## 次の作業の候補

順番は決めていない。

- シートの API（組み立て・URL 共有・PDF 生成・CSV・読み出し）と、DB の V1 の作り直し
- フロントを、定義の API から画面を組み立てる形にする（段階3）

## 実装のときに決めること

フロント。

- `index.html` の `<title>` と、読み込み中の表示（`<div id="app">` の中に置くか）
- PDF の描画役が待つ `data-pdf-ready` が、定義とシートの読み込みの後に付くかを確かめる
- PDF・CSV のダウンロードのしかた
  - 今は `ShareButton.vue` の `handlePdfDownload` で `window.open` している。ダウンロード用のエンドポイントが 410 を返すと、新しいタブにエラーの JSON が出る
- 版が変わったときの一文を出す画面
- 版が違う（409）を受けたときの戻し方。フロントがメモリに持つ定義は古いままなので、初期値に戻すだけでは次の操作も 409 になる。定義の読み直し方と、一文の出し方をあわせて決める
- 組み立てに失敗したときの、自分の結果ページの表示
- 保存した時点で名前を固定する処理（確定後の編集の猶予の途中でも固定にする）
- アンケートをやり直すリセットのボタン（`resetAll` と同じ処理を使う）
- URL 共有の API が失敗したときのボタンの表示（`ShareUrlButton.vue`。今は代わりにクエリ方式の URL をコピーしている）
- タブの間の状態の同期（入れるかどうかも含めて）

バックエンド。

- エンドポイントのパス（組み立て・URL 共有・PDF 生成・CSV と、PDF・CSV のダウンロード）
- `ProblemDetail` の `type` の値（URI の形と名前）
- 冪等キーの項目の名前と、シートのテーブルの列とテーブルの名前（今の `skill_sheets` は汎用の名前にする）
- 名前の検証を、定義の API と同じ上限（`DefinitionService` の値）で行う。今の `SaveSheetRequest` の `@Size(max = 100)` は使わない
- IP の取り方（解説を付けて決める）
  - 今の `resolveClientIp`（`SkillSheetController.java`）は、前段にプロキシが無いと、`X-Real-IP` のヘッダーで IP を偽れる
  - Spring Boot 版は `server.forward-headers-strategy` に任せる形がある
- 回数制限の上限と時間の幅の、既定値と環境変数の名前
- 期限の日数の、既定値と環境変数の名前（`application.properties` の、S3 のライフサイクルのルールと揃えるというコメントは無くなる）
- `config.json` に入れる設定の一覧と、`config.json` から環境変数へ渡す仕組み（段階8で決めてもよい）
- 共有 URL とダウンロードの URL を組み立てるための、公開の URL の設定（名前と、未設定のときの扱い）。PDF の描画役が開く URL（compose の中のコンテナの名前）との違いも、ここで扱う
- ready になる前に、PDF のダウンロードの URL を開かれたときの返し方
- 保存のリクエストの本文の大きさの上限（Spring Boot 版の今の値は未確認。Go 版は `http.MaxBytesReader` で掛ける）
- CSV のファイル名の指定（`Content-Disposition`。日本語のファイル名）と、BOM（今はフロントの `downloadCsv` で付けている）
- コンテナの時刻の設定（CSV のファイル名の日付のため）

## 実装のときに変える名前

名前の付け方は `CLAUDE.md` の「名前」に従う。処理ごと無くなるもの（「段階4以降で行うこと」）は含めない。

バックエンド（パッケージの `com.surveysheet` と、`SurveySheetApplication` は変更済み）。

- `SkillSheet`（エンティティ）→ `Sheet`、`SkillSheetRepository` → `SheetRepository`、`SkillSheetService` → `SheetService`、`SkillSheetController` → `SheetController`
  - シートの API を作り直すときに変える。コントローラーを分けるかは、エンドポイントを決めるときに決める

フロントのファイル。

| 今                                   | 変えた後                     |
| ------------------------------------ | ---------------------------- |
| `stores/useSurveyStore.ts`           | `stores/useAnswerStore.ts`（ストアの ID は `'answer'`） |
| `composables/useSurveyValidation.ts` | `composables/useValidation.ts` |
| `composables/useGuestGate.ts`        | `composables/useNameGate.ts` |
| `composables/useAppNavigation.ts`    | `composables/useNavigation.ts` |
| `components/AppButton.vue`           | `components/BaseButton.vue`  |
| `components/ShareButton.vue`         | `components/ShareMenu.vue`   |
| `components/MenuItemButton.vue`      | `components/IconMenuItem.vue` |
| `components/LevelLegend.vue`         | `components/ScaleGuide.vue`  |
| `components/ValidationError.vue`     | `components/ValidationAlert.vue` |
| `components/StatePanel.vue`          | `components/ErrorPanel.vue`  |
| `utils/constants.ts`                 | 無くす。`ROUTES` は `router/routes.ts` へ移し、`FORMULA_PREFIX` は削除する |

フロントのファイルの中の名前。

- ストア
  - TOP で選んだグループの一覧：`selectedGroupIds`
  - チェックした項目と評価：`checkedItems`
  - `setCategoryChecked` → `setGroupSelected(groupId, selected)`
  - `setAnswerSelection` → `setItemChecked(key, checked)` と `setItemValue(key, value)` に分ける（引数の形は実装のとき）
- `useNameGate`：`isGuest` → `hasName`（真偽を反転。`ShareMenu.vue` は `v-if` と `v-else` の順を入れ替える）、`goToNameInput` → `focusInput`、`UseGuestGateReturn` は無くす（戻り値から型を導かせる）
- `useNavigation`：`goToTop`・`goToSurvey`・`goToResult` → `visitTop`・`visitSurvey`・`visitResult`
- `useValidation`
  - `flaggedAnswerIdsOf` → `getInvalidItemIds(groupId, questionId)`
  - 中だけの名前：`flaggedAnswers` → `snapshot`、`IncompleteAnswer` → `IncompleteItem`、`isSameAnswer` → `isSameItem`
- 型：`ValidationErrorItem` → `ValidationError`（`categoryLabel` → `groupLabel`）、`PdfGenerationState` → `PdfState`（値の `'waiting'` → `'idle'`。ほかの値は今のまま）、`StarLevel` は無くす（評価の値は `number`）
- `TopPage.vue`：`selectableCategories` → `selectableGroups`、`isCategoryChecked` → `isSelected`、`toggleCategory` → `toggle`
- `AnswerItem.vue`
  - props：`isChecked` → `checked`、`isFlagged` → `invalid`（評価が未選択かの判断も済ませて渡す）、`answerId` は無くす
  - イベント：`update:answer` → `update:checked`・`update:value`
  - `handleCheckChange`・`handleLevelChange` は無くし、テンプレートに `$event` で書く
- `QuestionSection.vue`：props の `flaggedAnswerIds` → `invalidItemIds`。イベントを中継せず、ストアの `setItemChecked`・`setItemValue` を呼ぶ（`SurveyPage.vue` の `handleAnswerUpdate` は無くなる）
- `v-for="category in …"` → `v-for="group in …"`

e2e のロケーター（`data-slot`）。

- `category-cards`・`category-card`・`category-card-title`・`category-card-description` → `group-cards`・`group-card`・`group-card-title`・`group-card-description`
- `skill-card`・`skill-info` → `item-card`・`item-info`
- `state-panel`・`state-panel-title` → `error-panel`・`error-panel-title`

変えないもの。

- `SurveyPage.vue`、`NotFound.vue`、`AnswerItem.vue`、`PdfButton.vue`・`CsvButton.vue`・`ShareUrlButton.vue`、`shareUtils.ts`、`WebConfig`
- `useNameGate` の `guard`、`BaseButton.vue` の `inactive`、`AnswerItem.vue` の `v-for` の `level`、`useValidation` の `collectIncomplete`

## 実装のときに確かめること

- `ddl-auto=validate` で、`String` の項目と `text` の列の照合が通るか
- Spring Boot 版で、jsonb の列をエンティティにどう写すか（Hibernate の JSON 型の対応で足りるか）
- 描画役を待つ時間の上限（HTTP クライアントと SDK の読み取りのタイムアウト）を、描画役の時間の上限（Gotenberg の `API_TIMEOUT`、既定30秒。Lambda のタイムアウト）より長くする
- PDF の失敗の記録を消す時期（再試行のときと、残った記録の定期削除）
- フロントの60秒の打ち切りは、`/status` が failed を返すようになっても、受け皿として残す
- PDF のサイズ（DB の容量と、Lambda の同期の応答の上限 6MB に対して）
- Gotenberg（v9.6.0 のソースで確認した内容）
  - 今の `aws-lambda/src/index.ts` の処理との対応
    - 許可するオリジン：`--chromium-allow-list`（正規表現）
    - ライトモード：`emulatedMediaFeatures` に `{"prefers-color-scheme":"light"}`（JSON のオブジェクト）
    - 待ち合わせとエラーの判定：`waitForExpression`。式は Promise の結果を待ち、例外なら 400、時間切れなら 503 になる
    - A4・余白：`paperSize=A4`、`marginTop=15mm` など
    - 背景：`printBackground` の既定値が false
  - 待ち合わせの式の案

    ```js
    (async () => {
      if (document.querySelector('[data-pdf-error]'))
        throw new Error('pdf-error');
      if (!document.querySelector('[data-pdf-ready]')) return false;
      await document.fonts.ready;
      return true;
    })();
    ```

  - 許可の一覧は、ページが読み込むすべてのリクエストに掛かる。Google Fonts（`fonts.googleapis.com`・`fonts.gstatic.com`）も入れないと、フォントが止められ、イメージの `fonts-noto-cjk` で描かれる
  - ライトモードの指定は、ページの読み込みの後に行われる（Puppeteer は読み込みの前）。`index.html` のテーマのスクリプトと `useColorMode`（初期値 `auto`）で、ライトで描かれるかを確かめる
  - `--chromium-max-concurrency`（既定6、上限6）と `--chromium-max-queue-size`（既定0。あふれると 429）を、バックエンドの上限とどう組み合わせるか
- Gotenberg の Lambda 向けイメージ（`build/Dockerfile` の `gotenberg-aws-lambda-chromium`）
  - AWS Lambda Web Adapter を使い、応答はまとめて返す方式（`AWS_LWA_INVOKE_MODE=buffered`）
  - バックエンドからの呼び出し方（関数 URL に IAM の署名を付けて HTTP で送るか、SDK の Invoke に HTTP の形のイベントを渡すか）
  - Lambda のイメージは ECR に置く必要がある。`deploy.yml` の「Build and push Lambda image」を、Gotenberg のイメージを取得して ECR に push する形に変える
  - Docker Hub での Lambda 向けイメージのタグ名

## 段階3で行うこと

- 固定の文言を `src/data/texts.ts` に移し、`composables/useTexts.ts` を作る（表示設定の値の差し込みだけを行う。既定値はバックエンドが当てる）
  - `constants.ts` の `LEVEL_LABELS` は無くす。星の表示と凡例は、評価の数値と `scale.levels` からフロントで作る
  - 名前の入力欄の上限とラベルの数は、定義の API の `limits.userNameMaxLength` を使う（今は `EditableNameHeading.vue` の定数 `NAME_MAX_LENGTH`）
- 文面の変更と、直すテスト
  - 「お名前」→「名前」：`EditableNameHeading.vue:23, 119`、`useGuestGate.ts:6`
    - テスト：`EditableNameHeading.test.ts:12, 14`、e2e の `share.spec.ts:175`、`pages/ResultPage.ts:20`
  - 共有ビューのボタン（`ResultPage.vue:254`）
    - テスト：`App.test.ts:107`、`ResultPage.test.ts:213, 218, 277, 284`
  - リンク切れの文（`ResultPage.vue:124`）：テストの修正は不要
- TOP（`TopPage.vue`）
  - グループの `description` が `null` のときは、説明の `<p>` と `aria-describedby` を出さない
  - `selectableGroups` が0件なら選ぶ欄を出さない
  - 選ぶ欄の見出しと補足を、件数で切り替える（見出しの既定値はバックエンドが決める）
  - 全員が回答するグループが0件のときの開始ボタン（ツールチップと、押したときのフォーカス）
    - カードの枠は `has-[:focus-visible]` のため、プログラムからのフォーカスで出ない可能性がある。実機で確かめ、出なければカードを短く強調するなどで補う
  - `top.banner` の看板。本文は `text-sm`（14px）にする案
- 回答ページ（`/survey`）：表示するグループが無いときは TOP へ移すガードを足す（`router/index.ts`）
- グループの並び順を、全画面・CSV・PDF で揃える
- 全員が回答するグループが0件の定義を使うテストを足す

## 段階4以降で行うこと

- 回答の状態を、選んだものだけを持つ形に変える（段階3・4）
  - `src/data/questions/`、`buildInitialSelections`・`buildQuestionsForCategory`・`normalizeSurveyState`（`utils/surveyState.ts`）を削除する
  - `sheetMapper.ts` の `toSheetDto` を、新しい形から組み立てる
- CSV をバックエンドに移す（段階4）
  - バックエンドに、CSV の API とダウンロード用のエンドポイントを足す
  - フロントの `csvUtils.ts` とそのテストを削除し、`CsvButton.vue` を、CSV の API が返す URL を開く形に変える
- 回数制限を、新しいシートの行を作ったときだけ数える形に変える（段階4）
  - `SaveRateLimiter.java:50` の文から「スキルシート」を外す
  - `SaveRateLimiter.java` の Javadoc の「PdfController/LambdaPdfServiceの再試行スロットリングと同じ設計方針に揃えている」を直す
- V1 を作り直すときに、シートのテーブルを `DECISIONS.md` の「DB とデータ」の形にし、`sheet_categories`・`sheet_answers`・`share_token` を無くす（段階4）
- 方式の変更で影響するもの（段階4）
  - バックエンド：エンティティ（`SheetCategory`・`SheetAnswer` は無くなる）、リポジトリ、`dto/`、`SkillSheetService`、テスト（`SkillSheetServiceTest`・`SkillSheetServiceIntegrationTest`・`SkillSheetControllerTest`）
  - バックエンドの PDF：`PdfController.regenerate`、`LambdaPdfService.onSheetSaved` と `SheetSavedEvent`（保存のコミット後の依頼）は無くなる。`PdfStatusResponse` から `downloadUrl` を外す
  - フロント：`useMergedSurvey.ts` は無くなる。`sheetMapper.ts`（`toSheetDto`・`toSurveyState`）、`ResultPage.vue`（`handleNameCommitted` の保存と、共有ビューの経路）、それぞれのテスト
  - フロントのストアと共有メニュー：`useSurveyStore.ts` の `savedDataSnapshot`・`isIdVerified`・`getSavedIdOrSave`、`ShareButton.vue` の `openMenu`・`preparePdf`・`handlePdfRetry`、`usePdfStatus.ts` の `refreshDownloadUrl`・`requestRegeneration`（`request` と `retry` は PDF 生成の API を呼ぶ1つの操作にまとめる）
  - e2e：共有ビューのモック（`e2e/mocks.ts`）の応答の形
- 名前の先頭の文字の確認をやめる（段階4）
  - `useNameCommit.ts` の `errorMessage`・`INVALID_PREFIX_MESSAGE`、`EditableNameHeading.vue` の名前のエラーの表示、`constants.ts` の `FORMULA_PREFIX`
  - テスト：e2e の `basicFlow.spec.ts`・`pages/ResultPage.ts`
- クエリ方式の共有を、フロントの作業のときに削除する（段階4）
  - 削除の対象：`shareUtils.ts` の圧縮・展開、`lz-string`、`ResultPage.vue` と `router/index.ts` の `data=`、関連するテスト、e2e の `share.spec.ts` と `e2e/pages/ResultPage.ts` の `data=` の扱い
  - あわせて `isBackendEnabled()` を無くす（PDF ボタンの表示（`ShareButton.vue`）や `api.ts` でも使っている）
- PDF のファイル名の指定（`Content-Disposition`）を、Lambda（`aws-lambda/src/index.ts:58`）からバックエンドのダウンロード用のエンドポイントに移し、`filename="skillsheet.pdf"` を汎用の名前にする（段階5）
- 開発環境の `docker-compose.yml` に Gotenberg のコンテナを足す（段階5）
- Lambda を Gotenberg にそろえたら、`aws-lambda/` と `test.yml` の `lambda-build-check` を削除する（段階5）
- `aws-lambda/` を使い続ける場合は、`page.pdf()` の `printBackground: true` を外す（段階5）
- 本番での `surveys/` の渡し方を、デプロイ先に合わせて決める（段階6）
  - 今の本番のイメージには `surveys/` が入っていないので、そのままでは起動しない（`SURVEY_DIR` が無いと起動を止める）
  - 候補：イメージに入れる、実行するときにマウントする、両方
- `deploy.yml` の自動実行を戻すときに、`on:` と、`github.event.workflow_run` を使う `if:` の条件を作り直す（段階6。今の `on:` は `workflow_dispatch` だけなので、手動で実行しても `flags` のジョブが飛ばされ、何もデプロイされない）
- `TheFooter.vue` の GitHub のリンク先（旧リポジトリ `FullStack-CreateSkillSheet`）を差し替える（段階8）

## 未着手の整理

- `README.md` を、利用者向けの導入手順書に書き換える（時期は未定）
  - スキーマの解説もここに書く（`additionalProperties` で書き間違いを検出していること、グループの合計10件・項目の総数500件の上限など）

## 未決事項

- 段階7で Go のスキーマのライブラリを選ぶときに確かめること（Java の networknt 3.x では、次のとおり確認済み）
  - 正規表現の `\p{Cc}`・`\p{Zs}` が使えるか（Java：使える。Ajv・VS Code・Go の標準の正規表現も確認済み）
  - `maxLength` を Unicode の文字数（コードポイントの数）で数えるか（Java：コードポイントで数える）
  - キーの重複のエラーに、行番号などの位置の情報が付くか（Java：Jackson の例外から行・列・キーの並びが取れる。Go は v2 のエラーで確かめる）
  - JSON として読み込めなかったときのエラーの文に、ファイルの中身が含まれるか（Java：例外の文は使わず、行と列だけから文を作る。Jackson 3 は既定で中身を伏せる）
- 共用するテスト用の定義（`testdata/`）を作るときに決めること
  - 期待するエラーの書き方、正常系の期待する結果に含めるもの、境界のケースの範囲
  - 案：スキーマのファイルも共用する。規則を1つずつすべて試す（`alwaysShownGroups` 0件・`selectableGroups` 1件の形も入れる）
  - 両方の間違いがある定義では、期待するエラーはスキーマの分だけになる（コードの規則は、スキーマの違反が0件のときだけ確かめるため）
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
- `vue-frontend` の TypeScript 6 への移行
  - 上げる場合は `typescript@~6.0.3` と `@vue/tsconfig@^0.9.1` を揃えて上げる
  - typescript-eslint の対応範囲が `<6.1.0` のため、`^` ではなく `~` にする
  - npm の `latest` は 7 系なので、バージョンは明示して入れる
- 英語版で、エラーの文の件数（「{n}件あります」）は、数によって名詞の形が変わるので、文を組み立てる処理の形を変える必要がある（配布の段階）

## 依存関係

- `vue-frontend` には moderate の脆弱性が25件残っている。上流の更新を待つ
  - `minimatch`（<11.0.1）と `glob`（<14.0.2）。どちらも開発用の依存（ESLint 9 系、prettier のプラグイン、`@vue/test-utils`）
- `aws-lambda` の脆弱性は0件
- `@vitest/eslint-plugin` はロックファイル上 1.6.27
  - 1.6.28 は TypeScript 6 以上が前提のため、引数なしの `npm update` は衝突で止まる
  - 更新は、対象のパッケージを指定して行う

## 環境の注意

- macOS では、`npm ci` のたびに fsevents の install script がブロックされた警告が出る。動作には影響しないため、`allowScripts` には追加していない
- Apple Silicon の Mac で、PDF 生成の Lambda イメージ（Puppeteer）がローカルでビルドできるかは未確認（本番用のイメージは CI の amd64 でビルドしている）
  - `eclipse-temurin:25-jdk-alpine`・`25-jre-alpine` は、arm64 で動くことを確認済み
