# 引き継ぎ

最終更新：2026-11-02

## 現在地

段階1（新しいリポジトリの作成）が完了し、段階2に入る前。

- `deploy.yml` は手動実行のみ。本番のデプロイ先は未設定。
- `main` はブランチ保護済み（PR 必須、必須チェックは `ci-result`）。
- アプリの中身は、移行前のスキルシート作成アプリのまま。
- Dependabot は alerts と security updates を有効にしている。\
  version updates は GitHub Actions だけ（`dependabot.yml`）。

## 次の作業

段階2：設問定義の形とコンテンツの範囲を決め、JSON Schema を作る。

- 対象：`vue-frontend/src/data/questions/` のマスタと、\
  画面に直接書かれている文言（アプリ名、尺度の説明、CSV の見出しなど）
- 2つ目のサンプル定義のテーマを決める

## 未着手の整理

- `DESIGN_NOTES.md` を削除する。\
  `README.md` の「設計ノート」節がこのファイルを参照しているので、同時に直す。
- `README.md` を、利用者向けの導入手順書に書き換える（時期は未定）。

## 未決事項

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
