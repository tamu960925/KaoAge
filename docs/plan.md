---
title: KaoAge SDK 技術計画
---

## 1. 目的
- SDDで定義したKaoAge SDK仕様を実装可能な単位へ分解し、モジュール構成・依存技術・公開API・データモデル・テストおよびCI/リリース方針を明確化する。
- Kotlin実装とJava互換APIを両立し、Android 10+ 端末でオンデバイス推論を完結させる。

## 2. 全体アーキテクチャ
- **層構造**
  - `sdk-core`: 画像前処理 → ML Kitによる顔検出 → TFLite Interpreterで年齢/性別推定 → 結果整形（`Parcelable` + `toJson()`）。
  - `sdk-bestshot`: `sdk-core` の結果を入力として品質判定ロジックを提供。
  - `samples/cashier-app`: CameraXもしくは既存カメラAPIで取得したBitmapを解析し、JSONをUI表示。
  - `scripts`: モデル取得・ハッシュ検証・性能計測補助。
  - `docs`: 導入・API・JSONスキーマ・モデルカード・プライバシーなどのドキュメント群。
- **データフロー**
  1. アプリが `KaoAgeRequest` にBitmapと撮影メタデータをセット。
  2. `KaoAge` シングルトンが`KaoAgeConfig`に基づき前処理と推論を実施。
  3. `KaoAgeResult` を返却（成功時/失敗時問わず構造化）。`toJson()` で返却スキーマに変換。
  4. BestShotは任意で `KaoAgeResult` を受け取り、`BestShotDecision` を返す。

## 3. モジュール別詳細
### 3.1 sdk-core
- パッケージ構造指針
  - `com.kaoage.core.api`: Config/Request/Result/Callbackなど公開API。
  - `com.kaoage.core.internal.detector`: ML Kitラッパー、入力フォーマット変換。
  - `com.kaoage.core.internal.inference`: TFLite Interpreter管理、Delegate切替、SHA256検証付モデルローダ。
  - `com.kaoage.core.internal.processing`: 座標補正、スコア計算、正規化座標生成。
  - `com.kaoage.core.internal.metrics`: 計測、ログ出力（PIIなし）。
  - `com.kaoage.core.json`: JSONシリアライズ/デシリアライズユーティリティ。
- 公開APIは `KaoAge` オブジェクト経由で統一し、Kotlin/Java両対応にする。

### 3.2 sdk-bestshot
- `BestShotAdvisor` が `evaluateResult(result: KaoAgeResult): BestShotDecision` と `evaluateFrame(request, result)` を提供。
- `BestShotDecision`: `score: Float`, `ready: Boolean`, `reasons: List<String>`、`Parcelable` + `toJson()`.
- 設定値（角度閾値・スコア閾値）は `BestShotConfig` で外部調整可能。

### 3.3 samples/cashier-app
- Kotlin Activity（Coroutineベース）とJava Activity（Callbackベース）の両方を収録。
- Camera取得は簡易化し、既存写真読み込み + 任意のBitmap解析でMVP達成。
- 推論結果JSONとメトリクスを画面表示し、BestShot結果も併記。

### 3.4 scripts
- `download_models.sh`: 依存モデルの取得、SHA256検証、保存先（`models/`）作成。
- `verify_sha256.sh`: 任意モデルのハッシュ検証を行う単体スクリプト。
- 将来的な性能計測用 `perf_bench.sh` を追加検討（バックログ）。

## 4. 技術選定と依存
- 顔検出: Google ML Kit Face Detection（Play Services依存のないオンデバイス版）。
- 年齢/性別推定: TensorFlow Lite Interpreter。サポートDelegate＝CPU/GPU/NNAPI（設定で切替）。
- JSON: `kotlinx.serialization` を全モジュールに採用。未知フィールドは `ignoreUnknownKeys = true` で無視。
- 並列制御: Kotlin Coroutines + `Dispatchers.IO`。TFLite Interpreterは専用スレッドプールまたはインスタンスプールを整備。
- ログ: Android `Log` ではなく `Logger` ラッパーを実装し、ビルドフラグで有効/無効を切替。
- 最小SDK: minSdk 29 (Android 10)、targetSdk 最新安定版。

## 5. 公開API設計
```kotlin
object KaoAge {
    @JvmStatic fun initialize(config: KaoAgeConfig)
    @JvmStatic fun initialize(context: Context): Unit // @JvmOverloads でConfig省略版
    @JvmStatic suspend fun analyze(request: KaoAgeRequest): KaoAgeResult
    @JvmStatic fun analyzeAsync(request: KaoAgeRequest, callback: KaoAgeCallback)
    @JvmStatic fun warmUp()
    @JvmStatic fun shutdown()
}
```

