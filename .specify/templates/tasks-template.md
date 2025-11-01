
description: "Task list template for feature implementation"
---

# Tasks: [FEATURE NAME]

**Input**: Design documents from `/specs/[###-feature-name]/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: TDD is mandatory. Add failing JUnit/Robolectric tests before any production code.

**Organization**: Group tasks by user story so each slice stays independently testable.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions
- Use Kotlin paths (`sdk-core/src/main/kotlin/...`, `samples/cashier-app/src/main/java|kotlin/...`)

## Path Conventions

- `sdk-core/` — ML Kit face analysis primitives
- `sdk-bestshot/` — BestShot heuristics layered on core analysis
- `samples/cashier-app/` — Host-controlled camera sample integrating the SDK
- `docs/` — Architecture notes, compliance records
- Paths shown below assume this core structure; expand only with documented justification

<!-- 
  ============================================================================
  IMPORTANT: The tasks below are SAMPLE TASKS for illustration purposes only.
  
  The /speckit.tasks command MUST replace these with actual tasks based on:
  - User stories from spec.md (with their priorities P1, P2, P3...)
  - Feature requirements from plan.md
  - Entities from data-model.md
  - Endpoints from contracts/
  
  Tasks MUST be organized by user story so each story can be:
  - Implemented independently
  - Tested independently
  - Delivered as an MVP increment
  
  DO NOT keep these sample tasks in the generated tasks.md file.
  ============================================================================
-->

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [ ] T001 Confirm module layout (`sdk-core`, `sdk-bestshot`, `samples/cashier-app`, `docs`)
- [ ] T002 Configure Gradle Kotlin DSL, toolchain, and ML Kit dependency with license log
- [ ] T003 [P] Validate `scripts/download_models.sh` workflow and document asset destinations

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T004 Create parcelable data contracts in `sdk-core` with `toJson()` scaffolds
- [ ] T005 [P] Implement ML Kit detector configuration wrapper with dependency injection seams
- [ ] T006 [P] Establish Robolectric harness to inject camera frames for tests
- [ ] T007 Add failing contract tests covering BestShot signal outputs
- [ ] T008 Document host-app camera ownership and frame handoff in `docs/architecture.md`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - [Title] (Priority: P1) 🎯 MVP

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 1 (MANDATORY — author first, ensure they fail)

- [ ] T010 [P] [US1] Add JUnit tests in `sdk-core/src/test/kotlin/...` for face box and Euler data
- [ ] T011 [P] [US1] Add Robolectric test in `samples/cashier-app/src/test/...` for camera handoff

### Implementation for User Story 1

- [ ] T012 [P] [US1] Implement parcelable in `sdk-core/src/main/kotlin/[path]/FaceAnalysisResult.kt`
- [ ] T013 [P] [US1] Provide `toJson()` helper in `sdk-core/src/main/kotlin/[path]/serialization`
- [ ] T014 [US1] Wire ML Kit detector wrapper in `sdk-core/src/main/kotlin/[path]/FaceAnalyzer.kt`
- [ ] T015 [US1] Expose Java-friendly facade in `sdk-core/src/main/kotlin/[path]/KaoAgeSdk.kt`
- [ ] T016 [US1] Update sample UI output in `samples/cashier-app/src/main/java|kotlin/[path]`
- [ ] T017 [US1] Add structured logging (no image persistence) around analysis lifecycle

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - [Title] (Priority: P2)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 2 (MANDATORY — author first, ensure they fail)

- [ ] T018 [P] [US2] Add unit tests in `sdk-bestshot/src/test/kotlin/...` for BestShot heuristics
- [ ] T019 [P] [US2] Add integration test in `samples/cashier-app/src/test/...` for BestShot UI

### Implementation for User Story 2

- [ ] T020 [P] [US2] Add BestShot evaluator in `sdk-bestshot/src/main/kotlin/.../BestShotEngine.kt`
- [ ] T021 [US2] Publish Java callback in `sdk-bestshot/.../BestShotCallbacks.kt`
- [ ] T022 [US2] Render BestShot hints in `samples/cashier-app/src/main/java|kotlin/[path]`
- [ ] T023 [US2] Sync parcelable updates across modules and docs

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - [Title] (Priority: P3)

**Goal**: [Brief description of what this story delivers]

**Independent Test**: [How to verify this story works on its own]

### Tests for User Story 3 (MANDATORY — author first, ensure they fail)

- [ ] T024 [P] [US3] Add regression tests in `sdk-core/src/test/kotlin/...` for landmark edge cases
- [ ] T025 [P] [US3] Add Robolectric test verifying privacy constraints (no image persistence)

### Implementation for User Story 3

- [ ] T026 [P] [US3] Implement feature logic in `sdk-core/src/main/kotlin/[path]/[Feature].kt`
- [ ] T027 [US3] Update Gradle configs with dependency and license notes
- [ ] T028 [US3] Refresh docs in `docs/[feature]/` with API examples and TDD evidence

**Checkpoint**: All user stories should now be independently functional

---

[Add more user story phases as needed, following the same pattern]

---

## Phase N: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] TXXX [P] Documentation updates in `docs/` (include compliance checkpoints)
- [ ] TXXX Refactor Kotlin APIs while preserving Java interop contract
- [ ] TXXX Performance optimization for frame processing budgets
- [ ] TXXX [P] Additional unit/integration tests reinforcing regressions caught during review
- [ ] TXXX Verify `scripts/download_models.sh` usage and checksum logs
- [ ] TXXX Run quickstart validation in `samples/cashier-app`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Starts after Foundational; remains independent even with US1 touchpoints
- **User Story 3 (P3)**: Starts after Foundational; remains independent even when touching US1/US2

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together (if tests requested):
Task: "Contract test for [endpoint] in tests/contract/test_[name].py"
Task: "Integration test for [user journey] in tests/integration/test_[name].py"

# Launch all models for User Story 1 together:
Task: "Create [Entity1] model in src/models/[entity1].py"
Task: "Create [Entity2] model in src/models/[entity2].py"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group with references to failing/passing tests
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- Capture license review decisions alongside dependency upgrades
