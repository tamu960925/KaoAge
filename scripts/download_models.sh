#!/usr/bin/env bash

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODELS_DIR="${REPO_ROOT}/models"

AGE_MODEL_URL="https://raw.githubusercontent.com/shubham0204/Age-Gender_Estimation_TF-Android/master/app/src/main/assets/model_age_nonq.tflite"
GENDER_MODEL_URL="https://raw.githubusercontent.com/shubham0204/Age-Gender_Estimation_TF-Android/master/app/src/main/assets/model_gender_nonq.tflite"

mkdir -p "${MODELS_DIR}"

download() {
  local url="$1"
  local target="$2"

  echo "Downloading ${url} -> ${target}"
  curl -fsSL "${url}" -o "${target}.tmp"
  mv "${target}.tmp" "${target}"
}

download "${AGE_MODEL_URL}" "${MODELS_DIR}/model_age_nonq.tflite"
download "${GENDER_MODEL_URL}" "${MODELS_DIR}/model_gender_nonq.tflite"

echo "Models downloaded to ${MODELS_DIR}"
