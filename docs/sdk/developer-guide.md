# Face Insights SDK 開発者ガイド

## このドキュメントについて

Face Insights SDK は、ホストアプリが所有するカメラフレームを使って、顔ランドマーク検出・年齢/性別推定・ベストショット判定をすべてオンデバイスで実行するライブラリです。本ガイドは SDK をアプリにリンクして利用する Android 開発者向けに、環境準備から運用のベストプラクティスまでを網羅的に説明します。

## 想定環境

- Android 10 (API 29) 以上のデバイス
- Kotlin 1.9.24 / Java 17（`@JvmStatic` や `@JvmOverloads` で Java 互換 API を提供）
- Android Gradle Plugin 8.6.0、Gradle 8.7
- CameraX を用いたカメラ制御（カメラのライフサイクルはホストアプリが管理）
- ML Kit Face Detection（オンデバイスモード）
- TensorFlow Lite 2.13.0（年齢・性別モデル推論用）
- ネットワーク接続不要（SDK はネットワークアクセスせず、結果はメモリ内で完結）

## モジュール構成

- `sdk-core`  
  顔検出、年齢/性別推論、`Parcelable` + `toJson()` 可能なデータモデル、設定クラスを提供します。
- `sdk-bestshot`  
  フレーム品質のヒューリスティクスを実装する `BestShotEngine` を扱います。
- `samples/cashier-app`  
  CameraX での統合例と UI プレゼンテーションのリファレンスを示します。
- `docs/`  
  本ガイドや構成図、モデル管理ポリシーなどのドキュメント群です。
- `scripts/`  
  推論モデルを取得する `download_models.sh` など、ビルド前処理用のスクリプトを格納します。

## セットアップ手順

1. **リポジトリを取得**  
   ```
   git clone git@github.com:kaoage/FaceInsights.git
   cd FaceInsights
   git checkout main
   ```
2. **推論モデルのダウンロード**  
   ```
   scripts/download_models.sh
   ```
   - `models/model_age_nonq.tflite` と `models/model_gender_nonq.tflite` が作成されます。
   - 署名付きチェックサムは `docs/compliance/dependency-register.md` を参照してください。
3. **モジュールをプロジェクトに追加**  
   `settings.gradle.kts` に以下を追記します。
   ```kotlin
   include(":sdk-core", ":sdk-bestshot")
   ```
4. **依存関係をリンク**  
   アプリ側の `build.gradle.kts` でモジュールを参照します。
   ```kotlin
   dependencies {
       implementation(project(":sdk-core"))
       implementation(project(":sdk-bestshot"))
   }
   ```
5. **Kotlin/Java オプション**  
   - `compileSdk = 35`, `minSdk = 29` を設定
   - `kotlinOptions.jvmTarget = "17"`
   - `buildFeatures { buildConfig = false }`
   - `plugins` に `kotlin-parcelize` と `kotlin-serialization` を有効化

## クイックスタート（CameraX + FaceInsightsAnalyzer）

最短で結果を得たい場合は `FaceInsightsAnalyzer` を利用します。CameraX の `ImageAnalysis.Analyzer` から `ImageProxy` を受け取り、以下の手順で SDK に渡します。

```kotlin
@AndroidEntryPoint
class CheckoutAnalyzer @Inject constructor(
    private val context: Context
) : ImageAnalysis.Analyzer {

    private val analyzer = FaceInsightsAnalyzer(context)
    private val bestShot = BestShotEvaluator()
    private val sessionConfig = SessionConfig(
        minFaceConfidence = 0.75f,
        minFaceSizeRatio = 0.18f
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val timestamp = System.currentTimeMillis()

        analyzerScope.launch {
            try {
                val result = analyzer.analyze(
                    image = input,
                    imageProxy = imageProxy,
                    sessionConfig = sessionConfig,
                    frameTimestampMillis = timestamp
                )
                val signal = result?.let { bestShot.evaluate(it, sessionConfig) }
                withContext(Dispatchers.Main) {
                    render(result, signal)
                }
            } finally {
                imageProxy.close()
            }
        }
    }
}
```

- `analyze` はサスペンド関数です。`lifecycleScope` や専用 `CoroutineScope` から呼び出してください。
- `imageProxy.close()` はホスト側の責務です。必ず `finally` ブロックで呼び出します。
- `SessionConfig` で検出閾値やクールダウンを調整できます（詳細は後述）。
- `BestShotEvaluator` で `FaceInsightsResult` を入力し、必要に応じて「保存する価値のあるフレーム」を判定します。

### 結果の扱い

`FaceInsightsResult` には以下が含まれます。

- `detectionId` / `frameTimestampMillis` : フレーム識別子
- `boundingBox` / `eulerAngles` : 顔位置と姿勢（yaw/pitch/roll）
- `landmarks` と `landmarkPresence` : 目・鼻・口の座標と検出有無
- `ageBracket` / `ageConfidence` : 年齢帯と信頼度
- `gender` / `genderConfidence` : 性別推定と信頼度（`UNDETERMINED` を含む）
- `bestShotEligible` / `bestShotReasons` : ベストショット判定の可否と理由
- `estimatedAgeYears` : 推論モデルが有効な場合の実数年齢推定
- `toJson()` / `Parcelable` : 画面間データ受け渡しやローカルログに利用

## 高度な統合（FaceInsightsSession）

