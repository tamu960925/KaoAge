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
| Age/Gender Age Model (non-quantized) | 2025-11-02 snapshot | Model | github.com/shubham0204/Age-Gender_Estimation_TF-Android/app/src/main/assets/model_age_nonq.tflite | MIT | 870609353f25a3f6af4f1b815ac4c09a4a086e2a2f5c1109e28286bb6d410efe | Download via `scripts/download_models.sh` |
| Age/Gender Gender Model (non-quantized) | 2025-11-02 snapshot | Model | github.com/shubham0204/Age-Gender_Estimation_TF-Android/app/src/main/assets/model_gender_nonq.tflite | MIT | 7317b8e5e1014b5062970c61dcd0afa3293b5766cc89facbb43cd16d8e619cf1 | Download via `scripts/download_models.sh` |
