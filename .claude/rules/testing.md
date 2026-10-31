---
paths:
  - "vue-frontend/src/**/*.test.ts"
  - "vue-frontend/e2e/**"
  - "vue-frontend/vitest.setup.ts"
  - "spring-backend/src/test/**"
---

# テスト

- Vitest はロジック、Playwright は見た目と挙動を確認する。\
  Reka UI の Portal、`as-child` の入れ子、クリックで開くかといった実際の挙動は、\
  e2e で確認する。
- テストは、表示テキスト・role・アクセシブル名で書く。
- ブラウザ標準の挙動は検証しない。\
  自前で用意した要素や属性は検証する。
- テストを書く前に、同じ領域の既存テストを読み、書き方を揃える。
- ツールチップは `getByRole('tooltip')` ではなく、\
  `[data-slot="tooltip-content"]` で取得する。\
  Reka UI は、`aria-hidden` の要素に `role="tooltip"` を付けるため。
- `@vue/test-utils` は、`flushPromises` のためだけに残している。
- Vitest のタイムゾーンは `Asia/Tokyo` に固定している（`vite.config.ts`）。
- テストを書き直したら、実装を意図的に壊して失敗することを確かめる。
- `ValidationError.vue` の、設問タイトルが無い場合のガードとそのテストは残す。\
  将来の拡張で不具合が起きたときに検出するためのもの。
