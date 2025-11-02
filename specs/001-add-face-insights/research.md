# Research Log – Face Insights SDK

## Kotlin & Build Toolchain
- **Decision**: Target Kotlin 1.9.24 with Android Gradle Plugin 8.6.0 and JVM toolchain 17 for all SDK modules.
- **Rationale**: Kotlin 1.9.24 remains the long-term supported channel in late 2025, delivers stable K2 compiler improvements, and pairs with AGP 8.6.0 for Android 10+ desugaring while avoiding Kotlin 2.0 migration risk during initial SDK delivery.
- **Alternatives considered**:
  - Kotlin 2.0.x + AGP 8.7: Declined due to compiler migration overhead and potential kapt regressions with ML Kit’s annotations.
  - Kotlin 1.8.x + AGP 8.3: Declined because it lacks the latest performance optimizations and structured concurrency fixes needed for real-time inference.

## JSON Serialization Strategy
- **Decision**: Use `kotlinx.serialization` with JSON format for `toJson()` implementations across parcelable result models.
- **Rationale**: Built-in to Kotlin ecosystem, works in multiplatform contexts, avoids extra reflection-based dependencies, and supports deterministic schema definitions that align with Java callers via generated adapters.
- **Alternatives considered**:
  - Moshi: Provides mature JSON handling but adds reflection or codegen complexity and extra dependency reviews.
  - Gson: Broad adoption yet slower, reflection-heavy, and lacks null-safety guarantees expected for SDK consumers.

## Reference Hardware for Performance Targets
- **Decision**: Validate latency and CPU metrics on Google Pixel 6 (Tensor G1) and Pixel 4a (Snapdragon 730G) as representative modern and mid-tier Android 10+ devices.
- **Rationale**: Pixel 6 mirrors enterprise device refreshes with Tensor optimizations, while Pixel 4a aligns with cost-sensitive deployments still within support windows, ensuring targets cover realistic adoption curves.
- **Alternatives considered**:
  - Pixel 8 Pro only: Too high-end, risks missing mid-tier constraints.
  - Moto G Power: Popular but lacks ML acceleration; would overly constrain targets for first release.

## ML Kit Face Detection Configuration
- **Decision**: Configure ML Kit Face Detection with `PERFORMANCE_MODE_ACCURATE`, `LANDMARK_MODE_ALL`, `CLASSIFICATION_MODE_ALL`, enable face tracking IDs for BestShot scoring, and set `minFaceSize` to 0.15f of frame height.
- **Rationale**: Accurate mode prioritizes consistent landmarks essential for age/gender inference; landmark and classification modes expose required features; tracking IDs help smooth predictions across frames without persisting imagery.
- **Alternatives considered**:
  - Fast performance mode: Lower latency but sacrifices landmark fidelity needed for downstream models.
  - Disabling tracking: Simplifies configuration but reduces BestShot continuity and stability of demographic estimates.

## TDD Workflow Guardrails (t-wada style)
- **Decision**: Adopt micro-cycle TDD with “Write a failing test → make it pass minimally → refactor” enforced through Robolectric/Instrumentation suites and golden JSON snapshots for result serialization.
- **Rationale**: Aligns with t-wada’s emphasis on driving design via tests, using precise failure messages and keeping production changes scoped to the minimal diff required, ensuring observable regression safety.
- **Alternatives considered**:
  - High-level integration tests first: Slower feedback and obscures failure causes for ML pipelines.
  - Manual QA driven verification: Violates constitution’s test-first mandate and delays regression detection.