より細かくパイプラインを制御したい場合は、`FaceInsightsSession` を使用します。`FrameInput` を介して `ImageProxy` を渡し、検出器・推論器・ベストショット橋渡しを差し替え可能です。

```kotlin
val detector: FaceDetector = MlKitFaceAnalyzer()
val estimator: AgeGenderEstimator = AgeGenderEngine(
    ageModel = ModelAssetManager.loadAgeModelByteBuffer(context),
    genderModel = ModelAssetManager.loadGenderModelByteBuffer(context)
)
val bestShotBridge = BestShotBridge(
    evaluator = BestShotEngine(),
    onBestShot = { signal -> viewModel.onBestShot(signal) },
    onLowConfidence = { viewModel.onLowConfidence() }
)

val session = FaceInsightsSession(
    detector = detector,
    estimator = estimator,
    config = DetectionSessionConfig(),
    bestShotBridge = bestShotBridge
)
```

`session.analyze(FrameInput(timestampMillis = now, imageProxy = proxy))` を呼び出すと、検出・推論結果が統合された `FaceInsightsResult` が返ります。`BestShotBridge` は推論が閾値に満たない場合でも低信頼度ハンドラを呼び出すので、UI に再案内を表示できます。

## 設定のカスタマイズ

`DetectionSessionConfig` で調整できる主な項目と既定値は以下のとおりです。

| 項目 | 既定値 | 説明 |
|------|--------|------|
| `minFaceConfidence` | 0.7 | 検出結果を採用する最小顔信頼度 |
| `minFaceSizeRatio` | 0.15 | 画像全体に対する顔領域サイズ比 |
| `minAgeConfidence` / `minGenderConfidence` | 0.6 | 年齢・性別推定を採用する最小信頼度 |
| `maxFrameLatencyMillis` | 500 | 解析処理の SLA（ログ用途） |
| `enableBestShot` | true | ベストショット判定の有効/無効 |
| `cooldownMillis` | 2500 | ベストショット連続発火の抑制時間 |

流暢な Kotlin/Java API のために `DetectionConfigBuilder` も用意しています。

```kotlin
val config = DetectionConfigBuilder()
    .minFaceConfidence(0.8f)
    .minFaceSizeRatio(0.2f)
    .cooldownMillis(1500L)
    .build()
```

## モデル管理

- `scripts/download_models.sh` が MIT ライセンスの公開モデルをダウンロードし、`models/` ディレクトリに保存します。
- SDK は `ModelAssetManager` を通じて `ByteBuffer` または一時ファイルとしてモデルを読み込みます。
- モデルファイルはリポジトリにコミットせず、配布 APK には Android Asset として同梱されます。
- モデルが見つからない場合、SDK は安全側に倒してヒューリスティック結果（推定不能または低信頼）を返します。

## ベストショット評価

`sdk-bestshot` の `BestShotEngine` は `BestShotEvaluator` を実装し、検出のクールダウンや信頼度を考慮したシグナルを返します。

```kotlin
val signal = bestShotEngine.evaluate(detection, config)
if (signal?.eligible == true) {
    // UI に保存ボタンを表示する、など
}
```

クールダウン期間中は `eligible = false` で返り、UI 側で再トライやポーズ調整を案内できます。

## JSON と Parcelable

すべてのモデル (`FaceInsightsResult`, `BestShotSignal`, `BoundingBox` など) は `Parcelable` 実装済みであり、`kotlinx.serialization` による `toJson()` / `fromJson()` を提供します。ログ保存や Fragment 間受け渡し、サービス連携で統一フォーマットを活用できます。

## テスト戦略

- SDK 自体は Robolectric と JUnit による TDD を前提に実装されています。アプリ側も `BestShotEngineTest` や `FaceInsightsPipelineTest` を参考にしながらユニットテストを追加してください。
- モデルの有無、低照度環境、マスク着用といったエッジケースはモックや固定データで再現すると迅速に検証できます。
- CI では `./gradlew test` がオンデバイス判定を含むテストを実行します（ネットワーク不要）。

## プライバシーと運用上の注意

- SDK は画像を保存せず、結果オブジェクトのみを返します。必要に応じてアプリ側でセッション終了時に `FaceInsightsResult` を破棄してください。
- 解析やログは端末内にとどめ、ネットワーク送信が必要な場合は法務・プライバシーチームの承認を得てください。
- 端末の熱暴走や処理落ちに備え、UI 側では 500 ms を超過する場合のリトライ処理やユーザー通知を実装してください。

## よくある質問

- **Q. Java プロジェクトでも使えますか？**  
  A. 主要 API は `@JvmStatic` / `@JvmOverloads` を付与しており、`FaceInsightsAnalyzer` や `DetectionConfigBuilder` は Java からも呼び出せます。
- **Q. ベストショットを使わずに結果だけ欲しい場合は？**  
  A. `enableBestShot = false` を設定するか、`BestShotEvaluator` を呼び出さなければ追加コストは発生しません。
- **Q. モデルを差し替えるには？**  
  A. 同じ入力形式の TensorFlow Lite モデルを `models/` に配置し、チェックサムとライセンス情報を更新してください。`ModelAssetManager` はファイル名でモデルを識別します。

---

SDK に関する質問や改善要望は `docs/` 内のガイドラインに沿って Issue を作成してください。
