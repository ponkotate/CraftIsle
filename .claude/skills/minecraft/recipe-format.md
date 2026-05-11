# Minecraft レシピ JSON フォーマット（MC 1.21.4+）

## ディレクトリ

```
src/main/resources/data/craft_isle/recipe/<name>.json
```

MC 1.21.4 以降はディレクトリ名が **単数形**（`recipe/`）。旧バージョンの `recipes/`（複数形）は無効。

同様に `loot_table/`（単数形）、`tag/`（単数形）も注意。

---

## minecraft:crafting_shaped（整形クラフト）

```json
{
  "type": "minecraft:crafting_shaped",
  "pattern": ["RS", "I "],
  "key": {
    "R": "craft_isle:plastic_rope",
    "S": "craft_isle:pebble",
    "I": "minecraft:stick"
  },
  "result": {
    "id": "craft_isle:stone_knife",
    "count": 1
  }
}
```

### pattern

- 文字列の配列（最大 3 要素、各要素は最大 3 文字）
- 空きスロットは **ASCII スペース（U+0020）** で表現する
  - **全角スペース（U+3000）は無効**（クラフトが成立しない）
- 使用する文字はすべて `key` で定義すること

### key

- オブジェクトキー：`pattern` に使った 1 文字
- 値の形式：
  - アイテム ID（ベア文字列）：`"S": "craft_isle:pebble"`
  - タグ（`#` プレフィックス）：`"L": "#minecraft:logs"`
  - 複数候補（配列）：`"W": ["minecraft:oak_log", "minecraft:birch_log"]`
- `{"item": "..."}` オブジェクト形式は**不要**（1.21.4+ では使わない）

### result

```json
"result": {
  "id": "craft_isle:stone_knife",
  "count": 1
}
```

`count` は省略可（デフォルト 1）。

---

## minecraft:crafting_shapeless（不整形クラフト）

```json
{
  "type": "minecraft:crafting_shapeless",
  "ingredients": [
    "craft_isle:pebble",
    "minecraft:stick"
  ],
  "result": {
    "id": "craft_isle:stone_knife",
    "count": 1
  }
}
```

`ingredients` の値も `key` と同じベア文字列形式。

---

## よくある失敗パターン

| 症状 | 原因 |
|------|------|
| レシピが機能しない | ディレクトリが `recipes/`（旧形式）になっている |
| レシピが機能しない | `pattern` に全角スペース（U+3000）が混入している |
| レシピが機能しない | `key` の値を `{"item": "..."}` オブジェクト形式にしている |
| クラフト結果が出ない | `result.id` の名前空間が間違っている |
