# survey-sheet

設問定義ファイルを差し替えて使う、汎用のアンケートアプリ。\
回答者は項目をチェックして5段階で評価し、結果をシートとして確認・共有する。

スキルシート作成アプリからの移行中。\
方針と段階は `docs/ROADMAP.md`、現在地は `docs/HANDOFF.md` を参照する。

## 構成

| ディレクトリ | 内容 |
| --- | --- |
| `vue-frontend/` | Vue 3、TypeScript、shadcn-vue（Reka UI）、Tailwind v4、Pinia、Vite |
| `spring-backend/` | Spring Boot（Java 25）、PostgreSQL、Flyway |
| `aws-lambda/` | PDF 生成（Puppeteer、コンテナイメージ） |
| `docs/` | 方針・決定事項・引き継ぎ |

## コマンド

```bash
# vue-frontend/
npm run dev
npm run lint:check
npm run type-check
npm run test:unit:run
npm run test:e2e        # API はモックで代替している（e2e/mocks.ts）

# spring-backend/
./mvnw test             # Testcontainers を使うため Docker が必要

# リポジトリ直下
docker compose up       # DB・バックエンド・フロントをまとめて起動
```

Node と Java のバージョンは `.nvmrc` と `.java-version` に従う。

## 作業の進め方

- 説明と文書は日本語で書く。
- 変更は差分で提案する。適用とコミットは、依頼されたときだけ行う。
- 説明にはファイル名を添える。大きい変更は、解説を差分より先に置く。
- テストや e2e のロケーターに影響する変更は、着手前に影響範囲を示す。
- 確認が必要な事項は、一度にまとめて聞く。
- 事実が不明な点は推測で補わず、確認する。
- インフラやシェルのコマンドは、動作を確かめた形で提示する。
- 作業の区切りでは、`docs/HANDOFF.md` の更新案を出す。

設計判断は、理由があれば見直しを提案してよい。\
ただし次の2点は維持する。

- 回答 UI のチェックボックス方式（`AnswerItem.vue`）
- 命名には `resolved` ではなく `merged` を使う

## コード

- コメントは、コードを読むだけでは意図が分からない箇所にだけ書く。\
  処理の説明や仕様の背景は書かない。
- `─── State ───` のような区切り見出しは、意図して置いているので残す。
- 将来起こりうる問題への対策は、必要になった時点で着手する。

## 文書

| ファイル | 役割 |
| --- | --- |
| `README.md` | 利用者向けの導入手順書 |
| `docs/ROADMAP.md` | 方針と段階 |
| `docs/DECISIONS.md` | 決定事項と理由 |
| `docs/HANDOFF.md` | 現在地と残作業。毎回上書きし、経緯は書かない |
| `.claude/rules/` | 領域ごとのルール |

- `docs/` と `.claude/` は引き継ぎ用の文書で、書式の指定は無い。
- `README.md` は GitHub で読みやすいよう、段落の1行を全角50字までにし、\
  段落内の改行には行末の2スペースではなく `\` を使う。
- 決定事項が変わったら `docs/DECISIONS.md` を更新し、\
  ほかの文書と矛盾しないようにする。
