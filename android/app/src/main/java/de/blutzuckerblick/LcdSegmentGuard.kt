// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

/**
 * Conservatively checks the upper side segments of a 7-segment LCD glyph.
 * OCR often confuses 3 and 5. If the *visible* upper-left versus upper-right
 * segment strongly contradicts the OCR label, reject that proposal rather
 * than silently changing the health measurement.
 *
 * This guard intentionally does not claim to fully decode every LCD digit.
 * Measurements must still be confirmed by the user before being stored.
 */
internal object LcdSegmentGuard {
    fun contradictsOcr(
        number: String,
        left: Int, top: Int, right: Int, bottom: Int,
        darkPixel: (Int, Int) -> Boolean
    ): Boolean {
        if (number.length !in 2..3 || number.last() !in "35") return false
        val width = right - left
        val height = bottom - top
        // A too-small image cannot justify rejecting a recognized digit.
        if (width < number.length * 23 || height < 36) return false

        val digitLeft = left + (width.toFloat() * (number.length - 1) / number.length)
        val digitWidth = width.toFloat() / number.length
        fun darkFraction(x0: Float, x1: Float, y0: Float, y1: Float): Double {
            val xa = (digitLeft + digitWidth * x0).toInt().coerceIn(left, right - 1)
            val xb = (digitLeft + digitWidth * x1).toInt().coerceIn(xa + 1, right)
            val ya = (top + height * y0).toInt().coerceIn(top, bottom - 1)
            val yb = (top + height * y1).toInt().coerceIn(ya + 1, bottom)
            var active = 0
            var total = 0
            for (y in ya until yb step 2) {
                for (x in xa until xb step 2) {
                    if (darkPixel(x, y)) active++
                    total++
                }
            }
            return if (total == 0) 0.0 else active.toDouble() / total
        }
        // Upper-left (f) and upper-right (b) distinguish 5 from 3.
        val upperLeft = darkFraction(.03f, .29f, .20f, .39f)
        val upperRight = darkFraction(.71f, .97f, .20f, .39f)
        val looksLikeFive = upperLeft > .13 && upperLeft > upperRight * 2.1
        val looksLikeThree = upperRight > .13 && upperRight > upperLeft * 2.1
        return (number.last() == '3' && looksLikeFive) ||
            (number.last() == '5' && looksLikeThree)
    }
}
