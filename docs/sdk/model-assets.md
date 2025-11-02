# Model Asset Workflow

1. Run `scripts/download_models.sh` to provision the `models/` directory.
2. Verify model artifacts against approved SHA-256 checksums supplied by compliance.
3. Record model name, version, source, license, and checksum in `docs/compliance/dependency-register.md`.
4. Ensure models are stored only on developer machines and packaged through Android Asset delivery (no raw image persistence).
