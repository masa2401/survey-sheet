# 引き継ぎ

最終更新：2026-11-04

## 現在地

段階2（設問の定義の形と、JSON Schema の作成）を検討中。\
設定ファイルの分け方・形式・カテゴリの構造・アイコン・`id` の扱い・回答者の名前・文言の既定値は\
決定済み（`docs/DECISIONS.md`）。

- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定。
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）。
- アプリの中身は、移行前のスキルシート作成アプリのまま。
- Dependabot は alerts と security updates を有効にしている。\
  version updates は GitHub Actions だけ（`dependabot.yml`）。

## 次の作業

段階2で、まだ決まっていないことを決め、JSON Schema とサンプルを作る。

1. JSON Schema で表せない検証の規則と、表示設定（`display.json`）をどこで検証するか\
   （バックエンドは中身を解釈しない）。規則の候補は次のとおり。
   - カテゴリの `id` の重複（2つの配列をまたいで確認する）
   - 各階層の最低件数、文字数の上限
   - 名前のテンプレートに `{name}` がちょうど1つ含まれること\
     （`EditableNameHeading.vue` は、入力欄の前後に文言を分けて置くため）
2. カテゴリの件数による TOP の挙動\
   （案：選択式が0件なら選択欄を出さない。共通が0件なら1つ以上選ばないと開始できない。両方0件は定義のエラー）
3. 回答ページでのカテゴリの並び順（案：共通を先、選択式を後に、それぞれ定義の順）
4. 2つ目のサンプル定義のテーマ（尺度の意味やカテゴリの数がスキルシートと違うもの）
5. 画面の言語：固定の文言は日本語のままにし、多言語対応は対象外とするか
6. リポジトリ内の置き場所（設問の定義・表示設定・JSON Schema）
7. 表示設定で入れ替える名詞の一覧と、汎用的な既定値の文言\
   （JSON Schema を作るときに、固定の文言を洗い出して決める）

固定の文言のうち、スキルシート固有の言葉を含むものの例：\
`AnswerItem.vue`（習熟度）、`csvUtils.ts`（CSV の見出し・ファイル名）、\
`ResultPage.vue`（見出し・ボタン・エラー文）、`TopPage.vue`（見出し）、\
`EditableNameHeading.vue`（ラベル・見出し）、`TheHeader.vue`（アプリ名）

## 未着手の整理

- `DESIGN_NOTES.md` を削除する。\
  `README.md` の「設計ノート」節がこのファイルを参照しているので、同時に直す。
- `README.md` を、利用者向けの導入手順書に書き換える（時期は未定）。

## 未決事項

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
- ライセンス（開発が進んでから決める）
- レンタルサーバーで動く PHP 版を作るか
- 版のハッシュの計算方法（段階4で決める）。\
  候補は、定義ファイルのバイト列から計算する案と、JSON を整えてから計算する案。\
  後者は、ライブラリの更新などで書き出し方が変わるとハッシュも変わる。
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
