---
description: 新規ブランチを作成して作業を開始する
argument-hint: "[作業内容の説明（省略可）]"
allowed-tools: Bash(git *)
---

新規ブランチを作成して作業を開始してください。

## 現在の git 状態

**現在のブランチ:**
```
!`git branch --show-current`
```

**直近のコミット:**
```
!`git log --oneline -3`
```

## 手順

1. 引数 `$ARGUMENTS` が指定されている場合はそれを作業内容として使用する。指定がない場合は、どのような作業を行うか1文で尋ねる。

2. 作業内容からブランチ名を決定する。以下の規則に従う：
   - フォーマット: `<type>/<short-description>`
   - `type` は `feat`・`fix`・`docs`・`refactor`・`chore` から選ぶ
   - `short-description` は英小文字・数字・ハイフンのみ（スペース・スラッシュ不可）
   - 例: `feat/add-new-block`、`fix/mixin-registration`、`docs/update-readme`

3. 以下を実行する：
   ```bash
   git checkout -b <ブランチ名>
   ```

4. 作成したブランチ名をユーザーに伝える。
