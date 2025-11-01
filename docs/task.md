# Tasks: KaoAge SDK

**Input**: Design documents from `/docs/`
**Prerequisites**: plan.md (available), spec.md (not provided – user stories derived from SDD)

**Tests**: TDD方針に従い、各ユーザーストーリーでは RED → GREEN → REFACTOR を明示しています。

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: プロジェクト雛形とCI構築

- [ ] T001 Create Gradle project skeleton in `settings.gradle.kts` and module directories (`sdk-core`, `sdk-bestshot`, `samples/cashier-app`, `docs`, `scripts`)
- [ ] T002 Configure root Gradle build in `build.gradle.kts` with Kotlin, Android, ktlint, detekt plugins
- [ ] T003 Add GitHub Actions workflow in `.github/workflows/android-ci.yml` running `./gradlew ktlintCheck detekt test`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: 全ストーリーで共有する基盤を整える

- [ ] T004 Define Android library setup and dependencies in `sdk-core/build.gradle.kts`
- [ ] T005 Configure lint/format settings in `config/ktlint/.editorconfig` and `config/detekt/detekt.yml`
- [ ] T006 Add Robolectric and kotlinx.serialization test dependencies in `sdk-core/build.gradle.kts` with failing placeholder test classes
- [ ] T007 Stub build scripts for `sdk-bestshot/build.gradle.kts` and `samples/cashier-app/build.gradle.kts`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - コア解析APIでBitmapからJSONレスポンスを返す (Priority: P1) 🎯 MVP

**Goal**: `KaoAge.analyze()` がBitmap入力から顔検出・推論・JSON整形までを完遂し、`KaoAgeResult` を返す  
**Independent Test**: RobolectricでBitmapを入力し、0/1/多顔ケースの結果JSONが期待どおり出力される

### Tests for User Story 1 (RED)

- [ ] T008 [P] [US1] Author failing serialization tests in `sdk-core/src/test/kotlin/com/kaoage/core/json/KaoAgeResultJsonTest.kt` covering toJson() と未知フィールド無視
- [ ] T009 [P] [US1] Add failing rotation/mirror golden tests in `sdk-core/src/test/kotlin/com/kaoage/core/processing/ImageTransformTest.kt` comparing against `sdk-core/src/test/resources/golden/transform_cases.csv`
- [ ] T010 [P] [US1] Create failing ML Kit wrapper tests in `sdk-core/src/test/kotlin/com/kaoage/core/detector/FaceDetectorTest.kt` for 0/1/多顔ケース
- [ ] T011 [P] [US1] Add failing inference pipeline tests in `sdk-core/src/test/kotlin/com/kaoage/core/inference/AgeGenderPipelineTest.kt` with SHA256 mismatch scenarios
- [ ] T012 [P] [US1] Write failing integration tests in `sdk-core/src/test/kotlin/com/kaoage/core/api/KaoAgeApiTest.kt` for suspend and callback analyze flows

### Implementation for User Story 1 (GREEN → REFACTOR)

- [ ] T013 [US1] Implement Config/Request/Result models and serialization in `sdk-core/src/main/kotlin/com/kaoage/core/api/model`
- [ ] T014 [US1] Implement rotation/mirror utilities in `sdk-core/src/main/kotlin/com/kaoage/core/processing/ImageTransform.kt`
- [ ] T015 [US1] Implement ML Kit face detector wrapper in `sdk-core/src/main/kotlin/com/kaoage/core/detector/FaceDetector.kt`
- [ ] T016 [US1] Implement ModelSource + TFLite inference pipeline in `sdk-core/src/main/kotlin/com/kaoage/core/inference/AgeGenderPipeline.kt`
- [ ] T017 [US1] Implement `KaoAge` facade (initialize/analyze/analyzeAsync) in `sdk-core/src/main/kotlin/com/kaoage/core/api/KaoAge.kt`

**Checkpoint**: User Story 1 functional — Bitmap解析結果をJSONで返すことを確認

---

## Phase 4: User Story 2 - パフォーマンス制御とメトリクスを提供する (Priority: P2)

