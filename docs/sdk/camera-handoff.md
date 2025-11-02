# カメラハンドオフ契約

Face Insights SDK はカメラデバイスを直接制御しません。ホストアプリが CameraX などでフレームを取得し、解析対象として SDK に委譲する際の責務分担を以下にまとめます。

## ホストアプリの責務

1. **カメラ権限とライフサイクルの管理**  
   - カメラ権限のリクエスト、`ProcessCameraProvider` の生成、`bindToLifecycle` の呼び出しをアプリで行います。  
   - 実装例は `samples/cashier-app/MainActivity.kt` を参照してください。

2. **フレーム生成と整形**  
   - `ImageAnalysis` または独自パイプラインで `ImageProxy` を取得し、回転情報 (`imageInfo.rotationDegrees`) を保持したまま SDK に渡します。  
   - 必要に応じて、複数カメラ（前面・背面）をトライし、利用不可の場合は UI で案内します。

3. **ImageProxy のクローズ**  
   - `FaceInsightsAnalyzer.analyze` / `FaceInsightsSession.analyze` はフレーム所有権を持ちません。処理完了後に必ず `imageProxy.close()` を呼び出してください。  
   - 例外発生時も漏れなくクローズするため、`try/finally` ブロックを推奨します。

4. **スレッドとバックプレッシャー管理**  
   - 解析はコルーチンまたはワーカースレッドで実行し、UI スレッドをブロックしないようにします。  
   - `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` を利用すると、処理待ちフレームが蓄積して遅延することを防げます。

## SDK の責務

- 受け取った `ImageProxy` から ML Kit 向けに `InputImage` を構築し、検出/推論を実行します。  
- 解析結果が利用できる場合は `FaceInsightsResult`、`BestShotSignal`（オプション）を返し、利用できない場合は `null` を返します。  
- 画像データを保持せず、結果はすべてメモリ内で完結します。

## ハンドオフの標準フロー

1. `ImageProxy` 取得 (`analyze(imageProxy)` 呼び出し)  
2. `FaceInsightsAnalyzer.analyze` または `FaceInsightsSession.analyze` をサスペンド呼び出し  
3. 結果を UI/ロジックに反映  
4. `imageProxy.close()` でフレーム破棄  
5. 次フレームを待機

```
CameraX Analyzer
    -> obtain ImageProxy
    -> launch coroutine { sdk.analyze(...) }
    <- FaceInsightsResult? / BestShotSignal?
    -> imageProxy.close()
```

## ベストプラクティス

- **低照度やブラーへの対応**  
  - `result.bestShotEligible == false` の場合は「カメラを近づけてください」などのガイダンスを表示し、再撮影を促します。
- **回転の扱い**  
  - ML Kit は入力画像の回転を自動補正します。`InputImage.fromMediaImage(mediaImage, rotationDegrees)` を必ず使用してください。
- **メモリ管理**  
  - 大きなフレームを高速に処理するため、解析スレッドの数を制限し、GPU/CPU の競合を避けます。
- **例外ハンドリング**  
  - モデルが存在しない場合や推論が失敗した場合、SDK は `null` や低信頼度を返します。UI 側で例外を飲み込みつつ適切に案内してください。

## 参考実装

`samples/cashier-app` では以下を確認できます。

- CameraX セットアップと複数カメラのフォールバック
- `AtomicBoolean` による同時解析の制御
- `FaceInsightsAnalyzer` と `BestShotEvaluator` の組み合わせ
- UI スレッドへの `withContext(Dispatchers.Main)` での結果反映

これらをベースに、ホストアプリの要件に合わせたハンドオフ契約を実装してください。
