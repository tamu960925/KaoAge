# Face Insights SDK アーキテクチャ

## 全体像

Face Insights SDK は「カメラ制御はホストアプリが担当し、解析のみを SDK が受け持つ」という分離を徹底しています。アプリは `ImageProxy` を提供し、SDK は ML Kit による顔検出と TensorFlow Lite による年齢・性別推論をオンデバイスで実行します。解析結果は構造化された `FaceInsightsResult` と `BestShotSignal` として返却され、生画像は保存されません。

```
CameraX -> ImageProxy -> FaceInsightsSession / FaceInsightsAnalyzer
        -> ML Kit Face Detection -> DetectionResult
        -> AgeGenderEngine -> AgeGenderEstimate
        -> Result Merge -> FaceInsightsResult -> (BestShotBridge) -> BestShotSignal
```

## コンポーネント層

1. **ホストアプリ**  
   CameraX などでカメラデバイスを管理し、`ImageAnalysis` のコールバックから `ImageProxy` を取得します。フレームのクローズ (`imageProxy.close()`) や UI 反映もアプリ側で行います。

2. **検出レイヤー (`sdk-core:detection`)**  
   - `FaceDetector` インターフェースを通じて顔検出を抽象化。標準実装は `MlKitFaceAnalyzer`。  
   - `DetectionSessionConfig` に基づき、顔サイズや信頼度をフィルタリングします。  
   - `DetectionResult` にはバウンディングボックス、オイラー角、ランドマーク、信頼度が含まれます。

3. **推論レイヤー (`sdk-core:inference`)**  
   - `AgeGenderEngine` が TensorFlow Lite モデルで年齢・性別を推論。  
   - モデルが存在しない場合はヒューリスティックで安全側の結果 (`UNDETERMINED` 等) を返します。  
   - 結果は `AgeGenderEstimate` として、年齢帯・信頼度・性別ラベルを保持します。

4. **セッション統合 (`sdk-core:session`)**  
   - `FaceInsightsSession` が検出と推論の結果をマージし、`FaceInsightsResult` を生成します。  
   - `BestShotBridge` を介して、検出情報を `sdk-bestshot` のロジックに流し込みます。  
   - API を簡潔に利用したいケースでは `FaceInsightsAnalyzer` が上記をラップしています。

5. **ベストショット (`sdk-bestshot`)**  
   - `BestShotEngine` が検出信頼度・クールダウンを評価し、`BestShotSignal` を返します。  
   - フレームが閾値に達しない場合は `eligible = false` を返し、UI 側で再撮影を促します。

## データモデル

- `BoundingBox` / `EulerAngles` / `NamedLandmark` : 顔の位置・姿勢・ランドマーク集合。  
- `FaceInsightsResult` : 検出 ID、タイムスタンプ、推定年齢帯・性別、ランドマーク有無、ベストショット候補などを保持。`Parcelable` と `toJson()` に対応。  
- `BestShotSignal` : ベストショットのトリガー、クールダウン、品質スコアを返します。

## 非機能要件への対応

- **プライバシー** : 画像は SDK によって保存されず、コールバック終了時にホストが破棄します。  
- **パフォーマンス** : 500 ms 以内の処理を目標にコルーチン/CameraX バックプレッシャー戦略を採用。  
- **Java フレンドリー** : `@JvmStatic` / `@JvmOverloads` を付与し、Java プロジェクトでも `FaceInsightsAnalyzer` や `DetectionConfigBuilder` を直接利用できます。

## サンプルアプリとの関係

`samples/cashier-app` は上記レイヤーを組み合わせ、以下を実践的に示します。

- CameraX を用いたカメラ管理 (`MainActivity`)  
- `FaceInsightsAnalyzer` と `BestShotEvaluator` を使ったリアルタイム UI 更新  
- `FaceInsightsResult.toJson()` でのオーバーレイ表示  
- 自己レジ/対面レジの両シナリオにおけるガイダンス

この構造をベースに、自社アプリでは必要な UI やログの形にカスタマイズしてください。

## 追加リソース

- `docs/sdk/developer-guide.md` : セットアップから高度な統合までの実装手順
- `docs/sdk/camera-handoff.md` : カメラ制御責務の詳細
- `docs/sdk/model-assets.md` : TFLite モデルの取得・検証フロー
