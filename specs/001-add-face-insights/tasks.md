# Tasks: Face Insights SDK

**Input**: Design documents from `/specs/001-add-face-insights/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD is mandatory. Add failing JUnit/Robolectric tests before any production code.

**Organization**: Group tasks by user story so each slice stays independently testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions
- Use Kotlin paths (`sdk-core/src/main/kotlin/...`, `samples/cashier-app/src/main/java|kotlin/...`)

## Path Conventions

- `sdk-core/` — ML Kit face analysis and age/gender inference primitives
- `sdk-bestshot/` — BestShot heuristics layered on core analysis
- `samples/cashier-app/` — Host-controlled camera sample integrating the SDK
- `docs/` — Architecture notes, compliance records
- Paths shown below assume this core structure; expand only with documented justification

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and baseline tooling

- [X] T001 Align Kotlin 1.9.24, AGP 8.6.0, and JDK 17 toolchains in `/Users/tamurayousuke/work/KaoAge/gradle/libs.versions.toml`
- [X] T002 Enable `kotlin-parcelize` and `kotlinx-serialization` plugins for SDK modules in `/Users/tamurayousuke/work/KaoAge/sdk-core/build.gradle.kts` and `/Users/tamurayousuke/work/KaoAge/sdk-bestshot/build.gradle.kts`
- [X] T003 [P] Verify `scripts/download_models.sh` fetches sanctioned models and document checksum workflow in `/Users/tamurayousuke/work/KaoAge/docs/sdk/model-assets.md`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared infrastructure required before user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T004 Write failing unit tests for `FaceInsightsResult` parcelable + `toJson()` integrity in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/FaceInsightsResultTest.kt`
- [X] T005 Implement `FaceInsightsResult` and supporting data classes per data model in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/model/FaceInsightsResult.kt`
- [X] T006 Write failing unit tests for `DetectionSessionConfig` validation rules in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/DetectionSessionConfigTest.kt`
- [X] T007 Implement `DetectionSessionConfig` with validation and defaults in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/config/DetectionSessionConfig.kt`
- [X] T008 [P] Establish shared Robolectric test harness for CameraX frame injection in `/Users/tamurayousuke/work/KaoAge/samples/cashier-app/src/test/kotlin/com/kaoage/cashier/test/CameraFrameHarness.kt`
- [X] T009 Document host-camera ownership and frame handoff contract in `/Users/tamurayousuke/work/KaoAge/docs/sdk/camera-handoff.md`

**Checkpoint**: Foundation ready — user story implementation can now begin in parallel

---

## Phase 3: User Story 1 – Age-restricted checkout verification (Priority: P1) 🎯 MVP

**Goal**: Provide cashiers on-device age/gender insights with bounding boxes to accelerate compliance checks.

**Independent Test**: Run cashier flow in airplane mode; confirm SDK returns bounding box, age bracket, gender, and confidence within 500 ms for compliant captures.

### Tests for User Story 1 (author first, ensure failing)

- [X] T010 [US1] Write failing detection pipeline unit tests covering bounding box, orientation, age, and gender outputs in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/FaceInsightsPipelineTest.kt`
- [X] T011 [US1] Write failing Robolectric flow test ensuring cashier UI renders on-device analysis within latency target in `/Users/tamurayousuke/work/KaoAge/samples/cashier-app/src/test/kotlin/com/kaoage/cashier/ui/CashierFlowTest.kt`

### Implementation for User Story 1

- [X] T012 [US1] Integrate ML Kit Face Detection wrapper using accurate mode and tracking IDs in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/detection/MlKitFaceAnalyzer.kt`
- [X] T013 [US1] Implement age/gender inference pipeline loading on-device models via byte buffers in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/inference/AgeGenderEngine.kt`
- [X] T014 [US1] Compose `FaceInsightsSession` orchestrator to merge detection + inference results into parcelables in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/session/FaceInsightsSession.kt`
- [X] T015 [US1] Update cashier overlay to visualize boxes, demographics, and low-confidence prompts in `/Users/tamurayousuke/work/KaoAge/samples/cashier-app/src/main/kotlin/com/kaoage/cashier/ui/overlay/CashierOverlayViewModel.kt`
- [X] T016 [US1] Record compliance notes and cashier guidance in `/Users/tamurayousuke/work/KaoAge/docs/sdk/cashier-playbook.md`

**Checkpoint**: User Story 1 independently functional and testable (MVP scope)

---

## Phase 4: User Story 2 – Self-checkout privacy guardrails (Priority: P2)

**Goal**: Guarantee self-checkout flow stays offline, communicates confidence limits, and never persists imagery.

**Independent Test**: Operate self-checkout kiosk offline; verify BestShot hints respect privacy rules and low-confidence states trigger user prompts without storing frames.

### Tests for User Story 2 (author first, ensure failing)

- [X] T017 [US2] Write failing unit tests for BestShot eligibility and cooldown heuristics in `/Users/tamurayousuke/work/KaoAge/sdk-bestshot/src/test/kotlin/com/kaoage/sdk/bestshot/BestShotEngineTest.kt`
- [X] T018 [US2] Write failing Robolectric privacy test confirming no disk/network writes occur during self-checkout flow in `/Users/tamurayousuke/work/KaoAge/samples/cashier-app/src/test/kotlin/com/kaoage/cashier/privacy/SelfCheckoutPrivacyTest.kt`

### Implementation for User Story 2

- [X] T019 [US2] Implement BestShot scoring engine and triggers with configurable cooldowns in `/Users/tamurayousuke/work/KaoAge/sdk-bestshot/src/main/kotlin/com/kaoage/sdk/bestshot/BestShotEngine.kt`
- [X] T020 [US2] Surface BestShot callbacks and low-confidence guidance through SDK APIs in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/bestshot/BestShotBridge.kt`
- [X] T021 [US2] Update self-checkout UI prompts to explain offline processing and retry guidance in `/Users/tamurayousuke/work/KaoAge/samples/cashier-app/src/main/kotlin/com/kaoage/cashier/ui/selfcheckout/SelfCheckoutFragment.kt`
- [X] T022 [US2] Document privacy assurances and audit checklist in `/Users/tamurayousuke/work/KaoAge/docs/privacy/self-checkout.md`

