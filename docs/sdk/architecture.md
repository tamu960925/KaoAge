# Face Insights SDK Architecture

- `sdk-core`: Hosts detection orchestration, data models, configuration builders, and telemetry helpers.
- `sdk-bestshot`: Supplies `BestShotEngine` implementing `BestShotEvaluator` for frame quality scoring.
- `samples/cashier-app`: Demonstrates host-controlled camera ownership with cashier and self-checkout flows.
- `specs/001-add-face-insights`: Houses planning artifacts, quickstart, and validation notes for the feature.
- Camera frames flow from host app → `FaceInsightsSession` → optional `BestShotBridge` callbacks; no raw imagery is persisted.
