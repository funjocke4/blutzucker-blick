# Große Ziffern – experimenteller OCR-Fix

Die zuvor verwendeten sich überlappenden Scanbänder enthielten teilweise Uhrzeit, Datum und mg/dL. In `SevenSegmentDigits.kt` wird zuerst die vertikale Zone der großen beleuchteten Ziffern ermittelt. Danach werden innerhalb dieser Zone benachbarte Ziffern einer plausiblen Messwertgruppe zugeordnet.

Die Erkennung ist **keine verlässliche medizinische Messung**. Der Wert muss vor dem Speichern immer mit dem Display des Messgeräts verglichen werden. Die App speichert nicht automatisch.

Bei fehlender Erkennung: WLAN ist nicht erforderlich; besseres Licht, weniger Spiegelung und das Messgerät möglichst stabil auf einer Fläche positionieren. Bei Fehlfunktionen Bluetooth-Import oder manuelle Eingabe verwenden.

**Teststatus**: Logiktests können lokal laufen; ein vollständiger Android-Build und der Live-Test am Samsung wurden hier nicht durchgeführt.