**Checkpoint**: User Stories 1 and 2 independently functional with privacy guardrails proven offline

---

## Phase 5: User Story 3 – Developer integration and monitoring (Priority: P3)

**Goal**: Enable developers to configure thresholds, serialize outputs, and monitor SDK behavior rapidly.

**Independent Test**: Integrate SDK into fresh project template; confirm configuration builders adjust thresholds live and JSON payloads stream to logs without code changes.

### Tests for User Story 3 (author first, ensure failing)

- [X] T023 [US3] Write failing unit tests for configurable thresholds and runtime updates in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/config/DetectionConfigUpdateTest.kt`
- [X] T024 [US3] Write failing integration test ensuring `toJson()` snapshots feed developer telemetry logs in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/serialization/TelemetryExportTest.kt`

### Implementation for User Story 3

- [X] T025 [US3] Implement builder-style configuration API with `@JvmOverloads` bridge in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/config/DetectionConfigBuilder.kt`
- [X] T026 [US3] Add developer telemetry hooks and JSON export utilities in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/main/kotlin/com/kaoage/sdk/core/telemetry/TelemetryReporter.kt`
- [X] T027 [US3] Extend quickstart with integration walkthrough and logging samples in `/Users/tamurayousuke/work/KaoAge/specs/001-add-face-insights/quickstart.md`

**Checkpoint**: All user stories independently functional with developer-focused controls and observability

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality, performance, and documentation refinements spanning stories

- [X] T028 [P] Benchmark latency/CPU on Pixel 6 and Pixel 4a, logging results in `/Users/tamurayousuke/work/KaoAge/docs/sdk/performance-benchmarks.md`
- [X] T029 Harden error handling scenarios and add regression tests for edge cases in `/Users/tamurayousuke/work/KaoAge/sdk-core/src/test/kotlin/com/kaoage/sdk/core/ErrorHandlingRegressionTest.kt`
- [X] T030 [P] Review license compliance and update records for new dependencies in `/Users/tamurayousuke/work/KaoAge/docs/compliance/dependency-register.md`
- [X] T031 Refresh architecture and privacy overview in `/Users/tamurayousuke/work/KaoAge/docs/sdk/architecture.md`
- [X] T032 [P] Run end-to-end validation using quickstart instructions and capture findings in `/Users/tamurayousuke/work/KaoAge/specs/001-add-face-insights/validation-notes.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 → Phase 2 → Phase 3 → Phase 4 → Phase 5 → Phase 6** (strict order)
- Phases 3–5 may proceed in parallel across different teams only after Phase 2 completes, respecting story priorities.

### User Story Dependency Graph

- **US1 (P1)**: Depends on Phase 2 completion; no other story dependencies.
- **US2 (P2)**: Depends on Phase 2 completion; independent of US1 but leverages shared models.
- **US3 (P3)**: Depends on Phase 2 completion; may start after US1 to reuse telemetry hooks but not strictly required.

### Task Dependencies

- Tests precede implementation within each story (e.g., T010/T011 before T012–T016).
- Configuration builder tasks (T025–T026) depend on foundational config work (T007).
- Polish tasks (T028–T032) require completion of user stories to generate meaningful benchmarks and documentation.

---

## Parallel Execution Examples

### User Story 1
- Parallel option: After T010 completes, tasks T012 and T013 can proceed concurrently if coordinated on shared interfaces.
- Test execution: Run new unit and Robolectric suites together via `./gradlew sdk-core:test samples:cashier-app:test`.

### User Story 2
- Parallel option: T019 (bestshot engine) and T021 (UI prompts) can run in parallel once T017/T018 establish expected behaviors.
- Test execution: Execute `./gradlew sdk-bestshot:test samples:cashier-app:test` with offline mode simulations.

### User Story 3
- Parallel option: T025 (builder API) can progress while T026 (telemetry reporter) is implemented, provided interfaces are agreed.
- Test execution: Run `./gradlew sdk-core:test` focusing on new config and serialization suites.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phases 1 and 2 to establish toolchain, data models, and test harness.
2. Execute Phase 3 tasks T010–T016 to deliver cashier-ready insights.
3. Validate MVP via Robolectric tests and manual quickstart walkthrough before extending scope.

### Incremental Delivery
1. Deliver MVP (US1).
2. Add self-checkout privacy capabilities (US2) while preserving independent releases.
3. Layer developer observability and configurability (US3).
4. Finish with polish tasks for performance, compliance, and documentation.

### Parallel Team Strategy
1. Team collectively completes Phases 1–2.
2. Assign US1, US2, US3 to separate sub-teams once foundation is green.
3. Coordinate via shared contracts to avoid merge conflicts; reconvene for Phase 6 polish.
