# テストルール

## 概要

このプロジェクトでは以下の 2 種類のテストを使用する。

| テスト種別 | 用途 | フレームワーク |
|------------|------|----------------|
| ユニットテスト | Minecraft クラスを使わない純粋なロジック・Minecraft クラスを使うロジック（レジストリ初期化後） | fabric-loader-junit + JUnit 5 |
| ゲームテスト | 実際の Minecraft 環境でのブロック・エンティティ・レシピの動作確認 | Minecraft GameTest Framework |

## コードカバレッジ

**最小カバレッジ: 80%**（ライン・ブランチ両方）

JaCoCo でカバレッジを計測する。`./gradlew check` 実行時に 80% を下回るとビルドが失敗する。

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

### テスト対象の選定
以下はユニットテストでカバーする。

- ヘルパークラス・ユーティリティメソッド
- Codec・データ変換・シリアライズロジック
- ブロック・アイテムのロジック（Minecraft クラスを使う場合はブートストラップ後）

## ゲームテスト規約

### ファイル配置
- `src/gametest/java/org/ponkotate/craftisle/gametest/` 配下に配置する
- クラス名はテスト対象機能名 + `GameTest`（例: `StoneKnifeProgressionGameTest`）

### エントリポイント登録
`src/gametest/resources/fabric.mod.json` を作成する。

```json
{
  "schemaVersion": 1,
  "id": "craft_isle_test",
  "version": "1.0.0",
  "entrypoints": {
    "fabric-gametest": ["org.ponkotate.craftisle.gametest.CraftIsleGameTest"]
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

以下はカバレッジ計測の対象外とする。

| 除外対象 | 理由 |
|----------|------|
| `mixin/**` | Mixin はバイトコード注入で動作するため JaCoCo で正確に計測できない |
| `client/**` | クライアント専用コードはサーバーサイドのテスト環境では実行不可 |
