#!/usr/bin/env bash
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODELS_DIR="$REPO_ROOT/models"

mkdir -p "$MODELS_DIR"

echo "[kaoage] Place sanctioned age/gender TFLite models in $MODELS_DIR" >&2
echo "[kaoage] Update docs/compliance/dependency-register.md with model license and checksum" >&2
