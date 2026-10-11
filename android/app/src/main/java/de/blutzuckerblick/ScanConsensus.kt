// SPDX-License-Identifier: GPL-3.0-only
package de.blutzuckerblick

/** Band agreement is a *filter* for unreliable OCR, not a medical accuracy guarantee. */
internal object ScanConsensus {
    data class Vote<T>(val value: Int, val confidence: Float, val exposure: Int, val payload: T)

    fun <T> choose(votes: List<Vote<T>>): Vote<T>? {
        val groups = votes.filter { it.confidence >= .14f }
            .groupBy { it.value }
            .filterValues { list -> list.size >= 3 && list.map { it.exposure }.distinct().size >= 2 }
        if (groups.isEmpty()) return null
        val ranked = groups.map { (value, items) ->
            Triple(value, items.sumOf { it.confidence.toDouble() }, items)
        }.sortedByDescending { it.second }
        val winner = ranked.first()
        val others = votes.filter { it.value != winner.first }
            .groupBy { it.value }
            .values.maxOfOrNull { list -> list.sumOf { it.confidence.toDouble() } } ?: 0.0
        // A few highly overlapping scan windows must not defeat contradictory evidence.
        if (winner.second < 0.7 || winner.second < others * 1.7) return null
        return winner.third.maxByOrNull { it.confidence }
    }
}