**Goal**: Interpreterプール・タイムアウト制御・Delegate設定とメトリクス収集を提供する  
**Independent Test**: 並列呼び出しとメトリクスON/OFFを切り替え、正しいdelegate/latencyが計測される

### Tests for User Story 2 (RED)

- [ ] T018 [P] [US2] Add failing concurrency tests in `sdk-core/src/test/kotlin/com/kaoage/core/inference/InterpreterPoolTest.kt` covering maxParallelismとTIMEOUT
- [ ] T019 [P] [US2] Create failing metrics toggling tests in `sdk-core/src/test/kotlin/com/kaoage/core/metrics/KaoAgeMetricsTest.kt`

### Implementation for User Story 2 (GREEN → REFACTOR)

- [ ] T020 [US2] Implement InterpreterPool with timeout handling in `sdk-core/src/main/kotlin/com/kaoage/core/inference/InterpreterPool.kt`
- [ ] T021 [US2] Implement KaoAgeMetrics recorder and config flags in `sdk-core/src/main/kotlin/com/kaoage/core/metrics/KaoAgeMetrics.kt`

**Checkpoint**: User Story 2 functional — 並列制御とメトリクスが期待通り動作

---

## Phase 5: User Story 3 - BestShot評価ロジックを提供する (Priority: P2)

**Goal**: 顔スコアと姿勢角から撮影品質を判定し、撮り直し指標を返す  
**Independent Test**: サンプル結果でBestShotAdvisorがscore/ready/reasonsを一貫して返す

### Tests for User Story 3 (RED)

- [ ] T022 [P] [US3] Add failing evaluation tests in `sdk-bestshot/src/test/kotlin/com/kaoage/bestshot/BestShotAdvisorTest.kt` for score/ready判定
- [ ] T023 [P] [US3] Create failing hysteresis tests in `sdk-bestshot/src/test/kotlin/com/kaoage/bestshot/BestShotFrameEvaluatorTest.kt`

### Implementation for User Story 3 (GREEN → REFACTOR)

- [ ] T024 [US3] Implement BestShotAdvisor and config in `sdk-bestshot/src/main/kotlin/com/kaoage/bestshot/BestShotAdvisor.kt`
- [ ] T025 [US3] Implement frame-based evaluator with hysteresis in `sdk-bestshot/src/main/kotlin/com/kaoage/bestshot/BestShotFrameEvaluator.kt`

**Checkpoint**: User Story 3 functional — BestShot結果が独立に検証可能

---

## Phase 6: User Story 4 - サンプルアプリでKotlin/Java利用例を提供する (Priority: P3)

**Goal**: Kotlin/Java UIがBitmap解析・BestShot表示を行いJSONを表示する  
**Independent Test**: RobolectricでKotlin/Javaの各フローが成功し、UIに結果が表示される

### Tests for User Story 4 (RED)

- [ ] T026 [P] [US4] Add failing Robolectric test for Kotlin flow in `samples/cashier-app/src/test/kotlin/com/kaoage/sample/KotlinAnalyzeActivityTest.kt`
- [ ] T028 [P] [US4] Add failing Robolectric test for Java callback flow in `samples/cashier-app/src/test/kotlin/com/kaoage/sample/JavaAnalyzeActivityTest.kt`

### Implementation for User Story 4 (GREEN → REFACTOR)

- [ ] T027 [US4] Implement Kotlin activity and UI in `samples/cashier-app/src/main/kotlin/com/kaoage/sample/ui/KotlinAnalyzeActivity.kt`
- [ ] T029 [US4] Implement Java activity and callback integration in `samples/cashier-app/src/main/java/com/kaoage/sample/ui/JavaAnalyzeActivity.java`
- [ ] T030 [US4] Document usage in `samples/cashier-app/README.md`

**Checkpoint**: User Story 4 functional — Kotlin/Java双方のデモが成立

---

## Phase 7: User Story 5 - スクリプトとドキュメントで運用準備を整える (Priority: P3)

**Goal**: モデルダウンロード/検証スクリプトとドキュメントを揃え、性能結果を共有する  
**Independent Test**: スクリプトがCIで成功し、ドキュメントに手順と性能値が記載される

