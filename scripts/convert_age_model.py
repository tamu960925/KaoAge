#!/usr/bin/env python3
"""
Convert the UTKFace age regression Keras model to TensorFlow Lite.

Usage:
  python scripts/convert_age_model.py --input models/age_regression_source.h5 --output models/age_regression.tflite

Requires TensorFlow 2.10+ (tested with 2.13+) and Python 3.9-3.11.
"""

import argparse
import pathlib
import sys

try:
    import tensorflow as tf
except ImportError as exc:  # pragma: no cover
    sys.stderr.write(
        "TensorFlow is required to convert the age regression model.\n"
        "Install it with: pip install tensorflow-cpu\n"
    )
    raise exc


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Convert age regression Keras model to TFLite")
    parser.add_argument("--input", required=True, help="Path to best_model.h5 from Sharris/age_detection_regression")
    parser.add_argument("--output", required=True, help="Destination path for the .tflite file")
    parser.add_argument("--quantize", action="store_true", help="Apply dynamic range quantization")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    input_path = pathlib.Path(args.input).expanduser().resolve()
    output_path = pathlib.Path(args.output).expanduser().resolve()

    if not input_path.exists():
        raise FileNotFoundError(f"Input model not found: {input_path}")

    print(f"[converter] Loading Keras model from {input_path}")
    model = tf.keras.models.load_model(input_path, compile=False)

    print("[converter] Building TensorFlow Lite converter")
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    if args.quantize:
        converter.optimizations = [tf.lite.Optimize.DEFAULT]

    print("[converter] Converting…")
    tflite_model = converter.convert()

    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(tflite_model)
    print(f"[converter] Saved TensorFlow Lite model to {output_path}")

    try:
        interpreter = tf.lite.Interpreter(model_content=tflite_model)
        interpreter.allocate_tensors()
        print("[converter] Verified TensorFlow Lite interpreter initialization")
    except Exception as exc:  # pragma: no cover
        print(f"[converter] Warning: could not initialize TFLite interpreter: {exc}")


if __name__ == "__main__":
    main()
