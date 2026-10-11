package de.blutzuckerblick

fun main() {
    check(ReadingStatusIcons.forReading(69.0, 70.0, 180.0, "😐") == "😐 🍬")
    check(ReadingStatusIcons.forReading(45.0, 70.0, 180.0, "😟") == "😟 🍬")
    check(ReadingStatusIcons.forReading(70.0, 70.0, 180.0, "😊") == "😊")
    check(ReadingStatusIcons.forReading(180.0, 70.0, 180.0, "😊") == "😊")
    check(ReadingStatusIcons.forReading(181.0, 70.0, 180.0, "😐") == "😐 🏃")
    check(ReadingStatusIcons.forReading(220.0, 70.0, 180.0, "😲") == "😲 🏃")
    // Individuell angepasste Grenzwerte werden unmittelbar berücksichtigt.
    check(ReadingStatusIcons.forReading(90.0, 100.0, 140.0, "😐") == "😐 🍬")
    check(ReadingStatusIcons.forReading(150.0, 100.0, 140.0, "😐") == "😐 🏃")
    println("OK: low=candy, target=smiley, high=runner; bounds are strict")
}
