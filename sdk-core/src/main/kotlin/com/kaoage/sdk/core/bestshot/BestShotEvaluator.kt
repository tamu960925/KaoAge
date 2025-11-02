package com.kaoage.sdk.core.bestshot

import com.kaoage.sdk.core.config.DetectionSessionConfig
import com.kaoage.sdk.core.detection.DetectionResult
import com.kaoage.sdk.core.model.BestShotSignal

fun interface BestShotEvaluator {
    fun evaluate(detection: DetectionResult, config: DetectionSessionConfig): BestShotSignal?
}

class BestShotBridge(
    private val evaluator: BestShotEvaluator?,
    private val onBestShot: (BestShotSignal) -> Unit,
    private val onLowConfidence: () -> Unit
) {
    fun handle(detection: DetectionResult, config: DetectionSessionConfig) {
        val signal = evaluator?.evaluate(detection, config)
        if (signal?.eligible == true) {
            onBestShot(signal)
        } else {
            onLowConfidence()
        }
    }
}
