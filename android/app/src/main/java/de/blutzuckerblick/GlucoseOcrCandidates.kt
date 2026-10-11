// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

/** Filters OCR proposals; never automatically saves a recognized medical value. */
internal object GlucoseOcrCandidates {
    data class Token(val text: String, val area: Int, val confidence: Float)

    private val glucoseNumber = Regex("""(?<![0-9])([0-9]{2,3})(?![0-9])""")

    fun rank(tokens: List<Token>, fallbackText: String): List<Int> {
        val ranking = linkedMapOf<Int, Double>()
        fun add(text: String, area: Int, confidence: Float) {
            val normalized = text.replace('O', '0').replace('o', '0')
                .replace('I', '1').replace('l', '1')
            for (match in glucoseNumber.findAll(normalized)) {
                val glucose = match.value.toIntOrNull() ?: continue
                if (glucose !in 20..600) continue
                // Wie in der bisherigen Erkennung werden große Displayziffern bevorzugt.
                val confidenceFactor = 0.5 + confidence.coerceIn(0f, 100f) / 200.0
                val score = area.coerceAtLeast(1) * confidenceFactor
                ranking[glucose] = maxOf(ranking[glucose] ?: 0.0, score)
            }
        }
        for (token in tokens) add(token.text, token.area, token.confidence)
        if (ranking.isEmpty()) add(fallbackText, 1, 0f)
        return ranking.entries.sortedByDescending { it.value }.map { it.key }.take(8)
    }
}
