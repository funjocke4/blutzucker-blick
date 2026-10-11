package de.blutzuckerblick
fun main() {
    val tests = listOf(
        GlucoseOcrCandidates.Token("136", 2200, 92f),
        GlucoseOcrCandidates.Token("23", 150, 75f),
        GlucoseOcrCandidates.Token("12:30", 150, 65f),
        GlucoseOcrCandidates.Token("2OO", 400, 60f)
    )
    val got = GlucoseOcrCandidates.rank(tests, "")
    check(got.first()==136) { "Highest priority unexpected: $got" }
    check(200 in got)
    check(23 in got)
    check(GlucoseOcrCandidates.rank(emptyList(), "147 mg/dl") == listOf(147))
    check(GlucoseOcrCandidates.rank(emptyList(), "") == emptyList<Int>())
    println("OK: $got, Fallback and empty passed")
}
