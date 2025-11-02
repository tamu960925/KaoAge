# Model Asset Workflow

1. Run `scripts/download_models.sh` to provision the `models/` directory.
   - The script downloads `age_regression_source.h5` (MIT license, from `Sharris/age_detection_regression`).
   - If `models/age_regression.tflite` is missing, follow the console instructions to execute `scripts/convert_age_model.py` with TensorFlow installed; the converter produces the TFLite regression model required on-device.
2. Verify model artifacts against approved SHA-256 checksums supplied by compliance (see `docs/compliance/dependency-register.md`).
3. Record model name, version, source, license, and checksum in `docs/compliance/dependency-register.md` whenever assets change.
4. Ensure models are stored only on developer machines and packaged through Android Asset delivery (no raw image persistence). Do not commit generated `.tflite` artifacts to source control.
