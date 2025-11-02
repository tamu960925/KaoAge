#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODELS_DIR="$REPO_ROOT/models"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kaoage-models.XXXXXX")"

checksum() {
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$1" | awk '{ print $1 }'
  elif command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{ print $1 }'
  else
    echo "[kaoage] ERROR: Neither shasum nor sha256sum is available" >&2
    exit 1
  fi
}

cleanup() {
  rm -rf "$TMP_DIR"
}
trap cleanup EXIT

mkdir -p "$MODELS_DIR"

AGE_MODEL_URL="https://huggingface.co/Sharris/age_detection_regression/resolve/main/best_model.h5"
AGE_MODEL_FILENAME="age_regression_source.h5"
AGE_MODEL_TARGET="$MODELS_DIR/$AGE_MODEL_FILENAME"
AGE_EXPECTED_SHA256="1837be0488aebe6199bd17d0e27f5222b28b5a79d3af8cdeebdb9e9f6c631557"

echo "[kaoage] Downloading age regression source model (best_model.h5)…" >&2
curl --fail --location --output "$TMP_DIR/$AGE_MODEL_FILENAME" "$AGE_MODEL_URL"

SOURCE_SHA256="$(checksum "$TMP_DIR/$AGE_MODEL_FILENAME")"
if [[ "$SOURCE_SHA256" != "$AGE_EXPECTED_SHA256" ]]; then
  echo "[kaoage] ERROR: Checksum mismatch for $AGE_MODEL_FILENAME" >&2
  echo "Expected: $AGE_EXPECTED_SHA256" >&2
  echo "Actual:   $SOURCE_SHA256" >&2
  exit 1
fi

mv "$TMP_DIR/$AGE_MODEL_FILENAME" "$AGE_MODEL_TARGET"
echo "[kaoage] Age regression source stored at $AGE_MODEL_TARGET" >&2

AGE_TFLITE_TARGET="$MODELS_DIR/age_regression.tflite"
if [[ ! -f "$AGE_TFLITE_TARGET" ]]; then
  cat <<'EOM' >&2
[kaoage] age_regression.tflite is missing.
[kaoage] Run the converter with TensorFlow installed:

  python scripts/convert_age_model.py --input models/age_regression_source.h5 --output models/age_regression.tflite

EOM
else
  echo "[kaoage] Detected existing age_regression.tflite" >&2
fi

echo "[kaoage] Update docs/compliance/dependency-register.md with any new checksum information." >&2
