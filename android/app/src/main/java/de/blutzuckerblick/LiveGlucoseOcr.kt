// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

import android.graphics.Bitmap
import android.graphics.Rect

/** No native OCR dependency. Read only the large LCD segments in the target box. */
internal class LiveGlucoseOcr(@Suppress("UNUSED_PARAMETER") context: android.content.Context) : AutoCloseable {
    data class Detection(val mgdl: Int, val bounds: Rect, val confidence: Float)
    fun detect(frame: Bitmap, center: ScanWindow.Center = ScanWindow.Center()): Detection? {
        // Wide and tall enough to include each FULL LCD digit; user may reposition it.
        val target = ScanWindow.digitArea(frame.width, frame.height, center)
        return detect(frame, Rect(target.left, target.top, target.right, target.bottom))
    }
    /** The tracked area is deliberately limited to the large LCD digits;
     * status icons and the blinking Bluetooth symbol are outside it. */
    fun detect(frame: Bitmap, area: Rect): Detection? {
        val result = SevenSegmentDigits.detect(frame, area) ?: return null
        // Very weak matches are often small date digits or incomplete LCD glyphs.
        if (result.confidence < .28f) return null
        return Detection(result.mgdl,result.bounds,result.confidence)
    }
    override fun close() { }
}
