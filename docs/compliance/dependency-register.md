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
| TensorFlow Lite | 2.13.0 | Library | Maven Central | Apache-2.0 | N/A | Runs MobileNet inference |
| MobileNet V1 1.0 224 Quant (TFLite) | test_data/master | Model | raw.githubusercontent.com/google-coral/test_data | Apache-2.0 | 2d0ebfa2e75ae93c709e9ecc27570c9c0e49b7780733a840f82589fc1272fc3d | Download via `scripts/download_models.sh` |
| ImageNet Labels | 2012 | Data | storage.googleapis.com/download.tensorflow.org/data/ImageNetLabels.txt | Apache-2.0 | 536feacc519de3d418de26b2effb4d75694a8c4c0063e36499a46fa8061e2da9 | Packaged with SDK for MobileNet keyword mapping |
