<!--
Sync Impact Report
Version change: 0.0.0 → 1.0.0
Modified principles:
- (new) → Kotlin-First SDK with Java Interop
- (new) → Face Intelligence Boundaries
- (new) → On-Device Privacy Guarantees
- (new) → Test-Driven Development Discipline
- (new) → Model Stewardship & Dependency Safety
Added sections:
- Technical Guardrails
- Delivery Workflow
Removed sections:
- None
Templates requiring updates:
- ✅ .specify/templates/plan-template.md
- ✅ .specify/templates/tasks-template.md
Follow-up TODOs:
- None
-->

# KaoAge Constitution

## Core Principles

### Kotlin-First SDK with Java Interop
All KaoAge SDK modules MUST be authored in Kotlin. Any public API intended for Java
callers MUST expose `@JvmStatic`, `@JvmOverloads`, or builder/callback patterns so
Java apps can integrate without shims. Mixing additional JVM languages requires a
governance waiver. This keeps the codebase cohesive while honoring downstream
Android teams that rely on Java entry points.

### Face Intelligence Boundaries
Face analytics MUST use on-device ML Kit Face Detection. The SDK MUST output the
detected face box, Euler angles (yaw, pitch, roll), and landmarks for eyes, nose,
and mouth, plus the BestShot signal stream. Alternative detectors or cloud calls
are prohibited. Centralizing on ML Kit ensures consistent accuracy, licensing, and
support from Google’s maintained models.

### On-Device Privacy Guarantees
The SDK MUST never persist raw images, frames, or derived templates. All inference
occurs on-device, and camera ownership stays in the host application. KaoAge SDKs
may only return analysis payloads and BestShot instructions. These boundaries keep
consumer privacy intact and prevent data egress risks.

### Test-Driven Development Discipline
Every change begins with failing tests (JUnit/Robolectric). Developers MUST follow
Red-Green-Refactor: write tests, observe them fail, implement, then refactor with
tests passing. No production code merges without automated test coverage for the
behavior. This discipline preserves regression safety in a camera-facing SDK.

### Model Stewardship & Dependency Safety
Models remain external assets fetched via `scripts/download_models.sh` and loaded
by File, ByteBuffer, or AssetFileDescriptor. New dependencies require documented
tests and a license audit before adoption. Gradle, ML, and native artifacts MUST be
version-pinned to guarantee reproducible builds and compliance.

## Technical Guardrails

- Deliverables MUST live in `sdk-core`, `sdk-bestshot`, `samples/cashier-app`, and
  `docs`, with Gradle modules mirroring that structure.
- The host app retains camera session control; the SDK only consumes provided
  frames and returns analysis/best-shot guidance.
- Parcelables MUST back every outward-facing data model and provide `toJson()` for
  logging or transport. No binary serialization alternatives are allowed.
- ML Kit models and configuration MUST be bundled or downloaded via the managed
  script; runtime downloads need explicit UX handling in the host app.

## Delivery Workflow

- Plans and specs MUST document Kotlin targeting Android 10+ and spell out ML Kit
  integration, camera handoff, and parcelable contracts.
- Tasks MUST schedule test authoring ahead of implementation, enumerating the
  failing unit/integration tests required for each story.
- Dependency additions or upgrades MUST include a LICENSE check entry and test
  coverage plan before implementation begins.
- Sample app updates MUST demonstrate the BestShot flow, parcelable models, and
  on-device execution path.

## Governance

- This constitution governs all KaoAge SDK repositories, samples, and docs. Any
  conflicting practice documents are superseded.
- Amendments require agreement by the SDK lead engineer and product owner, a
  documented migration plan, updated templates, and a semantic version bump.
- Versioning follows SemVer: MAJOR for breaking policy changes, MINOR for new
  principles or material expansions, PATCH for clarifications.
- Every PR review MUST confirm Kotlin-only source, ML Kit usage, on-device privacy,
  TDD evidence (failing tests first), and dependency/license compliance.
- Compliance reviews occur quarterly; findings and remediation owners are recorded
  in `docs/governance.md`.

**Version**: 1.0.0 | **Ratified**: 2025-11-01 | **Last Amended**: 2025-11-01
