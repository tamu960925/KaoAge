package com.kaoage.sdk.core.telemetry

import com.kaoage.sdk.core.model.FaceInsightsResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TelemetryReporter {
    private val json = Json { encodeDefaults = true }

    fun serialize(result: FaceInsightsResult): String = json.encodeToString(result)
}
