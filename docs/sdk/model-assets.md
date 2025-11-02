# Model Asset Workflow

1. Run `scripts/download_models.sh` to provision the `models/` directory.
   - The script downloads the sanctioned `model_age_nonq.tflite` and `model_gender_nonq.tflite` artifacts published by [shubham0204/Age-Gender_Estimation_TF-Android](https://github.com/shubham0204/Age-Gender_Estimation_TF-Android) under the MIT license.
   - No local conversion step is required; downloaded `.tflite` files are consumed directly by the SDK.
2. Verify model artifacts against approved SHA-256 checksums supplied by compliance (see `docs/compliance/dependency-register.md`).
3. Record model name, version, source, license, and checksum in `docs/compliance/dependency-register.md` whenever assets change.
4. Ensure models are stored only on developer machines and packaged through Android Asset delivery (no raw image persistence). Do not commit generated `.tflite` artifacts to source control.
