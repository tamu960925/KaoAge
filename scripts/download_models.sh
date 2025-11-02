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

MODEL_NAME="mobilenet_v1_1.0_224_quant"
MODEL_FILENAME="${MODEL_NAME}.tflite"
MODEL_TARGET="$MODELS_DIR/$MODEL_FILENAME"
MODEL_URL="https://raw.githubusercontent.com/google-coral/test_data/master/${MODEL_FILENAME}"
# SHA-256 checksum verified against Google Coral test_data repository (Apache-2.0)
EXPECTED_SHA256="2d0ebfa2e75ae93c709e9ecc27570c9c0e49b7780733a840f82589fc1272fc3d"

if [[ -f "$MODEL_TARGET" ]]; then
  echo "[kaoage] $MODEL_FILENAME already exists. Verifying checksum…" >&2
  EXISTING_SHA256="$(checksum "$MODEL_TARGET")"
  if [[ "$EXISTING_SHA256" == "$EXPECTED_SHA256" ]]; then
    echo "[kaoage] Existing model is up to date." >&2
    exit 0
  fi
  echo "[kaoage] Existing model checksum mismatch. Re-downloading…" >&2
fi

echo "[kaoage] Downloading $MODEL_FILENAME from Google Coral test data repository…" >&2
curl --fail --location --output "$TMP_DIR/$MODEL_FILENAME" "$MODEL_URL"

DOWNLOADED_SHA256="$(checksum "$TMP_DIR/$MODEL_FILENAME")"
if [[ "$DOWNLOADED_SHA256" != "$EXPECTED_SHA256" ]]; then
  echo "[kaoage] ERROR: Checksum mismatch for $MODEL_FILENAME" >&2
  echo "Expected: $EXPECTED_SHA256" >&2
  echo "Actual:   $DOWNLOADED_SHA256" >&2
  exit 1
fi

mv "$TMP_DIR/$MODEL_FILENAME" "$MODEL_TARGET"
echo "[kaoage] Model stored at $MODEL_TARGET" >&2
echo "[kaoage] Remember to record the artifact in docs/compliance/dependency-register.md" >&2
