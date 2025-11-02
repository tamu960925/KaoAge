# Camera Handoff Contract

- Host application acquires and manages CameraX lifecycle.
- Frames are provided to the SDK as `ImageProxy` references alongside session configuration.
- SDK never opens camera devices or persists frame buffers.
- Host is responsible for disposing `ImageProxy.close()` after SDK completes analysis callbacks.
- SDK returns `FaceInsightsResult` and optional `BestShotSignal` via callbacks within 500 ms budget.
