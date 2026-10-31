---
paths:
  - "spring-backend/**"
---

# バックエンド

- 層は controller・service・repository・entity・dto に分ける。\
  dto は request と response を分け、入力の検証は request 側に持たせる。
- 例外とエラー処理は、細かく分けることより、\
  トラブル時に問題箇所を特定しやすいことを優先する。\
  専用の例外クラスは、必要になってから作る。
- DB のスキーマ変更は Flyway のマイグレーションで行う。\
  適用済みのファイルは編集しない。
- 外部への副作用（Lambda の Invoke など）は、トランザクションのコミット後に行う。\
  service 層で `@TransactionalEventListener(AFTER_COMMIT)` を使い、\
  コミット前に実行されないことを結合テストで確認する。
- 子テーブルの削除は、外部キーの `on delete cascade` に任せる。
- PostgreSQL は外部キー列にインデックスを自動で作らない。\
  検索や削除に使う列には、インデックスを明示的に追加する。
- 期限切れデータの定期削除は、利用者のいない時間帯として、\
  日本時間の午前3時に実行する。
