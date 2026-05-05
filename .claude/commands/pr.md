---
description: PR を作成または更新する。タイトルと本文は日本語で記述し、既存 PR があれば更新する
argument-hint: "[追加情報やメモ（省略可）]"
allowed-tools: Bash(git *), Bash(gh *)
---

`@.claude/rules/git-workflow.md` のルールに従い、現在のブランチの PR を作成または更新してください。

## 現在の git 状態

**現在のブランチ:**
```
!`git branch --show-current`
```

**ベースブランチとの差分コミット:**
```
!`git log main..HEAD --oneline`
```

**変更の概要（ファイル単位）:**
```
!`git diff main...HEAD --stat`
```

**変更の詳細:**
```
!`git diff main...HEAD`
```

**既存の PR（このブランチ）:**
```
!`gh pr list --head "$(git branch --show-current)" --json number,title,url 2>/dev/null || echo "PRなし"`
```

## 手順

1. **リモートへのプッシュ確認**
   - ローカルブランチがリモートに存在しない場合、または未プッシュのコミットがある場合は以下を実行する：
     ```bash
     git push -u origin $(git branch --show-current)
     ```

2. **PR タイトルと本文の作成**
   - 上記の diff とコミット履歴を分析し、変更内容を把握する
   - 引数 `$ARGUMENTS` が指定されている場合はそれも考慮する
   - 以下の規則でタイトルと本文を**日本語**で作成する：

   **タイトル（70文字以内）:**
   - 変更内容を簡潔に表す
   - 例: `アイランド生成システムの実装`、`Mixin 登録漏れのバグ修正`

   **本文テンプレート:**
   ```
   ## 概要
   - （変更点を箇条書き）

   ## テスト手順
   - （動作確認手順を箇条書き）
   ```

   テスト手順は変更内容に応じて具体的に記述する（例: `./gradlew runClient` で起動し XX を確認する、など）。

3. **PR の作成または更新**

   既存 PR が**ない**場合：
   ```bash
   gh pr create --title "<タイトル>" --body "<本文>"
   ```

   既存 PR が**ある**場合（PR 番号を使って更新）：
   ```bash
   gh pr edit <番号> --title "<タイトル>" --body "<本文>"
   ```

4. PR の URL をユーザーに伝える。
