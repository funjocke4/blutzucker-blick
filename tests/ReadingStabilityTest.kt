package de.blutzuckerblick

fun main() {
    // Normal: three readings, separated by 500 ms each.
    val r = ReadingStability()
    check(!r.offer(135, 1000) && r.matches == 1)
    check(!r.offer(135, 1500) && r.matches == 2)
    check(r.offer(135, 2000) && r.matches == 3)

    // Motion blur does not erase earlier matches, even with several blank frames.
    r.reset()
    check(!r.offer(112, 10_000))
    repeat(5) { check(!r.offer(null, 10_300L + 300L * it)) }
    check(!r.offer(112, 12_000) && r.matches == 2)
    check(r.offer(112, 13_000))

    // A single contradictory read can be tolerated (75% consensus), but never
    // accepted automatically without a separate confirmation screen.
    r.reset()
    check(!r.offer(135, 20_000))
    check(!r.offer(133, 20_500))
    check(!r.offer(135, 21_000))
    check(r.offer(135, 21_500))

    // Too many contradictory values prevent positive confirmation.
    r.reset()
    check(!r.offer(135, 30_000))
    check(!r.offer(133, 30_500))
    check(!r.offer(133, 31_000))
    check(!r.offer(135, 31_500))
    check(!r.offer(135, 32_000)) // 3/5 = 60% < required 75%

    // No detection can complete a scan; stale positives age out.
    r.reset()
    check(!r.offer(112, 40_000))
    check(!r.offer(112, 40_500))
    check(!r.offer(null, 41_000))
    check(!r.offer(null, 49_200) && r.matches == 1) // first expired
    check(!r.offer(null, 50_000) && r.matches == 0)
    check(!r.offer(112, 52_000) && r.matches == 1)

    // Touch-to-reposition clears old readings so the next screen starts fresh.
    r.reset()
    check(!r.offer(112, 60_000))
    check(!r.offer(112, 60_500))
    r.reset()
    check(!r.offer(135, 61_000) && r.matches == 1)
    check(!r.offer(135, 61_500) && r.matches == 2)
    check(r.offer(135, 62_000))

    // No two near-identical consecutive samples are considered three readings.
    r.reset()
    check(!r.offer(112, 70_000))
    check(!r.offer(112, 70_100))
    check(!r.offer(112, 70_200)) // min span 550 ms
    println("Handbewegungstoleranz und 75%-Konsens: OK")
}
