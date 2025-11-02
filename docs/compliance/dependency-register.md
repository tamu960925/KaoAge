# Dependency & Model Register

| Name | Version | Type | Source | License | Checksum | Notes |
|------|---------|------|--------|---------|----------|-------|
| Kotlin Stdlib | 1.9.24 | Library | Maven Central | Apache-2.0 | N/A | Bundled via Gradle |
| ML Kit Face Detection | 17.1.0 | Library | Google Maven | Proprietary | N/A | Ensure offline usage only |
| kotlinx-serialization-json | 1.6.3 | Library | Maven Central | Apache-2.0 | N/A | Used for toJson outputs |
| kotlinx-coroutines-play-services | 1.8.1 | Library | Maven Central | Apache-2.0 | N/A | Enables `Tasks.await` interop |
| AndroidX CameraX Core | 1.3.4 | Library | Google Maven | Apache-2.0 | N/A | Core camera pipeline |
| AndroidX CameraX Camera2 | 1.3.4 | Library | Google Maven | Apache-2.0 | N/A | Default camera backend |
| AndroidX CameraX Lifecycle | 1.3.4 | Library | Google Maven | Apache-2.0 | N/A | Binds camera to lifecycle |
| AndroidX CameraX View | 1.3.4 | Library | Google Maven | Apache-2.0 | N/A | Provides `PreviewView` helper |
| TensorFlow Lite | 2.13.0 | Library | Maven Central | Apache-2.0 | N/A | Runs on-device age regression inference |
| Age Regression Source (best_model.h5) | 2025-09-12 | Model | huggingface.co/Sharris/age_detection_regression | MIT | 1837be0488aebe6199bd17d0e27f5222b28b5a79d3af8cdeebdb9e9f6c631557 | Downloaded via `scripts/download_models.sh` |
| Age Regression (TFLite) | user-generated | Model | Converted locally from best_model.h5 | MIT | (user computed) | Produce with `scripts/convert_age_model.py`; do not commit the artifact |
