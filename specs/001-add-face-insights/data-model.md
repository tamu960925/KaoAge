# Data Model – Face Insights SDK

## Entities

### FaceInsightsResult
- **Purpose**: Primary payload describing a single face analysis outcome per frame.
- **Fields**:
  - `detectionId: String` – stable identifier scoped to the current session.
  - `frameTimestampMillis: Long` – capture time supplied by host frame provider.
  - `boundingBox: BoundingBox` – rectangle in preview coordinates.
  - `eulerAngles: EulerAngles` – yaw, pitch, roll in degrees.
  - `landmarks: LandmarkSet` – required points (`leftEye`, `rightEye`, `noseTip`, `mouthLeft`, `mouthRight`) with normalized coordinates.
  - `faceConfidence: Float` – ML Kit detection confidence 0..1.
  - `ageBracket: AgeBracket` – enum (`CHILD`, `TEEN`, `ADULT`, `SENIOR`) tuned via calibration.
  - `ageConfidence: Float` – probability 0..1 aligned with selected bracket.
  - `gender: GenderLabel` – enum (`FEMALE`, `MALE`, `UNDETERMINED`).
  - `genderConfidence: Float` – probability for selected gender label (0..1).
  - `bestShotEligible: Boolean` – whether the frame meets BestShot baseline.
  - `bestShotReasons: Set<BestShotReason>` – optional reasons confirming or rejecting eligibility.
- **Relationships**:
  - References `BestShotSignal` when `bestShotEligible` is true via shared `detectionId`.
- **Validation**:
  - Confidence values bounded between `0f` and `1f`.
  - Bounding box fits inside host-provided frame dimensions.
  - Landmarks contain all required points with normalized coordinates `0f..1f`.
  - `detectionId` uniqueness enforced within a session.

### BestShotSignal
- **Purpose**: Notifies the host that a given frame warrants capture or review.
- **Fields**:
  - `detectionId: String`
  - `frameTimestampMillis: Long`
  - `qualityScore: Float` – aggregate score 0..1.
  - `trigger: BestShotTrigger` – enum (e.g., `FACE_STABLE`, `MAX_CONFIDENCE`, `TIMEOUT_RECOVERY`).
  - `cooldownMillis: Long` – recommended waiting period before next capture.
- **Relationships**:
  - Linked 1:1 with `FaceInsightsResult` when emitted in the same frame.
- **Validation**:
  - `qualityScore >= 0.8` for positive triggers.
  - `cooldownMillis >= 0`.

### BoundingBox
- **Fields**: `left: Float`, `top: Float`, `right: Float`, `bottom: Float`, `imageWidth: Int`, `imageHeight: Int`.
- **Validation**:
  - Coordinates constrained within image dimensions.
  - Width and height ≥ configured `minFaceSize`.

### EulerAngles
- **Fields**: `yaw: Float`, `pitch: Float`, `roll: Float`.
- **Validation**: Values range within `-180..180`.

### LandmarkSet
- **Fields**: Map of `LandmarkType` → `NormalizedPoint`.
- **Validation**:
  - Must contain all enumerated mandatory landmark types.
  - Each `NormalizedPoint` has `x` and `y` within `0f..1f`.

### NormalizedPoint
- **Fields**: `x: Float`, `y: Float`, `probability: Float?`.
- **Validation**:
  - Coordinates `0f..1f`.
  - Optional probability `0f..1f` when supplied by ML Kit.

### DetectionSessionConfig
- **Purpose**: Host-supplied configuration for detection thresholds.
- **Fields**:
  - `minFaceConfidence: Float` – default 0.7.
  - `minAgeConfidence: Float` – default 0.6.
  - `minGenderConfidence: Float` – default 0.6.
  - `minFaceSizeRatio: Float` – default 0.15 of frame height.
  - `maxFrameLatencyMillis: Long` – default 500.
  - `enableBestShot: Boolean`.
  - `cooldownMillis: Long` – default 2500 when BestShot enabled.
- **Validation**:
  - All thresholds between `0f` and `1f`.
  - `maxFrameLatencyMillis` positive and ≥ detection loop period.
  - `cooldownMillis` ≥ 0.

### DetectionSessionState (state machine)
- **States**:
  - `Idle` → awaiting host frames.
  - `Analyzing` → processing incoming frame.
  - `Publishing` → delivering `FaceInsightsResult` and optional `BestShotSignal`.
  - `Error` → surfaces recoverable issues (missing models, invalid permissions).
- **Transitions**:
  - `Idle` → `Analyzing` on first frame receipt with loaded models.
  - `Analyzing` → `Publishing` after inference completes within latency target.
  - `Publishing` → `Idle` when no frames pending.
  - Any state → `Error` on missing models, invalid config, or ML Kit failure; host can reset to `Idle` after remediation.
