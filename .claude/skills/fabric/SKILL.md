---
name: fabric
description: Fabric mod 開発の問題を診断・修正するスキル。アイテムやブロックのテクスチャが Missing Texture になる、Mixin が効かない、クライアントコードがサーバーでクラッシュする、レシピ・ルートテーブルが機能しない、ビルドエラーが発生するといった問題に遭遇したときに使用する。新バージョン（1.21.4以降）での仕様変更に起因するバグに特に有効。
---

# Fabric Mod 開発 診断スキル

このプロジェクトは Minecraft 26.1.x + Fabric Loader + Mojang マッピングで構成されている。
問題を診断するときは、まず症状から原因カテゴリを特定し、対応する節を参照する。

## 診断フロー

症状 → 原因カテゴリ の対応：

| 症状 | 最初に確認する節 |
|------|-----------------|
| アイテム・ブロックの Missing Texture / 紫黒チェック | [アイテムモデル定義] |
| Mixin の @Inject が発火しない | [Mixin 登録] |
| 専用サーバーで `ClassNotFoundException` / クラッシュ | [サイド分離] |
| レシピが機能しない / ルートテーブルが空 | [Identifier と名前空間] |
| `./gradlew build` がコンパイルエラー | [ビルドエラー] |

---

## アイテムモデル定義（MC 1.21.4 以降の重大な仕様変更）

**1.21.4 より前：** `assets/<ns>/models/item/<id>.json` だけでアイテムが表示された。
**1.21.4 以降：** `assets/<ns>/items/<id>.json` という「アイテムモデル定義ファイル」が別途必要。これがないとテクスチャが Missing Texture になる。

### 確認手順

1. `src/client/resources/assets/craft_isle/items/` ディレクトリが存在するか確認
2. 新規登録したすべてのアイテム（`ModItems` に登録したもの）について `<id>.json` があるか確認

### 修正テンプレート

```json
// assets/craft_isle/items/<id>.json
{
  "model": {
    "type": "minecraft:model",
    "model": "craft_isle:item/<id>"
  }
}
```

`BlockItem` の場合もこのファイルは必要。ただし `model` の参照先をブロックモデルにする場合：

```json
{
  "model": {
    "type": "minecraft:model",
    "model": "craft_isle:block/<id>"
  }
}
```

### チェックリスト

- `ModItems` に登録したすべてのアイテム → `items/<id>.json` があるか
- `ModBlocks` に登録したすべてのブロック（BlockItem として登録）→ 同上
- `models/item/<id>.json` のテクスチャパスが `craft_isle:item/<id>` の形式になっているか

---

## Mixin 登録

Mixin クラスを作っても `*.mixins.json` に登録しないと**何も起こらず静かに無視される**。

### 確認手順

1. 共通 Mixin → `src/main/resources/craft_isle.mixins.json` の `"mixins"` 配列に含まれているか
2. クライアント専用 Mixin → `src/client/resources/craft_isle.client.mixins.json` の `"client"` 配列に含まれているか

### よくあるミス

- クライアント専用 Mixin（`@Environment(EnvType.CLIENT)` 付き）を共通の `craft_isle.mixins.json` に登録してしまう
- クラス名のタイポ（パッケージパスも含む）

---

## サイド分離

`src/main/java/` に置いたコードはサーバーでも読み込まれる。クライアント専用クラス（`MinecraftClient`、描画系）を `src/main/` に置くと専用サーバーで `ClassNotFoundException` が発生する。

### ルール

| コード種別 | 配置先 |
|------------|--------|
| 共通ロジック（ブロック・アイテム・エンティティ） | `src/main/java/` |
| クライアント専用（描画・GUI・`MinecraftClient`） | `src/client/java/` |

### 確認手順

- `src/main/` 内で `MinecraftClient` や `net.minecraft.client.*` を import していないか Grep する
- クライアント専用クラスには `@Environment(EnvType.CLIENT)` が付いているか

---

## Identifier と名前空間

Identifier の名前空間が間違うと、レシピ・ルートテーブル・タグ・レジストリ検索がすべて壊れる。

### ルール

- 必ず `Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "name")` の形で生成する
- `CraftIsle.MOD_ID` = `"craft_isle"`
- JSON ファイルのパスと Identifier の `name` 部分が一致しているか確認する

### レシピが機能しない場合の確認

1. `data/craft_isle/recipe/<name>.json` が存在するか（MC 1.21.4+ は **`recipe/`** 単数形）
2. `key` の値がベア文字列になっているか（`"S": "craft_isle:pebble"` — `{"item": "..."}` オブジェクト形式は不要）
3. `pattern` 文字列の空白が **ASCII スペース（U+0020）** であるか（全角スペース U+3000 は無効）
4. `result` の `id` が `"craft_isle:<name>"` になっているか
5. タグ参照 (`#minecraft:...`) のパスが正しいか

レシピ JSON の完全なフォーマット仕様は [recipe-format.md](recipe-format.md) を参照。

---

## ビルドエラー

### よくある原因と対処

| エラー | 原因 | 対処 |
|--------|------|------|
| `cannot find symbol` で Minecraft クラスが見つからない | Mojang マッピング名を使っていない | Yarn 名ではなく Mojang 公式名を使う |
| `src/client` のクラスが `src/main` から参照できない | split source set の設計違反 | クライアント専用クラスは `src/client/` に移動 |
| Loom キャッシュ起因のビルド失敗 | `minecraft_version` 変更後のキャッシュ | `./gradlew clean` 後に再ビルド |

### Mojang マッピングでよく間違えるクラス名

| Yarn 名 | Mojang 名 |
|---------|-----------|
| `net.minecraft.util.Identifier` | `net.minecraft.resources.Identifier` |
| `net.minecraft.util.registry.Registry` | `net.minecraft.core.Registry` |
| `net.minecraft.block.Block` | `net.minecraft.world.level.block.Block` |

---

## 新機能追加時のチェックリスト

ブロック・アイテムを新規追加するたびに確認する：

- [ ] `ModBlocks` / `ModItems` にレジストリ登録
- [ ] `ModBlocks.initialize()` / `ModItems.initialize()` が `CraftIsle.onInitialize()` から呼ばれている
- [ ] `assets/craft_isle/items/<id>.json`（アイテムモデル定義）を作成
- [ ] `assets/craft_isle/models/item/<id>.json`（モデル）を作成
- [ ] `assets/craft_isle/textures/item/<id>.png`（テクスチャ）を配置
- [ ] ブロックの場合は `blockstates/<id>.json` と `models/block/<id>.json` も作成
- [ ] 言語ファイル（`en_us.json` / `ja_jp.json`）にエントリ追加
