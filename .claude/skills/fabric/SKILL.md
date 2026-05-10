---
name: fabric
description: Fabric mod 開発の問題を診断・修正するスキル。アイテムやブロックのテクスチャが Missing Texture になる、Mixin が効かない、クライアントコードがサーバーでクラッシュする、レシピ・ルートテーブルが機能しない、ビルドエラーが発生する、カスタム進捗トリガーが動かないといった問題に遭遇したときに使用する。新バージョン（1.21.4・MC 26.1以降）での仕様変更に起因するバグに特に有効。
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
| レシピが機能しない / ルートテーブルが空 | [Identifier と名前空間] / minecraft スキル |
| `./gradlew build` がコンパイルエラー | [ビルドエラー] |
| カスタム進捗トリガーがビルド or 動作しない | [カスタム進捗トリガー] |

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

## バニラブロックのプロパティ変更（hardness など）

### BlockState は初期化時にプロパティをキャッシュする

`BlockBehaviour.Properties.destroyTime` を `onInitialize()` から書き換えても**効果がない**。

理由：Minecraft は `Blocks` クラスのロード時（Mod 初期化より前）に `BlockState.initCache()` を実行し、各 BlockState に `destroySpeed` をキャッシュしてしまう。後から `Properties` を書き換えてもキャッシュには反映されない。

### 正しいアプローチ：`getDestroyProgress` をインターセプト

`BlockBehaviour.getDestroyProgress` に `@Inject` して、対象ブロックのときだけ戻り値を上書きする。

```java
@Mixin(BlockBehaviour.class)
public class BlockHardnessMixin {
    // 石を素手で壊す相当: hardness 1.5 × divisor 100 = 150
    @Unique private static final float CRAFT_ISLE_HARDNESS_FACTOR = 150.0f;

    @Unique private static volatile Set<Block> craftIsle$hardenedBlocks = null;

    @Unique
    private static Set<Block> craftIsle$getHardenedBlocks() {
        Set<Block> set = craftIsle$hardenedBlocks;
        if (set == null) {
            craftIsle$hardenedBlocks = set = Set.of(Blocks.OAK_LOG, /* ... */ );
        }
        return set;
    }

    @Inject(method = "getDestroyProgress", at = @At("HEAD"), cancellable = true)
    private void overrideDestroyProgress(
        BlockState state, Player player, BlockGetter level, BlockPos pos,
        CallbackInfoReturnable<Float> cir
    ) {
        if (craftIsle$getHardenedBlocks().contains(state.getBlock())) {
            cir.setReturnValue(player.getDestroySpeed(state) / CRAFT_ISLE_HARDNESS_FACTOR);
        }
    }
}
```

`player.getDestroySpeed(state)` はツール効率ボーナスを含む速度倍率。素手なら `1.0f`。

### break time の計算式

```
break time (ticks) = hardness × divisor / playerSpeed
```

| 状況 | hardness | divisor | playerSpeed | 秒数 |
|------|----------|---------|-------------|------|
| 石・素手 | 1.5 | 100 | 1.0 | 7.5s |
| 木材・素手（バニラ） | 2.0 | 30 | 1.0 | 3.0s |
| `CRAFT_ISLE_HARDNESS_FACTOR = 150` で素手 | — | — | 1.0 | 7.5s |

---

## Mixin の `@Unique` 静的フィールドと循環初期化

### 問題

`BlockBehaviour` をターゲットにした Mixin で `static final Set<Block> = Set.of(Blocks.OAK_LOG, ...)` を宣言すると**クラッシュ**する。

```
ExceptionInInitializerError
  at BlockBehaviour.<clinit>
Caused by: NullPointerException (FireBlock.createBlockStateDefinition)
```

原因：`@Unique` 静的フィールドは Mixin によって**ターゲットクラスの `<clinit>`** に埋め込まれる。`BlockBehaviour.<clinit>` が走るときに `Blocks` への参照を評価しようとするが、`Blocks` 自体が `BlockBehaviour` を継承するブロックを登録中なので循環初期化になる。

### 解決策：遅延初期化

`Set<Block>` を `null` で宣言し、最初に呼ばれたときだけ初期化する。この時点では `Blocks` は完全に初期化済みなので安全。

```java
@Unique private static volatile Set<Block> craftIsle$mySet = null;

@Unique
private static Set<Block> craftIsle$getMySet() {
    Set<Block> set = craftIsle$mySet;
    if (set == null) {
        craftIsle$mySet = set = Set.of(Blocks.OAK_LOG, /* ... */);
    }
    return set;
}
```

**`float` や `int` などプリミティブ定数は安全**（`Blocks` に依存しないため）：

```java
@Unique private static final float CRAFT_ISLE_FACTOR = 150.0f; // これは OK
```

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

データファイルのパス・JSON フォーマットの詳細は **minecraft スキル** を参照。Fabric 側での確認事項：

1. `Identifier.fromNamespaceAndPath` の名前空間が `"craft_isle"` になっているか
2. レジストリ登録時の名前と JSON ファイル名が一致しているか

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

