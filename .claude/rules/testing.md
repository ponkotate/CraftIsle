# テストルール

## 概要

このプロジェクトでは以下の 2 種類のテストを使用する。

| テスト種別 | 用途 | フレームワーク |
|------------|------|----------------|
| ユニットテスト | Minecraft クラスを使わない純粋なロジック・Minecraft クラスを使うロジック（レジストリ初期化後） | fabric-loader-junit + JUnit 5 |
| ゲームテスト | 実際の Minecraft 環境でのブロック・エンティティ・レシピの動作確認 | Minecraft GameTest Framework |

## コードカバレッジ

**最小カバレッジ: 60%**（ライン・ブランチ両方）

JaCoCo でカバレッジを計測する。`./gradlew check` 実行時に 60% を下回るとビルドが失敗する。

**注意**: `canSurvive`・`useOn` など Level に依存するブロック/アイテムメソッドはゲームテストでのみ実行可能であり、JaCoCo のカバレッジには計上されない。また、レジストリ登録コード・ModInitializer エントリポイントはユニットテストでは実行されない。これらをカバレッジ除外対象に含めることで実質的なロジックのみを計測する。

## Gradle 設定

`build.gradle` に以下を追加する。

```groovy
plugins {
    id 'net.fabricmc.fabric-loom' version "${loom_version}"
    id 'maven-publish'
    id 'jacoco'  // 追加
}

dependencies {
    // ...既存の依存関係...
    testImplementation "net.fabricmc:fabric-loader-junit:${project.loader_version}"
}

// ユニットテスト
test {
    useJUnitPlatform()
    finalizedBy jacocoTestReport
}

// ゲームテスト
fabricApi {
    configureTests {
        createSourceSet = true
        modId = "craft_isle_test"
        enableGameTests = true
        eula = true
    }
}

// JaCoCo カバレッジレポート
jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
    // Mixin・クライアント専用コードをカバレッジ計測から除外
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                'org/ponkotate/craftisle/mixin/**',
                'org/ponkotate/craftisle/client/**',
            ])
        }))
    }
}

// カバレッジ閾値検証（80% 未満でビルド失敗）
jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = 'LINE'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
            limit {
                counter = 'BRANCH'
                value = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```

## ディレクトリ構成

```
src/
  test/
    java/org/ponkotate/craftisle/       # ユニットテスト
      <domain>/
        <ClassName>Test.java
  gametest/
    java/org/ponkotate/craftisle/       # ゲームテスト
      gametest/
        <FeatureName>GameTest.java
    resources/
      fabric.mod.json                   # ゲームテスト用エントリポイント
```

## ユニットテスト規約

### ファイル配置
- `src/test/java/` 配下に、テスト対象と同じパッケージ構造で配置する
- クラス名はテスト対象クラス名 + `Test`（例: `MyBlock` → `MyBlockTest`）

### Minecraft クラスへの依存がある場合
レジストリ等を使うテストでは `@BeforeAll` でブートストラップを実行する。

```java
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;

class MyBlockTest {
    @BeforeAll
    static void beforeAll() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }
}
```

### MC 26.1 固有の制約

#### ブロック構築時の ID 必須
MC 26.1 では `BlockBehaviour.Properties` に `.setId(ResourceKey<Block>)` が必須。指定しないと `effectiveDrops()` 等で `NullPointerException` が発生する。ユニットテストでブロックを直接インスタンス化する場合は必ず指定する。

```java
ResourceKey<Block> key = ResourceKey.create(
    Registries.BLOCK,
    Identifier.fromNamespaceAndPath("test", "my_block")
);
MyBlock block = new MyBlock(BlockBehaviour.Properties.of().setId(key));
```

#### `ItemStack` のコンポーネントバインディング制限
`Bootstrap.bootStrap()` のみではアイテムコンポーネントのバインディングが行われないため、`new ItemStack(someItem)` は `NullPointerException: Components not bound yet` で失敗する。ユニットテストでは `ItemStack.EMPTY` を使用するか、アイテムインスタンスを必要としないアプローチを取る。

#### `ItemPredicate` の直接構築
`ItemPredicate.CODEC.parse(JsonOps.INSTANCE, ...)` はアイテムレジストリへの `RegistryOps` アクセスが必要なため、ユニットテスト環境では失敗する。レコードコンストラクタで直接生成する。

```java
// NG: JsonOps.INSTANCE では registry アクセスできない
// ItemPredicate.CODEC.parse(JsonOps.INSTANCE, json)

// OK: レコードコンストラクタで直接生成
ItemPredicate predicate = new ItemPredicate(
    Optional.empty(),
    MinMaxBounds.Ints.exactly(1),
    DataComponentMatchers.ANY
);
```

