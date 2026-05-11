---
name: minecraft
description: Minecraft のデータパック・進捗・レシピ・ルートテーブルの JSON フォーマットに関する知識。進捗の背景テクスチャが missing texture になる、レシピ形式がわからない、タグ参照が動かないといった Minecraft 固有のデータ形式に関する問題に使用する。
---

# Minecraft データ形式リファレンス

情報源: https://minecraft.fandom.com/wiki/Minecraft_Wiki
バージョン: Minecraft 26.1.x（Java Edition 1.21.4 相当）

不明な仕様は上記 wiki を WebFetch で参照して調査する。

---

## データファイルパス

MC 1.21.4 以降、各ディレクトリ名が**単数形**に変更された。

| コンテンツ | パス |
|-----------|------|
| レシピ | `data/<ns>/recipe/<name>.json` |
| ルートテーブル | `data/<ns>/loot_table/<name>.json` |
| タグ | `data/<ns>/tag/<category>/<name>.json` |
| 進捗 | `data/<ns>/advancement/<name>.json` |

旧バージョンの `recipes/`・`loot_tables/`（複数形）は無効。

---

## 進捗 (Advancement) JSON フォーマット

### 基本構造

```json
{
  "display": {
    "icon": { "id": "minecraft:sand" },
    "title": { "translate": "advancements.craft_isle.story.my_advancement.title" },
    "description": { "translate": "advancements.craft_isle.story.my_advancement.description" },
    "background": "minecraft:block/blue_concrete_powder",
    "frame": "task",
    "show_toast": true,
    "announce_to_chat": true,
    "hidden": false
  },
  "parent": "craft_isle:story/parent_advancement",
  "criteria": {
    "criterion_name": {
      "trigger": "minecraft:inventory_changed",
      "conditions": {}
    }
  }
}
```

`frame` の値: `"task"`（通常）・`"goal"`（目標）・`"challenge"`（チャレンジ）

`background` はルート進捗（`parent` を持たない進捗）にのみ有効。

### 背景テクスチャ（background フィールド）

**フォーマット（MC 26.1.x）:**

```json
"background": "namespace:path/to/texture"
```

- `textures/` プレフィックスは**不要**
- `.png` 拡張子は**不要**
- ゲームが `assets/namespace/textures/path/to/texture.png` として自動解決する

**バニラの背景テクスチャ一覧:**

| 指定値 | 使用タブ |
|-------|---------|
| `minecraft:gui/advancements/backgrounds/stone` | メイン |
| `minecraft:gui/advancements/backgrounds/adventure` | 冒険 |
| `minecraft:gui/advancements/backgrounds/nether` | ネザー |
| `minecraft:gui/advancements/backgrounds/end` | エンド |
| `minecraft:gui/advancements/backgrounds/husbandry` | 牧畜 |

**ブロックテクスチャを流用する場合:**

```json
"background": "minecraft:block/blue_concrete_powder"
```

- 非アニメーションのブロックテクスチャは背景として使用可能
- `water_still` など `.mcmeta` によるアニメーション付きテクスチャは**不可**（縦長フレームストリップのため壊れる）

**カスタムテクスチャを作成する場合:**

16×16 PNG を `src/client/resources/assets/<ns>/textures/gui/advancements/backgrounds/<name>.png` に配置して：

```json
"background": "craft_isle:gui/advancements/backgrounds/<name>"
```

---

## レシピ JSON フォーマット

詳細: [recipe-format.md](recipe-format.md)

---

## Resource Location フォーマット

JSON 内でアイテム・ブロック・タグを参照する際の形式：

| 参照対象 | 形式 | 例 |
|---------|------|----|
| アイテム | `"namespace:id"` | `"craft_isle:pebble"` |
| タグ | `"#namespace:id"` | `"#minecraft:logs"` |
| 複数候補（配列） | `["id1", "id2"]` | `["minecraft:oak_log", "#minecraft:logs"]` |

---

## 条件 JSON フォーマット（MC 26.1.x）

### BlockPredicate

MC 26.1 で `"tag"` フィールドが削除され、`"blocks"` フィールド（HolderSet）に統合された。

```json
// タグ指定（# プレフィックス必須）
{ "block": { "blocks": "#minecraft:logs" } }

// 単一ブロック指定（配列）
{ "block": { "blocks": ["minecraft:stone"] } }
```

---

## 不明な仕様の調べ方

1. https://minecraft.fandom.com/wiki/Minecraft_Wiki を WebFetch で参照する
2. 主なページ:
   - 進捗: `https://minecraft.fandom.com/wiki/Advancement`
   - レシピ: `https://minecraft.fandom.com/wiki/Recipe`
   - ルートテーブル: `https://minecraft.fandom.com/wiki/Loot_table`
3. バニラの JSON を直接確認したい場合は `javap` でデコンパイル済み jar を調べる（パスは `~/.gradle/caches/fabric-loom/minecraftMaven/...`）
