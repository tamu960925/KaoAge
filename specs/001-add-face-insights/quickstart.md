# Quickstart – Face Insights SDK

## 1. Prepare Environment
- Install Android Studio Hedgehog or newer with JDK 17.
- Clone the repository and checkout branch `001-add-face-insights`.
- Run `scripts/download_models.sh` to fetch sanctioned age/gender models (no images stored).
- Verify `google-services.json` is **not** required; ML Kit face detection operates fully on-device.

## 2. Follow t-wada Style TDD Loop
1. **Red** – Write a failing Robolectric/JUnit test describing the observable behavior (e.g., age bracket thresholds, BestShot cadence).
2. **Green** – Implement the minimal production code to satisfy the test while keeping camera control in the host app.
3. **Refactor** – Remove duplication, clarify intent, and update fixtures; maintain all tests green.

> Keep each loop under 10 minutes; prefer parameterized tests to cover edge cases incrementally.

## 3. Integrate Modules
- Add `sdk-core` and `sdk-bestshot` as Gradle module dependencies in the host app.
- Configure Kotlin compiler to use version 1.9.24 and enable `kotlin-parcelize`.
- Register `FaceInsightsSdk.initialize(context, DetectionSessionConfig)` during app startup.

## 4. Feed Frames from Host Camera
- Use CameraX or existing camera stack to obtain `ImageProxy`.
- Convert frames to the SDK’s required buffer format (YUV NV21 recommended).
- Invoke `FaceInsightsSession.analyze(frameMetadata)`; the SDK returns `FaceInsightsResult` with landmarks, age, gender, and optional `BestShotSignal`.

## 5. Handle Outputs
- Overlay bounding boxes and landmarks via host UI.
- Evaluate `ageBracket`, `gender`, and `confidence` to guide checkout prompts.
- When `bestShotEligible` is true, capture the frame or escalate to compliance workflow without persisting raw pixels beyond session scope.

## 6. Serialize & Transport Results
- Use `result.toJson()` for logging or offline analytics (ensure logs remain on device).
- Pass `Parcelable` results between fragments or services as needed.
- When additional telemetry is required, call `TelemetryReporter().serialize(result)` and stream to on-device analytics.

## 7. Validate Performance
- Test on Pixel 6 and Pixel 4a devices; confirm latency ≤500 ms for 95% frames.
- Monitor CPU usage to remain below 30% during sustained detection windows.

## 8. Privacy Safeguards
- Keep airplane mode enabled during dry runs to demonstrate offline processing.
- Clear session state after checkout via `FaceInsightsSession.clear()` to drop references to previous frames.

## 9. Continuous Testing
- Add regression tests for every bug fix before shipping.
- Ensure CI pipelines execute instrumentation and Robolectric suites headlessly.