```kotlin
data class KaoAgeConfig @JvmOverloads constructor(
    val ageModel: ModelSource,
    val genderModel: ModelSource,
    val expectedAgeSha256: String? = null,
    val expectedGenderSha256: String? = null,
    val delegate: KaoAgeDelegate = KaoAgeDelegate.NnApi,
    val numThreads: Int = DEFAULT_THREADS,
    val returnNormalizedCoordinates: Boolean = false,
    val metricsEnabled: Boolean = true,
    val requestTimeoutMs: Long = DEFAULT_TIMEOUT_MS
) : Parcelable
```

```kotlin
data class KaoAgeRequest @JvmOverloads constructor(
    val bitmap: Bitmap,
    val imageRotationDeg: Int = 0,
    val cameraFacing: KaoAgeCameraFacing = KaoAgeCameraFacing.BACK,
    val requestId: String = UUID.randomUUID().toString(),
    val timestampMs: Long = System.currentTimeMillis()
) : Parcelable
```

```kotlin
data class KaoAgeResult(
    val version: String,
    val status: KaoAgeStatus,
    val image: ImageInfo,
    val faces: List<FaceInfo>,
    val quality: QualityInfo,
    val metrics: KaoAgeMetrics
) : Parcelable {
    fun toJson(pretty: Boolean = false): String
}
```

- Java互換: `KaoAgeCallback`（`onSuccess`, `onError`）をinterfaceで提供。`@JvmStatic`, `@JvmOverloads`, `@JvmName` を適宜付与。
- `ModelSource`: `sealed class`（File/ByteBuffer/AssetFileDescriptor）。Java向けにはファクトリメソッドを提供。

## 6. データモデル & バリデーション
- `gender` は confidence が設定閾値未満の場合 `"unknown"` にマップ。
- `confidence` は 0.0..1.0 にクリップ。年齢値は 0..120 にクリップし、異常値は `notes` に追記。
- 座標: 画像座標(px)を基本として `returnNormalizedCoordinates=true` の場合に追加フィールドを返却。
- JSONスキーマは `/docs/json-schema.md` に定義し、`ResultSchemaTest` で `toJson()` の構造を検証。

## 7. 推論パイプライン
1. `KaoAge.initialize(config)` でモデルロード・Delegate初期化・ウォームアップ（ダミーラン）。
2. `analyze` 呼び出しで、入力BitmapをYUV/RGB変換 → 回転・ミラー補正。
3. ML Kitで顔検出 → バウンディングボックスとランドマーク取得。
4. 各顔について224x224にクロップ＆リサイズ → Age/Gender TFLite推論。
5. 結果を`FaceInfo`へマッピングし、`landmarks_present` を算出。
6. ステータス判定 (`SUCCESS`/`NO_FACE`/etc) → `KaoAgeResult` を構築。
7. メトリクス収集（処理時間、Delegate、推論回数）を `KaoAgeMetrics` に格納。

## 8. 並列性・リソース管理
- `InterpreterPool` を実装し、最大同時推論数を `KaoAgeConfig.maxParallelism`（拡張項目）で制御。
- `DispatcherProvider` をDI可能にし、テストでは`StandardTestDispatcher`に差し替え。
- `analyzeAsync` は `CoroutineScope`（内部でSupervisorJob）で実行。Javaコールバックはメインスレッドへ戻す。
- `shutdown()` でInterpreterとML Kitリソースを開放。

## 9. エラー処理 & リカバリ
- `KaoAgeStatus` enum: `SUCCESS`, `NO_FACE`, `PROCESSING_ERROR`, `UNSUPPORTED_FORMAT`, `MODEL_NOT_LOADED`, `TIMEOUT`.
- 内部例外は捕捉し、`PROCESSING_ERROR` に変換。必要に応じて `notes` に要因コードを追加。
- Delegate初期化失敗時はCPUへフォールバックし、`metrics` に `delegateFallback=true` を記録。
- モデルSHA256不一致は `MODEL_NOT_LOADED` とし、`notes` に詳細を記載。

## 10. パフォーマンス方針
- Pixel 7 / 224x224 / int8 / NNAPI で p50 ≤ 100ms を目標。`performance.md` に測定手順を書き、`scripts/perf_bench.sh`（将来）で自動計測。
- 初回ウォームアップを `initialize()` または `warmUp()` オプションで実施。
- `numThreads` と Delegate設定をメトリクスに含め、ログ分析しやすくする。
- メモリリーク防止のため、Bitmap/ByteBufferは処理後にリサイクル or 明示的にクリア。

