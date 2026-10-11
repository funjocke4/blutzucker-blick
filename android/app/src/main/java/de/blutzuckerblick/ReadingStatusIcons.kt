// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

/** Reine Anzeigesymbole, keine medizinischen Handlungsempfehlungen. */
internal object ReadingStatusIcons {
    fun forReading(value: Double, lower: Double, upper: Double, smiley: String): String {
        return when {
            value < lower -> "$smiley 🍬"
            value > upper -> "$smiley 🏃"
            else -> smiley
        }
    }
}
