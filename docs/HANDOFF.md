# 引き継ぎ

最終更新：2026-11-17

## 現在地

- 段階2（設問の定義の形と、JSON Schema の作成）は完了し、`main` にマージ済み（#3）
  - スキーマは `schema/`、スキルシートのサンプルは `surveys/skill-sheet/` にある
  - VS Code でのスキーマのキーワードの効き方も確認済み
- 段階3と4をまとめて検討している。実装は、バックエンドの定義の API を先に作る
- 定義の API の形は、アイコンと印のキーの名前を除いて決定済み（`DECISIONS.md` の「定義の API」）
- シートの API と DB の形は決定済み（`DECISIONS.md` の「シートの API」「DB とデータ」）
  - シートは、回答したときの文言つきの回答を jsonb で1行に持つ
- PDF は、二重の生成の防止・同時に頼む数の上限・`/status` での確認まで決定済み。描画役は Gotenberg（Lambda は仮決定）（`DECISIONS.md` の「PDF 生成」）
- アプリの中身は、移行前のスキルシート作成アプリのまま
- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）
- Dependabot は alerts と security updates を有効にしている。version updates は GitHub Actions だけ（`dependabot.yml`）

## 完成形のイメージ（段階4を終えた時点）

- バックエンドは起動時に `SURVEY_DIR` の `survey.json` と `display.json` を読んで検証し、番号を振って版を計算し、メモリに持つ
- 定義の API は、メモリに持った内容を返すだけ
- シートの保存は、フロントが番号・値・版を送り、バックエンドがメモリの定義から文言を引いて、文言つきの回答を組み立てて保存する
- シートの読み出しは、保存した文言つきの回答を返す。共有ビューと PDF は、定義と結合せずにそのまま表示する
- フロントは画面を開いたときに定義の API を読み、定義から画面を組み立てる。回答者自身の結果での定義と回答の結合（`useMergedSurvey.ts`）と、CSV の作成はフロントで行う

## 次の作業：段階3・4の検討の続き

1つずつ、上から順に決める。

1. フロントの読み込みと状態
   - 読み込みを待つ場所（`main.ts` で待ってから `app.mount()` するか、マウントしてから `App.vue` で読み込み中を出すか）
   - 読み込みに失敗したときの表示
   - ストアの初期化（今はストアを作るときに `buildInitialSelections()` で組み立て、`pinia-plugin-persistedstate` でブラウザに保存している。定義が届いてから組み立てる形と、保存された状態の版の確認をどこに置くか）
   - アプリの中の型（API の形をそのまま使うか、今の `CategoryMaster`・`Merged*` に変換するか）
     - 結果ページの入力が2つになる（自分の結果は今の定義と結合したもの、共有ビューは文言つきの回答）。同じ表示の型に揃える形を決める
     - 共有ビューでの `normalizeSurveyState`（`ResultPage.vue:55`）は不要になる
     - 結果ページのテンプレートの `v-for` のキーは、グループが `category.id`、設問が `question.id`、項目が `answer.label`
   - `index.html` の `<title>`：読み込むまでの間に出す文字列
   - PDF
     - 描画役は結果ページの `data-pdf-ready` を待って印刷する。共有ビューも表示設定のために定義の API を読むので、定義の読み込みより後に付くかを確かめる
     - ダウンロードのしかた：今は `ShareButton.vue` の `handlePdfDownload` で `window.open` している。ダウンロード用のエンドポイントが 410 を返すと、新しいタブにエラーの JSON が出る
     - ページを開いたまま期限が切れると、`/status` と `regenerate` がエラーになり、再読み込みするまで保存し直せない（`handlePdfRetry` は、保存に失敗したときしか保存し直さない）
2. 定義の API の残り
   - 全員が回答するグループかの印のキーの名前
   - アイコンの渡し方と配信（Lucide の SVG をどこから配信するか、API が返すのは名前か URL か）
3. バックエンドの内部
   - Spring Boot 版で `schema/` を取り込む方法（`spring-backend/` の外にある。Maven のリソースにできるか、Docker のビルドの `context`（`./spring-backend`）から見えるかは未確認）
   - 開発環境で `surveys/` を読ませる方法（`docker-compose.yml` のバックエンドは `./spring-backend` だけをマウントしている。`surveys/` のマウントと `SURVEY_DIR` の設定が要る）
   - Java の検証ライブラリ（未決事項の「スキーマのライブラリを選ぶとき」）

次のものは、フロントの作りに影響しないため、段階4の実装のときに決める。

- エンドポイントのパス
- エラーの応答の形（今は `ProblemDetail`）。保存で版が違うときと、定義との照合で断るときの応答も、ここで決める
- `shareToken` の列の扱い
- 保存のリクエストの本文の大きさの上限（Spring Boot 版の今の値は未確認。Go 版は `http.MaxBytesReader` で掛ける）

実装のときに確かめること。

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
      if (document.querySelector('[data-pdf-error]')) throw new Error('pdf-error')
      if (!document.querySelector('[data-pdf-ready]')) return false
      await document.fonts.ready
      return true
    })()
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
- V1 を作り直すときに、シートのテーブルを文言つきの回答（jsonb）の形にし、`sheet_categories`・`sheet_answers` を無くす。ほかの文字列の列（`user_name` の `varchar(255)` など）の型も見直す（段階4）
- 方式の変更で影響するもの（段階4）
  - バックエンド：エンティティ（`SheetCategory`・`SheetAnswer` は無くなる）、リポジトリ、`dto/`、`SkillSheetService`、テスト（`SkillSheetServiceTest`・`SkillSheetServiceIntegrationTest`・`SkillSheetControllerTest`）
  - フロント：`sheetMapper.ts`（`toSheetDto`・`toSurveyState`）、`ResultPage.vue` の共有ビューの経路、それぞれのテスト
  - e2e：共有ビューのモック（`e2e/mocks.ts`）の応答の形
- 二重の生成の防止を状態での判定に変えるときに、次のコメントを直す（段階4）
  - `usePdfStatus.ts` の `requestRegeneration` の「バックエンドが直近15秒のInvokeをスロットリングするため」
  - `SaveRateLimiter.java` の Javadoc の「PdfController/LambdaPdfServiceの再試行スロットリングと同じ設計方針に揃えている」
- PDF のファイル名の指定（`Content-Disposition`）を、Lambda（`aws-lambda/src/index.ts:58`）からバックエンドのダウンロード用のエンドポイントに移し、`filename="skillsheet.pdf"` を汎用の名前にする（段階5）
- 開発環境の `docker-compose.yml` に Gotenberg のコンテナを足す（段階5）
- Lambda を Gotenberg にそろえたら、`aws-lambda/` と `test.yml` の `lambda-build-check` を削除する（段階5）
- `aws-lambda/` を使い続ける場合は、`page.pdf()` の `printBackground: true` を外す（段階5）
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
