package de.blutzuckerblick

fun main() {
    val votes112 = listOf(140, 175, 195).flatMap { exposure ->
        List(5) { ScanConsensus.Vote(112, .55f, exposure, "ok") }
    } + List(4) { ScanConsensus.Vote(111, .95f, 220, "wrong") }
    check(ScanConsensus.choose(votes112)?.value == 112)
    val votes145 = listOf(
        ScanConsensus.Vote(145, .29f, 195, "ok"),
        ScanConsensus.Vote(145, .29f, 220, "ok"),
        ScanConsensus.Vote(145, .29f, 220, "ok"),
        ScanConsensus.Vote(146, .18f, 220, "wrong"),
        ScanConsensus.Vote(146, .18f, 220, "wrong")
    )
    check(ScanConsensus.choose(votes145)?.value == 145)
    check(ScanConsensus.choose(listOf(ScanConsensus.Vote(112,.95f,220,"maybe"))) == null)
    check(ScanConsensus.choose(listOf(ScanConsensus.Vote(112,.95f,220,"maybe"),ScanConsensus.Vote(112,.95f,220,"maybe"),ScanConsensus.Vote(112,.95f,220,"maybe"))) == null)
    println("OK: consensus picks 112 and 145; rejects an unconfirmed guess")
}