### テスト対象の選定
以下はユニットテストでカバーする。

- ヘルパークラス・ユーティリティメソッド
- Codec・データ変換・シリアライズロジック
- ブロック・アイテムのロジック（Minecraft クラスを使う場合はブートストラップ後）

## ゲームテスト規約

### ファイル配置
- `src/gametest/java/org/ponkotate/craftisle/gametest/` 配下に配置する
- クラス名はテスト対象機能名 + `GameTest`（例: `StoneKnifeProgressionGameTest`）

### アノテーション（Fabric API 4.0.x / MC 26.1）

Fabric API 4.0.x では `FabricGameTest` インターフェースと `@net.minecraft.gametest.framework.GameTest` アノテーションが廃止された。代わりに Fabric 独自のアノテーションを使用する。

```java
import net.fabricmc.fabric.api.gametest.v1.GameTest;

public class MyFeatureGameTest {

    // 空の構造（デフォルト）— 属性不要
    @GameTest
    public void myTest(GameTestHelper helper) { ... }

    // カスタム構造を使う場合
    @GameTest(structure = "craft_isle:my_structure")
    public void myStructureTest(GameTestHelper helper) { ... }
}
```

デフォルト構造は `"fabric-gametest-api-v1:empty"`（5×5×5 の空間）。`@GameTest` に属性を指定しなければ自動で使用される。

### `GameTestHelper` のメソッドシグネチャ（MC 26.1）

エラーメッセージは `String` / `Supplier<String>` ではなく `Function<T, Component>` を渡す。

```java
import net.minecraft.network.chat.Component;

// assertBlock: Function<Block, Component>
helper.assertBlock(pos, b -> b == Blocks.STONE,
    b -> Component.literal("expected stone"));

// assertBlockState: Function<BlockState, Component>
helper.assertBlockState(pos, s -> s.getValue(MyBlock.COUNT) == 3,
    s -> Component.literal("count should be 3"));

// isAir: Block.isAir() は廃止、BlockState.isAir() を使う
helper.assertBlockState(pos, s -> s.isAir(),
    s -> Component.literal("block should be air"));
```

### エントリポイント登録
`src/gametest/resources/fabric.mod.json` を作成する。ゲームテストクラスは直接列挙できる。

```json
{
  "schemaVersion": 1,
  "id": "craft_isle_test",
  "version": "1.0.0",
  "entrypoints": {
    "fabric-gametest": [
      "org.ponkotate.craftisle.gametest.MyFeatureGameTest",
      "org.ponkotate.craftisle.gametest.AnotherFeatureGameTest"
    ]
  }
}
```

### テスト対象の選定
以下はゲームテストでカバーする。

- ブロックの設置・破壊・状態遷移
- クラフトレシピ・製錬の動作確認
- エンティティの挙動
- マルチブロック構造・隣接ブロック依存の動作

## テスト実行コマンド

```bash
# ユニットテスト実行 + カバレッジレポート生成
./gradlew test jacocoTestReport

# カバレッジ検証を含む全チェック（CI と同等）
./gradlew check

# ゲームテスト（サーバー）
./gradlew runGametest

# ゲームテスト（クライアント）
./gradlew runClientGameTest

# カバレッジレポートを確認
# build/reports/jacoco/test/html/index.html をブラウザで開く
```

## CI 統合（GitHub Actions）

```yaml
- name: Run tests
  run: ./gradlew check

- name: Store test reports
  if: failure()
  uses: actions/upload-artifact@v4
  with:
    name: reports
    path: |
      **/build/reports/
      **/build/test-results/
```

## カバレッジ除外対象

以下はカバレッジ計測の対象外とする。`jacocoTestReport` と `jacocoTestCoverageVerification` の両タスクに同じ除外リストを適用すること（片方だけ適用しても検証タスクが失敗する）。

| 除外対象 | 理由 |
|----------|------|
| `mixin/**` | Mixin はバイトコード注入で動作するため JaCoCo で正確に計測できない |
| `client/**` | クライアント専用コードはサーバーサイドのテスト環境では実行不可 |
| `block/**` | Level 依存メソッド（`canSurvive`, `neighborChanged` 等）はユニットテスト不可。GameTest でカバー |
| `item/**` | Level 依存メソッド（`useOn` 等）はユニットテスト不可。GameTest でカバー |
| `worldgen/**` | フル MC サーバーコンテキストが必要。GameTest でカバー |
| `registry/**` | レジストリ登録ボイラープレート。`onInitialize()` はユニットテストでは呼ばれない |
| `CraftIsle*` | `ModInitializer` エントリポイント。`onInitialize()` はユニットテストでは呼ばれない |
