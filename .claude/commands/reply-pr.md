---
description: PR レビューコメントの対応要否を判断し、各コメントに返信する
argument-hint: "[PR番号（省略時は現在のブランチの PR）]"
allowed-tools: Bash(gh *), Bash(git *)
---

現在のブランチまたは指定された PR のレビューコメントを分析し、対応要否を判断して各コメントに日本語で返信してください。

## 現在の git 状態

**現在のブランチ:**
```
!`git branch --show-current`
```

**直近のコミット:**
```
!`git log --oneline -10`
```

**対象 PR:**
```
!`PR_ARG="$ARGUMENTS"; if [ -n "$PR_ARG" ]; then gh pr view "$PR_ARG" --json number,title,url; else gh pr list --head "$(git branch --show-current)" --json number,title,url 2>/dev/null || echo "PRなし"; fi`
```

**PR レビューコメント一覧:**
```
!`PR_ARG="$ARGUMENTS"; PR_NUM=$([ -n "$PR_ARG" ] && echo "$PR_ARG" || gh pr list --head "$(git branch --show-current)" --json number --jq '.[0].number' 2>/dev/null); [ -n "$PR_NUM" ] && gh api repos/{owner}/{repo}/pulls/$PR_NUM/comments --jq '[.[] | {id: .id, path: .path, line: .line, body: .body, in_reply_to_id: .in_reply_to_id}]' || echo "PRが見つかりません"`
```

**最新の diff（main との差分）:**
```
!`git diff main...HEAD --stat`
```

## 手順

### 1. コメントの収集と整理

- 上記の一覧から `in_reply_to_id` が `null` のコメント（元コメント）のみを対象とする
- すでに自分が返信済みのコメント（`in_reply_to_id` が非 null のコメントが存在するもの）は除外する
- 返信が必要なコメント一覧を整理する

### 2. 各コメントの対応要否判断

各コメントについて以下の観点で判断する：

**対応が必要な場合（`要対応`）:**
- バグ・論理エラーの指摘
- セキュリティ上の問題
- パフォーマンスへの重大な影響
- 仕様に反する実装

**対応不要な場合（`対応不要`）:**
- レビュアーのコード誤読・誤解
- すでに修正済みの指摘（コミット履歴で確認）
- スタイルの好みの違いで機能に影響しないもの
- プロジェクト方針と合致しない提案

判断が難しい場合は、コードを実際に読んで確認する。

### 3. 対応が必要なコメントへの修正

`要対応` と判断したコメントについて、コードを修正してからコミットする。
修正後、`/commit` に相当する手順でコミットを作成する。

### 4. 各コメントへの返信

すべての元コメントに対して、以下の形式で**日本語**で返信する：

**返信の内容（判定ごと）:**

- `要対応・対応済み`: 修正内容とコミットハッシュを明記する
  > 例: 「対応済みです（`abc1234`）。〇〇を△△に修正しました。」

- `対応不要（誤読）`: 該当コードの動作を説明し、問題がないことを示す
  > 例: 「ご指摘ありがとうございます。〇〇のため、この条件は到達可能です。」

- `対応不要（方針）`: プロジェクトの設計意図を説明する
  > 例: 「ご指摘ありがとうございます。この実装は〇〇のため意図的なものです。」

**返信コマンド（PR番号と各コメントIDを使って実行）:**

```bash
gh api repos/{owner}/{repo}/pulls/1/comments/<comment_id>/replies \
  -X POST -f "body=<返信本文>"
```

### 5. 結果のサマリー

全コメントへの返信が完了したら、以下の形式でサマリーを出力する：

| # | ファイル | 判定 | 対応内容 |
|---|---------|------|---------|
| 1 | path/to/file.java:L10 | 対応済み | 〇〇を修正（abc1234） |
| 2 | path/to/file.java:L20 | 対応不要 | レビュアーの誤読。〇〇のため問題なし |