### Tests for User Story 5 (RED)

- [ ] T031 [P] [US5] Add failing shell test in `scripts/tests/download_models_test.sh` validating download+hash flow
- [ ] T032 [P] [US5] Add failing shell test in `scripts/tests/verify_sha256_test.sh` for standalone verifier

### Implementation for User Story 5 (GREEN → REFACTOR)

- [ ] T033 [US5] Implement `scripts/download_models.sh` with SHA256 enforcement
- [ ] T034 [US5] Implement `scripts/verify_sha256.sh` and integrate with CI
- [ ] T035 [US5] Draft documentation skeletons in `docs/quickstart.md`, `docs/api.md`, `docs/json-schema.md`, `docs/privacy.md`, `docs/licenses.md`
- [ ] T036 [US5] Populate documentation content per SDD in the same docs files
- [ ] T037 [US5] Record performance measurements in `docs/performance.md`

**Checkpoint**: User Story 5 functional — 運用準備とドキュメントが整備

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: 品質向上と横断課題の整理

- [ ] T038 [P] Run formatting and static analysis fixes via `./gradlew ktlintFormat detekt` before release
- [ ] T039 Address lingering TODOs and inline comments across `sdk-core` と `sdk-bestshot`
- [ ] T040 [P] Final verification of CI and scripts in `.github/workflows/android-ci.yml` and `scripts/`
- [ ] T041 Update change log in `docs/changelog.md` (create if missing)

---

## Dependencies & Execution Order

1. **Phase 1 → Phase 2**: プロジェクト雛形と基盤構築が完了するとユーザーストーリー着手可能  
2. **User Story 1 (P1)**: 最優先。Bitmap解析とJSON整形を提供し、以降のストーリーの土台になる  
3. **User Story 2 (P2)**: US1完了後に着手。性能制御とメトリクスはUS1のAPIを拡張  
4. **User Story 3 (P2)**: US1の結果モデルに依存、US2とは疎結合  
5. **User Story 4 (P3)**: US1〜US3の成果を利用してデモを構築  
6. **User Story 5 (P3)**: スクリプトとドキュメントはUS1〜US4の出力に基づく  
7. **Phase 8**: すべての実装後に横断的な仕上げを実施

---

## Parallel Execution Examples

- **User Story 1**: T008〜T012 のREDテストは異なるテストファイルで並行作業可。GREEN側の T013〜T017 も異なるパッケージで実装できる。  
- **User Story 2**: T018 と T019 (テスト) を並行で進め、実装 T020/T021 は責務が分かれているため同時進行可。  
- **User Story 3**: T022 と T023 のテストは独立、BestShotAdvisorとFrameEvaluatorの実装 T024/T025 も並行化できる。  
- **User Story 4**: Kotlin/Javaそれぞれのテスト・実装（T026/T028 と T027/T029）は別担当で同時進行可。  
- **User Story 5**: スクリプト系 (T031〜T034) とドキュメント系 (T035〜T037) を分担して並行化。

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. 完了済みの Phase 1〜2 を前提に、User Story 1 のREDテスト (T008〜T012) を先に書く  
2. GREENタスク (T013〜T017) を実装してテストを通す  
3. `samples` や `bestshot` への統合前に JSON出力が正しいことを検証  
4. MVPとして顧客へデモ可能

### Incremental Delivery

1. User Story 1 完了 → 基礎的な解析SDKを提供  
2. User Story 2 完了 → 並列実行とメトリクスで運用性向上  
3. User Story 3 完了 → BestShotによる撮り直し指標を追加  
4. User Story 4 完了 → サンプルアプリで統合例を提示  
5. User Story 5 完了 → スクリプト・ドキュメント・性能情報でリリース準備

### Parallel Team Strategy

1. 共通基盤 (Phase 1–2) をチーム全員で早期に整備  
2. US1完了後、US2〜US5 を担当者ごとに振り分けて並列開発  
3. 主要なインターフェース (`KaoAgeResult`, `KaoAgeConfig`, `BestShotDecision`) はAPIレビュー後に固定し、相互依存を最小化  
4. 各ストーリー完了時に独立テストを実施してリグレッションを防止
