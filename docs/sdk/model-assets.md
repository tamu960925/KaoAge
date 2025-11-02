# モデルアセット運用ガイド

Face Insights SDK は年齢推定・性別分類をオンデバイスで実行するために、TensorFlow Lite モデルをアプリ資産として同梱します。本ガイドでは取得・検証・配布の流れとアプリ側での扱い方をまとめます。

## 1. モデルの取得

1. リポジトリ直下で `scripts/download_models.sh` を実行します。
   ```bash
   scripts/download_models.sh
   ```
2. スクリプトは MIT ライセンスで公開されている [shubham0204/Age-Gender_Estimation_TF-Android](https://github.com/shubham0204/Age-Gender_Estimation_TF-Android) から以下のファイルをダウンロードします。
   - `model_age_nonq.tflite`
   - `model_gender_nonq.tflite`
3. 作成された `models/` ディレクトリは `.gitignore` 済みです。バイナリはリポジトリにコミットしないでください。

## 2. チェックサムとライセンス確認

- 取得したファイルの SHA-256 が `docs/compliance/dependency-register.md` に記載された値と一致することを確認してください。
- モデルの出典・バージョン・ライセンスが変更になった場合は、同ファイルの表を更新します。

## 3. ビルド/配布への組み込み

- `sdk-core/build.gradle.kts` の `sourceSets["main"].assets` により `models/` ディレクトリが Asset として APK に同梱されます。
- `preBuild` タスクでモデル存在チェック (`verifyAgeModel`, `verifyGenderModel`) を行い、欠落時はビルドログに警告を表示します。
- テストや CI でも `scripts/download_models.sh` を実行してからビルドするようにしてください。

## 4. ランタイムでの利用

`ModelAssetManager` がモデルの読み込み・存在確認・キャッシュコピーを提供します。

```kotlin
val ageBuffer = ModelAssetManager.loadAgeModelByteBuffer(context)
val genderBuffer = ModelAssetManager.loadGenderModelByteBuffer(context)
val engine = AgeGenderEngine(ageModel = ageBuffer, genderModel = genderBuffer)
```

- モデルが存在しない場合、`AgeGenderEngine` はフェイルセーフに動作し、`UNDETERMINED` や低信頼度で結果を返します。
- 永続化や外部ストレージへのコピーは禁止です。必要に応じて `copyAgeModelToCache` でアプリ専用キャッシュ領域に複製します。

## 5. セキュリティとプライバシーポリシー

- モデルファイルは開発者マシンおよびアプリ資産内に限定し、共有ストレージやネットワーク転送は行わないでください。
- モデル更新時は必ずライセンスリビューと SHA-256 更新を実施し、監査ログを保持します。
- SDK は画像データを保持せず、モデルもオンデバイスでのみ使用されます。

## 6. トラブルシューティング

| 症状 | 原因 | 対処 |
|------|------|------|
| ログで「model_age_nonq.tflite missing」と表示される | `scripts/download_models.sh` 未実行 | スクリプト実行後に再ビルド |
| 年齢/性別が常に `UNDETERMINED` になる | モデル未配置または破損 | `models/` を再取得し、チェックサムを照合 |
| ビルド後にアプリがクラッシュする | `ModelAssetManager` がファイルを開けない | アセット同梱設定と `preBuild` 依存関係を確認 |

モデル管理はコンプライアンスに直結します。変更が発生した場合は必ず関係者へ通知し、本ガイドと依存リストの更新を行ってください。
