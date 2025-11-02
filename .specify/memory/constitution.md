<!--
Sync Impact Report
Version change: N/A → 1.0.0
Modified principles: Initial adoption
Added sections: Entire constitution
Removed sections: None
Templates requiring updates:
- ✅ .specify/templates/plan-template.md (age/gender guardrail added)
- ✅ .specify/templates/spec-template.md (reviewed; no changes needed)
- ✅ .specify/templates/tasks-template.md (path conventions updated)
- ⚠ .specify/templates/commands/ (no command templates present; confirm if future commands needed)
Follow-up TODOs: None
-->
# KaoAge Constitution

## Metadata
- **Constitution Version**: 1.0.0
- **Ratification Date**: 2025-11-02
- **Last Amended Date**: 2025-11-02
- **Project Scope**: Android on-device face landmark detection and age/gender estimation SDK comprising `sdk-core`, `sdk-bestshot`, `samples/cashier-app`, and `docs`.

## Mission
KaoAge delivers an on-device Android 10+ SDK that detects facial landmarks and predicts age and gender while the host app retains camera ownership. The SDK returns analysis artifacts and BestShot guidance through Kotlin-first, Java-friendly APIs without persisting or transmitting images.

## Principles

### Principle 1: On-Device Privacy & Host Control
- The SDK MUST run all face analysis on-device with zero network calls and MUST NOT persist raw frames or intermediate tensors.
- The host application MUST own camera lifecycle and only pass frames to the SDK; SDK code MUST NOT open or manage camera sessions.
- SDK outputs MUST be limited to in-memory analysis artifacts (landmarks, bounding boxes, Euler angles, age/gender estimates, BestShot signals) and cleared after delivery.
**Rationale**: Protects user privacy, respects app boundaries, and ensures compliance with on-device mandates.

### Principle 2: Contract-First Kotlin APIs with Java Interop
- Production modules MUST be authored in Kotlin and expose Java-friendly builders, callbacks, and interop annotations (`@JvmStatic`, `@JvmOverloads`) for Android 10+ compatibility.
- All public result models MUST implement `Parcelable`, provide a deterministic `toJson()`, and include bounding box, Euler angles, required landmarks (eyes, nose, mouth), and prediction metadata.
- SDK layers MUST remain headless: camera control, UI, and threading policies stay in the host app; SDK only publishes analysis results and BestShot signals.
**Rationale**: Guarantees consistent integration across Kotlin and Java apps while keeping the SDK scoped to analysis responsibilities.

### Principle 3: Canonical ML Execution
- Face landmark detection MUST use ML Kit Face Detection with repository-approved configuration; alternative detectors or cloud services require governance approval.
- Age and gender estimation MUST rely on sanctioned on-device models fetched via `scripts/download_models.sh` and loaded through File, ByteBuffer, or AssetFileDescriptor paths with checksums.
- Pre/post-processing pipelines, score thresholds, and model versions MUST be documented in `docs/` and versioned alongside SDK releases.
**Rationale**: Maintains deterministic ML behavior, auditability, and reproducibility across environments.

### Principle 4: Test-Driven Quality & Dependency Governance
- Every change MUST begin with failing JUnit or Robolectric tests covering the desired behavior before production code changes land.
- New dependencies or ML models MUST ship with documented license review, reproducible tests, and recorded justification in `docs/compliance/`.
- CI pipelines MUST enforce failing-first evidence and block merges until tests validating principles pass on-device or Robolectric environments.
**Rationale**: Preserves reliability, legal compliance, and a verifiable TDD workflow.

## Governance

### Amendment Procedure
- Proposed amendments MUST be authored as RFCs in `docs/governance/` referencing impacted principles and rationale.
- Core maintainers review RFCs within five business days; adoption requires unanimous approval or, failing unanimity, a majority vote with documented dissent resolution.
- Upon approval, update this constitution, regenerate the Sync Impact Report, and propagate changes to plan/spec/task templates before merging.

### Versioning Policy
- Semantic versioning applies: MAJOR for principle removals or incompatible shifts, MINOR for new principles or expanded scope, PATCH for clarifications.
- `RATIFICATION_DATE` reflects the original adoption; `LAST_AMENDED_DATE` updates with each merged amendment.

### Compliance Reviews
- Feature kickoffs MUST pass the Constitution Check gate in `.specify/templates/plan-template.md` before implementation.
- Quarterly audits evaluate SDK modules, sample apps, and scripts for adherence; log findings and remediation tasks in `docs/compliance/`.
- Any violation triggers corrective tasks in `/specs/.../tasks.md` with accountable owners and deadlines tracked to closure.
