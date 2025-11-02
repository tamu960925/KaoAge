# KaoAge Face Insights SDK

Face Insights SDK は、店舗向けの年齢確認・顧客案内体験を支援するオンデバイス顔解析ライブラリです。CameraX などで取得したフレームをホストアプリから渡すだけで、顔ランドマーク・姿勢・年齢帯・性別推定・ベストショット判定を同時に取得できます。

## 特長

- **オンデバイス完結** : ML Kit と TensorFlow Lite を用いたローカル推論。画像や個人情報をサーバーに送信しません。
- **Java/Kotlin 両対応** : `@JvmStatic` / `@JvmOverloads` により、既存 Java コードベースからも呼び出しできます。
- **ベストショット判定** : ベストなフレームタイミングを `BestShotSignal` として通知し、撮り直しを減らします。
- **テスト容易性** : `FaceDetector` / `AgeGenderEstimator` などのインターフェースを公開し、テストダブル差し替えが可能です。

## モジュール構成

| モジュール | 役割 |
|-----------|------|
| `sdk-core` | 顔検出、推論、データモデル、設定クラス、`Parcelable` + `toJson()` API |
| `sdk-bestshot` | フレーム品質を評価する `BestShotEngine` 実装 |
| `samples/cashier-app` | CameraX を用いた統合例と UI プレゼンテーション |
| `docs/` | 開発者ガイドや運用メモ |

## 導入手順

1. リポジトリをクローンし、`main` ブランチに切り替えます。  
2. 推論モデルを取得します。  
   ```bash
   scripts/download_models.sh
   ```
3. `settings.gradle.kts` にモジュールを追加します。  
   ```kotlin
   include(":sdk-core", ":sdk-bestshot")
   ```
4. アプリ側 `build.gradle.kts` で依存関係を設定します。  
   ```kotlin
   dependencies {
       implementation(project(":sdk-core"))
       implementation(project(":sdk-bestshot"))
   }
   ```
5. Kotlin 1.9.24 / Java 17、`compileSdk = 35`、`minSdk = 29` を設定し、`kotlin-parcelize` と `kotlin-serialization` プラグインを有効化します。

## クイックスタート

```kotlin
val analyzer = FaceInsightsAnalyzer(context)
val bestShot = BestShotEvaluator()
val sessionConfig = SessionConfig(minFaceConfidence = 0.75f)

@OptIn(ExperimentalGetImage::class)
override fun analyze(imageProxy: ImageProxy) {
    val mediaImage = imageProxy.image ?: return imageProxy.close()
    val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
    val timestamp = System.currentTimeMillis()

    scope.launch {
        try {
            val result = analyzer.analyze(
                image = inputImage,
                imageProxy = imageProxy,
                sessionConfig = sessionConfig,
                frameTimestampMillis = timestamp
            )
            val signal = result?.let { bestShot.evaluate(it, sessionConfig) }
            withContext(Dispatchers.Main) { render(result, signal) }
        } finally {
            imageProxy.close()
        }
    }
}
```

- `FaceInsightsResult` は `Parcelable` と `toJson()` に対応しており、`landmarks`, `ageBracket`, `gender`, `bestShot` などを保持します。
- ベストショットを利用しない場合は `BestShotEvaluator` を呼び出さなくても構いません。

## ドキュメント

- `docs/sdk/developer-guide.md` : セットアップ、CameraX 統合、モデル運用を含む詳細ガイド
- `docs/sdk/architecture.md` : レイヤー構成とデータフロー
- `docs/sdk/camera-handoff.md` : カメラ制御責務とベストプラクティス
- `docs/sdk/model-assets.md` : TensorFlow Lite モデルの取得・検証手順

## サンプルアプリ

`samples/cashier-app` フォルダーには、自己レジ/対面レジの両シナリオを想定したデモが含まれています。CameraX のセットアップ、結果オーバーレイ、ベストショット通知などの参照実装としてご利用ください。

## テストと CI

- `./gradlew test` で Robolectric を含むユニットテストを実行できます。
- モデルが未配置の場合はビルド時に警告が表示されます。CI でも事前に `scripts/download_models.sh` を実行してください。
