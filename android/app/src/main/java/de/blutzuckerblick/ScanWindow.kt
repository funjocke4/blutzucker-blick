// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

/** Camera preview stays 40% shorter, but the scan box must cover WHOLE LCD digits. */
internal object ScanWindow {
    const val CAMERA_HEIGHT_FRACTION = 0.60f
    // About 13% narrower than 0.60; preserve height so complete LCD digits remain visible.
    const val TARGET_WIDTH = 0.52f
    const val TARGET_HEIGHT = 0.39f

    data class Center(val x: Float = .5f, val y: Float = .58f)
    data class Area(val left: Int, val top: Int, val right: Int, val bottom: Int) {
        val width get() = right - left
        val height get() = bottom - top
    }

    /** Rectangle in rotation-corrected camera image coordinates. */
    fun digitArea(imageWidth: Int, imageHeight: Int, center: Center = Center()): Area {
        require(imageWidth > 0 && imageHeight > 0)
        val halfW = TARGET_WIDTH / 2f
        val halfH = TARGET_HEIGHT / 2f
        val cx = center.x.coerceIn(halfW, 1f - halfW)
        val cy = center.y.coerceIn(halfH, 1f - halfH)
        return Area(
            ((cx - halfW) * imageWidth).toInt(),
            ((cy - halfH) * imageHeight).toInt(),
            ((cx + halfW) * imageWidth).toInt(),
            ((cy + halfH) * imageHeight).toInt()
        )
    }
}