## 11. テスト戦略
- **ユニット (JUnit)**: JSONシリアライズ、座標補正、ステータス遷移、SHA256検証、ModelSourceファクトリ。
- **Robolectric**: 画像回転/ミラー補正、0/1/多顔処理、Delegate設定反映、BestShot判定。
- **モック推論**: 実機モデル無しで動作する`MockInterpreter`を用意し、推論値を決定論的に設定。
- **ベンチテスト（任意）**: 実端末でのレイテンシ測定スクリプト＋手順書。
- **CIカバレッジ目標**: 80%以上（jacocoレポート）。
- テストデータは匿名サンプル画像（ぼかし/生成）を使用し、PII排除。

## 12. CI / 品質ゲート
- GitHub Actions ワークフロー
  - `./gradlew ktlintFormat detekt lintDebug testDebugUnitTest`
  - Robolectricテストを含む`testDebugUnitTest`を実行。
  - `scripts/verify_sha256.sh` をCIで走らせ、モデルの改ざん検知。
  - SBOM/ライセンスチェック: Gradle License Plugin または手動スクリプト。
- 失敗したジョブがある場合はリリース不可。mainブランチへのマージはPR経由で必須。

## 13. リリース方針
- バージョニング: `MAJOR.MINOR.PATCH`。初回リリースは `0.1.0` でプレリリース扱い。
- 成果物: `/sdk-core` および `/sdk-bestshot` のAAR生成。`samples` はAPKではなくソース配布。
- GitHub ReleasesにAARとSHA256ハッシュ、`docs/performance.md` の計測結果、SBOMを添付。
- 破壊的変更はメジャーアップデートで告知。`version` フィールドをJSONに保持し後方互換。

## 14. セキュリティ & プライバシー対策
- 画像・推論結果を永続化しないことをコードレビュー/テストで担保。
- ネットワークアクセス禁止：依存ライブラリでも外部送信がないか確認。CIで`lint`チェック。
- ログレベル制御: デフォルトINFOでPIIゼロ。デバッグ用ログはビルドフラグで無効化可能。
- `/docs/privacy.md` にデータライフサイクルを記載し、利用者責任範囲を明示。

## 15. ドキュメント整備
- `quickstart.md`: SDK導入手順、Kotlin/Javaコード例、モデル取得手順。
- `api.md`: 公開APIの詳細、Config項目、Callback仕様。
- `json-schema.md`: 結果JSONの正式スキーマ（draft-07相当）。
- `model-card.md`: 年齢/性別モデルの概要、評価データ、制約。
- `privacy.md`: データ取り扱いポリシー、推奨運用。
- `licenses.md`: ML Kit/TFLiteなどのライセンス表記。
- `performance.md`: 測定条件・結果・再現手順。

## 16. リスクと軽減策
- **Delegate非対応端末**: CPUフォールバック＋テストケース整備。
- **モデル更新によるズレ**: SHA256検証とバージョニングで管理。`download_models.sh` で固定URL + ハッシュ更新フロー。
- **顔ランドマーク未検出**: `landmarks_present=false` を返却し、BestShotでリトライ推奨。
- **JSON互換性劣化**: スキーマテストと`ignoreUnknownKeys`で後方互換を確保。
- **マルチスレッド競合**: InterpreterPoolの同期テスト、`@ThreadSafe` ドキュメントで利用側に注意喚起。

## 17. 実装ロードマップ（目安）
1. プロジェクトセットアップ（Gradle, モジュール生成, CI骨格）。
2. `sdk-core` 基盤：Config/Request/Resultモデル + JSONユーティリティ + テスト。
3. ML Kitラッパーと座標補正ユーティリティの実装。
4. TFLite推論パイプラインとモデルロード（モック→実機）。
5. `KaoAge` API統合（Coroutine＋Callback）とエラーハンドリング。
6. `sdk-bestshot` 実装とテスト。
7. サンプルアプリ整備（Kotlin/Javaフロー）。
8. スクリプト・ドキュメント整備。
9. パフォーマンス検証 & チューニング。
10. リリース準備（AAR生成、ドキュメント最終化）。

## 18. チェックリスト
- [ ] Kotlin/Java向けAPIのAPIレビュー完了。
- [ ] JSONスキーマと実装の整合テスト成功。
- [ ] Delegate切替テスト（CPU/GPU/NNAPI）の自動化。
- [ ] セキュリティ/プライバシーガイドのドラフト作成。
- [ ] `download_models.sh` のライセンス確認・利用規約順守チェック。
- [ ] パフォーマンス測定ログの保管場所（安全なローカル/社内共有）を決定。

