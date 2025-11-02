package com.kaoage.sdk.core.detection

import kotlin.test.Test
import kotlin.test.assertTrue

class DetectionConfidenceHeuristicTest {

    @Test
    fun `face slightly below size threshold still earns usable confidence`() {
        val confidence = DetectionConfidenceHeuristic.score(
            areaRatio = 0.12f,
            hasTrackingId = false,
            minFaceSizeRatio = 0.15f
        )

        assertTrue(confidence > 0.7f, "expected confidence above session threshold, actual=$confidence")
    }

    @Test
    fun `very small face stays below acceptance threshold`() {
        val confidence = DetectionConfidenceHeuristic.score(
            areaRatio = 0.02f,
            hasTrackingId = false,
            minFaceSizeRatio = 0.15f
        )

        assertTrue(confidence < 0.7f, "expected low confidence for tiny face, actual=$confidence")
    }

    @Test
    fun `tracking id provides modest confidence boost`() {
        val confidence = DetectionConfidenceHeuristic.score(
            areaRatio = 0.12f,
            hasTrackingId = true,
            minFaceSizeRatio = 0.15f
        )

        assertTrue(confidence > 0.85f, "expected boost from tracking id, actual=$confidence")
    }
}

