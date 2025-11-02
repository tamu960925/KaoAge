# Feature Specification: Face Insights SDK

**Feature Branch**: `[001-add-face-insights]`  
**Created**: 2025-11-02  
**Status**: Draft  
**Input**: User description: "Android向け、オンデバイスで顔ランドマーク検出 + 年齢/性別推定のライブラリを作る。"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Age-restricted checkout verification (Priority: P1)

Store associates using the cashier app need to confirm whether a customer purchasing age-restricted items appears to be an adult without slowing the line.

**Why this priority**: Compliance checks at checkout are the primary business driver for deploying face insights; failure directly impacts revenue and legal exposure.

**Independent Test**: Run the cashier app with the SDK in a staging store, capture sample customers, and verify the SDK outputs age bracket and gender cues that the associate can act on without external systems.

**Acceptance Scenarios**:

1. **Given** a cooperative customer centered in view, **When** the associate points the device camera for two seconds, **Then** the app displays the detected face box, age bracket, gender cue, and a confidence indicator generated on-device.
2. **Given** an adult customer with low lighting, **When** the associate retries capture, **Then** the SDK provides updated results within one second or clearly states that confidence is insufficient.

---

### User Story 2 - Self-checkout privacy guardrails (Priority: P2)

Self-checkout users need assurance that demographic analysis happens locally and no face imagery leaves the kiosk, while still receiving prompts when the system cannot classify them.

**Why this priority**: Privacy assurance is a major adoption barrier; demonstrating on-device processing builds trust and supports legal compliance.

**Independent Test**: Operate a self-checkout kiosk in airplane mode, run multiple scans, and verify all outputs are produced locally, persisted only in transient memory, and that the UI receives explicit "analysis unavailable" messages when confidence drops below threshold.

**Acceptance Scenarios**:

1. **Given** the kiosk offline with no network, **When** a shopper initiates the scan, **Then** the SDK returns analysis results and the app logs that no media files were stored or transmitted.
2. **Given** a masked shopper partially facing away, **When** the scan runs, **Then** the SDK signals low confidence and the app prompts the shopper to adjust instead of presenting potentially incorrect demographics.

---

### User Story 3 - Developer integration and monitoring (Priority: P3)

An Android app developer must integrate the SDK quickly, inspect outputs in JSON for telemetry, and configure thresholds to fit the store’s risk policy without writing custom ML code.

**Why this priority**: Faster integration shortens rollout time and lowers engineering costs compared to building ML pipelines in-house.

**Independent Test**: In a demo project, integrate the SDK, enable logging, and confirm the developer can adjust detection thresholds, serialize results for dashboards, and receive best-shot frame markers without modifying model internals.

**Acceptance Scenarios**:

1. **Given** a fresh Android project, **When** the developer follows SDK setup docs, **Then** the sample app compiles and emits structured detection objects that can be handed off between app modules and exported as JSON snapshots.
2. **Given** a need to tune confidence thresholds, **When** the developer updates configuration, **Then** the SDK respects the new settings on the next detection run without requiring app restarts.

---

### Edge Cases

- No face is detected within the frame window (e.g., empty scene or subject too far away).
- Multiple faces appear simultaneously and the primary face must be selected deterministically.
- Subject wears masks, hats, or glasses that occlude landmarks.
- Rapid head movement causes motion blur across consecutive frames.
- Extreme lighting (backlit sun, dark bar) reduces detection confidence.
- Device camera permission is denied or revoked mid-session.
- Device thermal throttling slows frame processing and risks exceeding response targets.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The SDK MUST detect the most prominent face in each supported frame and return its bounding box coordinates.
- **FR-002**: The SDK MUST provide face orientation output (yaw, pitch, roll) aligned to the device camera reference.
- **FR-003**: The SDK MUST surface landmark positions for both eyes, nose, and mouth corners sufficient for overlay rendering.
- **FR-004**: The SDK MUST estimate an age bracket per detection and expose both the bracket label and confidence score.
- **FR-005**: The SDK MUST classify perceived gender with a confidence score and allow “undetermined” when confidence falls below the configured threshold.
- **FR-006**: The SDK MUST emit a “best shot” marker identifying frames suitable for downstream capture or analytics.
- **FR-007**: The SDK MUST deliver results through structured objects that downstream app modules can consume and serialize to JSON without data loss.
- **FR-008**: The SDK MUST execute entirely on-device, never persisting face imagery or transmitting data over a network connection.
- **FR-009**: The SDK MUST allow integrators to configure minimum detection confidence, face size, and retry limits at runtime.
- **FR-010**: The SDK MUST fail gracefully by returning explicit error or low-confidence states when required inputs (camera feed, permissions, models) are unavailable.

### Key Entities *(include if feature involves data)*

- **FaceInsightsResult**: Represents a single detection event, including bounding box, orientation angles, landmark coordinates, age bracket, gender classification, confidence levels, and timestamp.
- **BestShotSignal**: Represents a hint for downstream systems that a given frame meets quality thresholds (sharpness, frontal pose, confidence) for storage or compliance review, without containing pixel data.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 95% of detections on reference cashier devices complete within 500 ms from frame capture to result delivery.
- **SC-002**: Age classification agrees with human-reviewed adult/minor labels in 98% of audited checkout sessions.
- **SC-003**: 90% of cashier trials report that the SDK-provided prompts allow them to proceed without manual ID checks in under 3 minutes.
- **SC-004**: 100% of privacy audits confirm no face imagery or demographic data leaves the device or persists beyond the active session.

## Assumptions

- Retail partners supply user consent flows before activating the camera for demographic estimation.
- Evaluation devices align with Android 10+ mid-tier hardware (e.g., Snapdragon 7-series class) for performance targets.
- Legal and ethics teams approve presenting gender as “female,” “male,” or “undetermined” to balance clarity with inclusivity.
