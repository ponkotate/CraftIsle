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
- **言語**: Java 21
- **マッピング**: Yarn

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
    java/org/ponkotate/craftisle/   # Mod ソースコード
      CraftIsle.java                # メイン初期化クラス（ModInitializer 実装）
      client/                       # クライアント専用コード（ClientModInitializer 実装）
      mixin/                        # バニラ挙動を変更する Mixin クラス
      block/                        # カスタムブロック
      item/                         # カスタムアイテム
      entity/                       # カスタムエンティティ
      screen/                       # GUI 画面（クライアント専用）
      registry/                     # レジストリヘルパー（ブロック・アイテム・エンティティ等）
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
      data/craft_isle/              # サーバーデータ（レシピ・ルートテーブル・タグ・進捗）
        recipes/
        loot_tables/
        tags/
      fabric.mod.json               # Mod メタデータとエントリポイント
      craft_isle.mixins.json        # Mixin 設定
build.gradle                        # ビルド設定（依存関係・Loom 設定）
gradle.properties                   # バージョン管理（Minecraft・Mod・マッピング）
settings.gradle
```

## 重要ファイル

- **`fabric.mod.json`** — Mod ID・バージョン・エントリポイント・依存関係・説明。新しいエントリポイントや Mixin を追加する際はここを編集する。
- **`gradle.properties`** — すべてのバージョン指定（Minecraft・Fabric Loader・Fabric API・Yarn マッピング）。バージョンを変更する際はまずここを編集する。
- **`build.gradle`** — Loom 設定・追加依存関係（`modImplementation`・`include`）。
- **`craft_isle.mixins.json`** — すべての Mixin クラスを列挙する。新しい Mixin はここへの登録が必須。

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
public class ModBlocks {
    public static final Block MY_BLOCK = register("my_block", new Block(AbstractBlock.Settings.create()));

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, Identifier.of(CraftIsle.MOD_ID, name), block);
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
- クラスとメソッドの指定は **Yarn 名**を使用する（中間名や公式名ではない）。

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

Identifier は必ず `Identifier.of(CraftIsle.MOD_ID, "name")` の形で生成する。Mod ID 文字列を複数箇所にハードコードせず、定数を参照する。

### ネットワーキング（必要な場合）

Fabric の `ServerPlayNetworking` / `ClientPlayNetworking` と `CustomPayload` レコード（Fabric API 0.100+）を使用する。ペイロード型は静的な `ID` フィールドと `CODEC` を持たせる。

## 依存関係

すべてのバージョンは `gradle.properties` で管理する。Mod 依存関係は `build.gradle` に `modImplementation` として追加する。同梱（JiJ）する依存関係には `include` を使う。

```groovy
// build.gradle — 依存関係の記述例
modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
modImplementation "maven.modrinth:modid:version"    // Modrinth
include modImplementation("some.library:lib:1.0.0") // 同梱（JiJ）
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
- Yarn マッピングビューア: https://lambdaurora.dev/tools/yarn_viewer/
- コミュニティ: https://discord.gg/v6v4pMv (Fabric Discord)

## よくある落とし穴

- **Mixin 登録漏れ** — `@Mixin` クラスが `craft_isle.mixins.json` に記載されていない場合、何も起こらず静かに無視される。
- **サーバーでのクライアントコード** — `main` エントリポイントで `MinecraftClient`・`Screen`・描画クラスを参照すると専用サーバーがクラッシュする。
- **Identifier の名前空間ミス** — 名前空間が間違っていると、レシピ参照・ルートテーブル・レジストリ検索がすべて壊れる。
- **Loom キャッシュの古さ** — `minecraft_version` や `mappings_version` を変更した後は、リビルド前に `./gradlew clean` を実行する。
- **Fabric API バージョン不一致** — Fabric API は Minecraft バージョンごとに異なる。`gradle.properties` の `fabric_version` が対象 MC バージョンに対応しているか確認する。