## カスタム進捗トリガー（MC 26.1 API 変更）

MC 26.1（Fabric API 0.145.1+26.1）では、進捗トリガー周辺のAPIが大幅に変わった。以下の変更点を見落とすと17件以上のコンパイルエラーが発生する。

### パッケージ名変更

`critereon` → `criterion`（`e` が消えた）。

```java
// NG（旧名）
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
// OK（MC 26.1）
import net.minecraft.advancements.criterion.SimpleCriterionTrigger;
```

影響するクラス: `SimpleCriterionTrigger`、`BlockPredicate`、`ItemPredicate`、`ContextAwarePredicate`、`EntityPredicate` など `advancements.criterion.*` 配下のすべて。

### トリガー登録方法の変更

`CriteriaTriggers.register()` が**削除された**。`BuiltInRegistries.TRIGGER_TYPES` に直接登録する。

```java
// NG（旧来の方法 — MC 26.1 では存在しない）
CriteriaTriggers.register(Identifier.fromNamespaceAndPath(MOD_ID, "my_trigger"), new MyTrigger());

// OK（MC 26.1）
Registry.register(
    BuiltInRegistries.TRIGGER_TYPES,
    Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "my_trigger"),
    new MyTrigger()
);
```

### メソッド名衝突の落とし穴

`SimpleCriterionTrigger` には protected メソッド `trigger(ServerPlayer, Predicate<T>)` がある。サブクラスで `trigger(ServerPlayer, ...)` という名前のpublicメソッドを追加するとオーバーロード解決が壊れ、コンパイルエラーになる。**publicメソッド名は `fire()` など別名にする。**

```java
// NG — 親の trigger() と衝突する
public void trigger(ServerPlayer player, ServerLevel level, BlockPos pos) { ... }

// OK — 別名を使う
public void fire(ServerPlayer player, ServerLevel level, BlockPos pos) {
    ItemStack heldItem = player.getMainHandItem();
    this.trigger(player, instance -> instance.matches(level, pos, heldItem)); // 親のtrigger()を呼ぶ
}
```

### ItemPredicate のメソッド名変更

`ItemPredicate.matches()` → `ItemPredicate.test()`（`Predicate<ItemInstance>` を実装するようになった。`ItemStack` は `ItemInstance` を実装している）。

```java
// NG
if (item.isPresent() && !item.get().matches(heldItem)) return false;
// OK
if (item.isPresent() && !item.get().test(heldItem)) return false;
```

### BlockPredicate の JSON フォーマット変更

MC 26.1 で `"tag"` フィールドが削除され `"blocks"`（HolderSet）に統合された。詳細は **minecraft スキル** を参照。

```json
// OK（MC 26.1）
{ "block": { "blocks": "#minecraft:logs" } }
```

### 完全実装パターン

```java
// AttackBlockTrigger.java
public class AttackBlockTrigger extends SimpleCriterionTrigger<AttackBlockTrigger.TriggerInstance> {

    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void fire(ServerPlayer player, ServerLevel level, BlockPos pos) {
        ItemStack heldItem = player.getMainHandItem();
        this.trigger(player, instance -> instance.matches(level, pos, heldItem));
    }

    public record TriggerInstance(
        Optional<ContextAwarePredicate> player,
        Optional<BlockPredicate> block,
        Optional<ItemPredicate> item
    ) implements SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(inst ->
            inst.group(
                EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf("player").forGetter(TriggerInstance::player),
                BlockPredicate.CODEC.optionalFieldOf("block").forGetter(TriggerInstance::block),
                ItemPredicate.CODEC.optionalFieldOf("item").forGetter(TriggerInstance::item)
            ).apply(inst, TriggerInstance::new)
        );

        public boolean matches(ServerLevel level, BlockPos pos, ItemStack heldItem) {
            if (block.isPresent() && !block.get().matches(level, pos)) return false;
            if (item.isPresent() && !item.get().test(heldItem)) return false;
            return true;
        }
    }
}
```

```java
// ModTriggers.java
public class ModTriggers {
    public static final AttackBlockTrigger ATTACK_BLOCK = Registry.register(
        BuiltInRegistries.TRIGGER_TYPES,
        Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "attack_block"),
        new AttackBlockTrigger()
    );
    public static void initialize() {}
}
```

```java
// CraftIsle.java（onInitialize 内）
ModTriggers.initialize();
AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
    if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
        ModTriggers.ATTACK_BLOCK.fire(serverPlayer, (ServerLevel) world, pos);
    }
    return InteractionResult.PASS;
});
```

### APIを実際に確認する方法

コンパイルエラーが解決できないときは `javap` でデコンパイルされた jar を直接調べる。

```bash
# MC jar のパス（バージョンに合わせて変更）
JAR="$HOME/.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.1/minecraft-merged-deobf-26.1.jar"

# クラスのメソッドシグネチャを確認
javap -classpath "$JAR" -p net.minecraft.advancements.criterion.SimpleCriterionTrigger
javap -classpath "$JAR" -p net.minecraft.advancements.criterion.ItemPredicate
```

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
