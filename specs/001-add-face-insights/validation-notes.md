# Validation Notes – Face Insights SDK

- Robolectric harness validates offline cashiers flows using `CameraFrameHarness`.
- Privacy tests confirm zero persistent writes and network transmissions in self-checkout scenario.
- Telemetry reporter serialized outputs inspected manually for schema drift.
- Pending: execute instrumentation tests on Pixel 6 and Pixel 4a hardware.
