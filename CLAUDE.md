# CLAUDE.md

このファイルは、Claude Code (claude.ai/code) がこのリポジトリで作業する際のガイダンスを提供します。

## プロジェクト概要

**Craft Isle** は、[Fabric](https://fabricmc.net/) モッディングフレームワークで構築された Minecraft Java Edition 26.1.x 向け Mod です。

- **Mod 名**: Craft Isle
- **Mod ID**: `craft_isle`
- **ルートパッケージ**: `org.ponkotate.craftisle`
- **Minecraft バージョン**: 26.1.x
- **Mod ローダー**: Fabric Loader
- **ビルドシステム**: Gradle + Fabric Loom
- **言語**: Java 25
- **マッピング**: Mojang 公式マッピング（Fabric Loom 1.16+ はデフォルトで Mojang マッピングを使用）

## ビルド & 実行

```bash
# Mod をビルド（build/libs/ に jar が生成される）
./gradlew build

# Mod を読み込んだ状態で Minecraft クライアントを起動
./gradlew runClient

# Mod を読み込んだ状態で Minecraft サーバーを起動
./gradlew runServer

# IDE 実行構成を生成（IntelliJ 向け）
./gradlew genIntellijRuns

# 参照用に Minecraft ソースを逆コンパイル
./gradlew genSources

# ビルド成果物を削除
./gradlew clean
```

コンパイル済み Mod jar は `build/libs/craft_isle-<version>.jar` に出力されます。

## ディレクトリ構成

```
src/
  main/
    java/org/ponkotate/craftisle/   # 共通（サーバー・クライアント両サイド）コード
      CraftIsle.java                # メイン初期化クラス（ModInitializer 実装）
      mixin/                        # 共通 Mixin クラス
      block/                        # カスタムブロック
      item/                         # カスタムアイテム
      entity/                       # カスタムエンティティ
      registry/                     # レジストリヘルパー（ブロック・アイテム・エンティティ等）
    resources/
      data/craft_isle/              # サーバーデータ（レシピ・ルートテーブル・タグ・進捗）
        recipes/
        loot_tables/
        tags/
      fabric.mod.json               # Mod メタデータとエントリポイント
      craft_isle.mixins.json        # 共通 Mixin 設定
  client/
    java/org/ponkotate/craftisle/
      client/                       # クライアント専用コード（ClientModInitializer 実装）
        CraftIsleClient.java
        mixin/                      # クライアント専用 Mixin
        screen/                     # GUI 画面
    resources/
      assets/craft_isle/            # クライアントアセット（テクスチャ・モデル・言語・サウンド）
        blockstates/
        models/
          block/
          item/
        textures/
          block/
          item/
        lang/
          en_us.json
          ja_jp.json
      craft_isle.client.mixins.json # クライアント専用 Mixin 設定
build.gradle                        # ビルド設定（依存関係・Loom 設定）
gradle.properties                   # バージョン管理（Minecraft・Mod・依存関係）
settings.gradle
```

## 重要ファイル

- **`fabric.mod.json`** — Mod ID・バージョン・エントリポイント・依存関係・説明。新しいエントリポイントや Mixin を追加する際はここを編集する。
- **`gradle.properties`** — すべてのバージョン指定（Minecraft・Fabric Loader・Fabric API）。バージョンを変更する際はまずここを編集する。
- **`build.gradle`** — Loom 設定・追加依存関係（`implementation`・`include`）。
- **`craft_isle.mixins.json`** — 共通 Mixin クラスを列挙する。共通 Mixin はここへの登録が必須。
- **`craft_isle.client.mixins.json`** — クライアント専用 Mixin クラスを列挙する（`src/client/resources/` 配下）。

## 開発規約

### レジストリパターン

コンテンツは専用のレジストリクラスにまとめて登録する。名前空間には必ず Mod ID を使用する。

```java
// org.ponkotate.craftisle.CraftIsle
public class CraftIsle implements ModInitializer {
    public static final String MOD_ID = "craft_isle";

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModItems.initialize();
    }
}

// org.ponkotate.craftisle.registry.ModBlocks
// Mojang マッピング: net.minecraft.core.Registry / BuiltInRegistries / net.minecraft.resources.Identifier
public class ModBlocks {
    public static final Block MY_BLOCK = register("my_block", new Block(new BlockBehaviour.Properties()));

    private static Block register(String name, Block block) {
        return Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, name), block);
    }

    public static void initialize() {} // ModInitializer から呼び出して静的初期化を発火させる
}
```

### エントリポイント

| エントリポイントキー    | インターフェース                     | 実行環境       |
|-------------------------|--------------------------------------|----------------|
| `main`                  | `ModInitializer`                     | 両サイド       |
| `client`                | `ClientModInitializer`               | クライアントのみ |
| `server`                | `DedicatedServerModInitializer`      | サーバーのみ   |

`main` エントリポイントにクライアント専用コード（描画・画面・`MinecraftClient`）を置いてはならない。専用サーバーでクラッシュする。

### Mixin

- すべての Mixin クラスは `mixin/` パッケージ配下に配置する。
- 各クラスを `craft_isle.mixins.json` に登録する。
- 互換性維持のため `@Overwrite` より `@Inject`・`@Redirect`・`@ModifyVariable` を優先する。
- `@Overwrite` より `CallbackInfo` / `CallbackInfoReturnable` を使う。
- クラスとメソッドの指定は **Mojang 公式名**を使用する（Fabric Loom 1.16+ はデフォルトで Mojang マッピングを使用）。

```java
@Mixin(SomeVanillaClass.class)
public class SomeVanillaClassMixin {
    @Inject(method = "targetMethod", at = @At("HEAD"))
    private void onTargetMethod(CallbackInfo ci) {
        // 注入するロジック
    }
}
```

### サイド分離

クライアント専用クラスには `@Environment(EnvType.CLIENT)` を付与する。サイドによって処理を分岐させる場合は `world.isClient` で実行時に確認する。

### Identifier 規約

Identifier は必ず `Identifier.fromNamespaceAndPath(CraftIsle.MOD_ID, "name")` の形で生成する。Mod ID 文字列を複数箇所にハードコードせず、定数を参照する。

### ネットワーキング（必要な場合）

Fabric の `ServerPlayNetworking` / `ClientPlayNetworking` と `CustomPayload` レコード（Fabric API 0.100+）を使用する。ペイロード型は静的な `ID` フィールドと `CODEC` を持たせる。

## 依存関係

すべてのバージョンは `gradle.properties` で管理する。Mod 依存関係は `build.gradle` に `modImplementation` として追加する。同梱（JiJ）する依存関係には `include` を使う。

```groovy
// build.gradle — 依存関係の記述例
implementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_api_version}"
implementation "maven.modrinth:modid:version"    // Modrinth
include implementation("some.library:lib:1.0.0") // 同梱（JiJ）
```

## テスト

Fabric にゲームロジック向けの組み込みユニットテストフレームワークはない。以下を使い分ける。

- **`runClient` / `runServer`** — ゲーム内での手動テスト。
- **[Fabric GameTest](https://fabricmc.net/wiki/tutorial:gametest)** — 自動化が必要な場合の構造ベーステスト。
- **標準 JUnit** — Minecraft クラスを使わない純粋なロジックのユニットテスト。

```bash
./gradlew test        # JUnit テスト（存在する場合）
./gradlew runGametest # GameTest サーバー（設定済みの場合）
```

## 参考リンク

- Fabric Wiki: https://fabricmc.net/wiki/
- Fabric API Javadoc: https://maven.fabricmc.net/docs/fabric-api-latest/
- コミュニティ: https://discord.gg/v6v4pMv (Fabric Discord)

## よくある落とし穴

- **Mixin 登録漏れ** — `@Mixin` クラスが対応する `*.mixins.json` に記載されていない場合、何も起こらず静かに無視される。クライアント専用 Mixin は `craft_isle.client.mixins.json`（`src/client/resources/`）に登録する。
- **split source set でのクライアントコード配置ミス** — クライアント専用クラスは `src/client/java/` に置く。`src/main/java/` に置くと Loom がコンパイルエラーを出す。
- **Identifier の名前空間ミス** — 名前空間が間違っていると、レシピ参照・ルートテーブル・レジストリ検索がすべて壊れる。
- **Loom キャッシュの古さ** — `minecraft_version` を変更した後は、リビルド前に `./gradlew clean` を実行する。
- **Fabric API バージョン不一致** — Fabric API は Minecraft バージョンごとに異なる。`gradle.properties` の `fabric_api_version` が対象 MC バージョンに対応しているか確認する。
- **Mojang マッピング名の使用** — Fabric Loom 1.16+ では Yarn ではなく Mojang 公式マッピングを使用する。クラス名・メソッド名は Mojang 命名規則に従う（例: `Identifier` は `net.minecraft.resources.Identifier`）。
