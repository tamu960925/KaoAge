package com.kaoage.sdk.core.inference

import com.kaoage.sdk.core.model.BoundingBox
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageProxyFaceCropperTest {

    @Test
    fun toCropRectWithTopOffset_shiftsTopWithinBounds() {
        val boundingBox = BoundingBox(
            left = 10f,
            top = 20f,
            right = 110f,
            bottom = 220f,
            imageWidth = 400,
            imageHeight = 400
        )

        val rect = assertNotNull(
            boundingBox.toCropRectWithTopOffset(
                offsetPx = 5,
                maxWidth = 400,
                maxHeight = 400
            ),
            "Rect should be created when bounds are valid"
        )
        assertEquals(10, rect.left, "left=${rect.left}")
        assertEquals(25, rect.top, "top=${rect.top}")
        assertEquals(110, rect.right, "right=${rect.right}")
        assertEquals(225, rect.bottom, "bottom=${rect.bottom}")
    }

    @Test
    fun toCropRectWithTopOffset_clampsAtImageEdge() {
        val boundingBox = BoundingBox(
            left = 5f,
            top = 390f,
            right = 205f,
            bottom = 430f,
            imageWidth = 400,
            imageHeight = 430
        )

        val rect = assertNotNull(
            boundingBox.toCropRectWithTopOffset(
                offsetPx = 5,
                maxWidth = 400,
                maxHeight = 430
            ),
            "Rect should survive clamping when partially outside"
        )
        assertEquals(5, rect.left, "left=${rect.left}")
        assertEquals(395, rect.top, "top=${rect.top}")
        assertEquals(205, rect.right, "right=${rect.right}")
        assertEquals(430, rect.bottom, "bottom=${rect.bottom}")
    }
}
