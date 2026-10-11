## 1.3.4 – Build 46 (Logo-Wechsel auf Basis Build 45)

- Bisheriges extern kopiertes Blutstropfen-Logo vollständig durch eigenständige GPLv3-Grafik ersetzt.
- Editierbare SVG und transparente PNG-Vorschau unter `branding/` hinzugefügt.
- Bildbeschreibung „Blauer Blutstropfen“ in „Roter Blutstropfen“ korrigiert.
- Interner Versionscode auf 46 erhöht, um eine Aktualisierung über Build 45 zu ermöglichen.
- **Keine Änderungen an Kameraerkennung, Bluetooth, Datenmodell, Diagrammen oder Grenzwert-Symbolen.**

# Änderungsverlauf

**Aktueller Stand: Version 1.3.4 / Build 45.** Die Angaben zu älteren Builds unten dokumentieren historische Entwicklungsschritte, nicht den aktuellen Funktionsumfang.

## Versionscode 45 – Bonbon- und Läufer-Symbol bei „Letzter Wert“

- Unter der individuell festgelegten Untergrenze: bisheriger Smiley plus 🍬.
- Über der individuell festgelegten Obergrenze: bisheriger Smiley plus 🏃.
- Innerhalb des Grenzwertbereichs: bisheriger Smiley ohne Zusatzsymbol.
- Beide Icons einschließlich Smiley 20 % größer als zuvor: RelativeSizeSpan von 2,0 auf 2,4.
- Exakte Grenzwertvergleiche: am Grenzwert selbst kein Zusatzsymbol.
- Anzeigehinweis: Die Symbole dienen ausschließlich der Orientierung, insbesondere ist 🏃 keine Aufforderung zu Sport bei erhöhtem Blutzucker.
- Keine Änderungen an Bluetooth, Kamera, Diagramm, Datenformat oder Messwertspeicherung.

## Versionscode 44 – Diagnose im Menü „Bluetooth & Messwerte“

- „Diagnose“ von der Hauptansicht in „Menü → Bluetooth & Messwerte → Diagnose“ verschoben.
- Diagnoseprotokoll bleibt im Hintergrund erhalten und zeigt auch während des geöffneten Dialogs neue Bluetooth-Ereignisse an.
- Bluetooth-Import, große LCD-Ziffernerkennung, Diagramm und lokale Messwerte unverändert.

## Versionscode 43 – Geräteneutrale Bluetooth-Verbindungsanleitung

- Unter „Bluetooth & Messwerte“ den Eintrag „GL49-Verbindungsanleitung“ zu „Verbindungsanleitung“ umbenannt.
- Dialogtitel und Hinweise für verschiedene Bluetooth-Messgeräte neutral formuliert.
- Die bewährte Erkennung großer LCD-Ziffern (Versionscode 42) und die übrigen Funktionen unverändert.

## Versionscode 41 – Automatisch nachgeführter Display-Rahmen

- Engere dynamische Markierung nach der ersten erkannten Messwertzifferngruppe.
- Bewegungsnachführung per lokalem Graustufen-Bildvergleich (ohne zusätzliche Bibliotheken).
- Nach dem Einrasten wird nur der Ziffernbereich ausgewertet; Beurer-Bluetooth-Blinksymbol darunter ausgeschlossen.
- Bei verlorenem Fokus Rückkehr zum breiten Suchrahmen; Tippen auf Ziffern weiterhin möglich.
- Einzelfoto bleibt entfernt. Speichern ausschließlich nach editierbarer Kontrolle.
- Separater Bewegungstest einschließlich wechselndem Blinksymbol.

## Android 1.3.4 – Handbewegungstoleranz (interner Versionscode 40)

- Beurer-Livekamera: OCR-Bilder ohne JPEG-Zwischenkomprimierung aus dem Y-Kanal.
- Versuche ungefähr alle 500 ms statt 1000 ms; tatsächliche Frequenz abhängig vom Telefon.
- Gültige Kandidaten innerhalb eines gleitenden 9-Sekunden-Fensters zusammenfassen.
- Drei Übereinstimmungen und mindestens 75 % Konsens der erfolgreichen Kandidaten erforderlich.
- Kurzzeitige Unschärfe leert den Zähler nicht; neue Rahmenposition setzt ihn zurück.
- Kein Einzelfoto, keine Änderung am Bluetooth- oder Diagrammcode.
- Manuelle Bestätigung weiterhin zwingend erforderlich. Keine klinische Validierung.

## Versionscode 39 – Nur Livekamera, kein Einzelfoto
- Button „Einzelfoto“ in der Live-Kamera entfernt; nur „Abbrechen“ bleibt.
- Alten Fotoaufnahme-, Zuschneide- und Dateifreigabe-Code entfernt.
- Hilfetexte entsprechend angepasst.
- Live-Scan und die Bestätigung eines erkannten Messwerts bleiben unverändert.
- Keine Änderungen an der lokalen Messwertspeicherung.

## Versionscode 38 – Schmalerer Kamerarahmen und tolerierte Aussetzer
- Gestrichelter Scanrahmen 52 % statt 60 % Bildbreite, gleiche Höhe (39 %).
- Live-Auswertung ungefähr alle 1,0 statt 1,2 Sekunden.
- Prüfung von drei identischen POSITIVEN Werten toleriert bis zu zwei fehlende Frames; andere erkannte Werte setzen zurück.
- Prüfdialog bleibt verbindlich; keine automatische Speicherung.
- Weitere Funktionen und GPLv3-Lizenz unverändert.

## Korrektur des Kamerarahmens – Versionscode 36

- Sichtbare Kamera weiterhin 40 % niedriger, Scan-ROI unabhängig davon vergrößert.
- Vollständiger Ausschnitt der LCD-Ziffern statt nur der mittleren Ziffernhälften.
- Tippen auf die Zahl verschiebt den Scanrahmen und setzt gleichzeitig den Kamerafokus.
- Stabilitätspuffer wird beim Verschieben zurückgesetzt.
- Mindesthöhe erkannter Segmente für den größeren Rahmen angepasst.

# Änderungen

## Kamera-/Ziffernfenster auf 60 % (Android versionCode 35; v1.3.4)

- Live-Kamerafenster in der Höhe um 40 % reduziert und zentriert, Bildauflösung gleich gelassen.
- Analysierter Ziffernbereich sowohl horizontal als auch vertikal auf 60 % der bisherigen Größe begrenzt.
- Gestrichelte Zielmarkierung an die tatsächliche OCR-Region und das Center-Crop-Mapping gekoppelt.
- Alle zuvor vorhandenen Funktionen und der Bestätigungsdialog bleiben bestehen.
- Keine Garantie auf bessere Erkennung: auf Samsung-Gerät testen und Werte manuell prüfen.

## Ziffernscan-Update (Android versionCode 34; sichtbarer Stand v1.3.4)

- Menü: Zusatzerklärungen "noch nichts gegessen" und "bereits gegessen" aus den zwei Auswahloptionen entfernt.
- Live-Kamera: enge Ziffern-Zielzone, sieben-Segment-Erkennung ohne beliebige Datumstexte, unsichere Treffer verwerfen.
- Einzelfoto: Foto anzeigen, Auswahlrechteck frei mit dem Finger über die großen Ziffern ziehen, danach offline lesen; manuelle Eingabe bleibt.
- Tesseract- und ML-Kit-Abhängigkeit entfallen, vollständig lokale Kotlin-Segmentprüfung.
- Der Wert muss immer bearbeitbar geprüft und ausdrücklich bestätigt werden.
- Der Datenbestand und die übrigen Funktionen der Version 1.3.4 werden nicht verändert.

## Experimentelle Segment-Mehrfachprüfung (Versionscode 37)
- Live-Scan prüft 9 überlappende Höhenbereiche bei 4 Helligkeitsstufen und verlangt einen klaren Mehrheitsvorsprung.
- Rahmenstandard ist etwas weiter unten, um kleine Datums-/Uhrzeitangaben zu vermeiden; mit Fingertipp verstellbar.
- Kameraansicht bleibt 40 % niedriger; Bluetooth und alle anderen Funktionen bleiben erhalten.
- Die Live-Erkennung ist weiterhin experimentell; keine automatische Speicherung ohne Bestätigung.

### OCR: Große LCD-Ziffern, Testbuild 42
- Vor dem Segmentlesen wird die lange, helle Ziffernzeile ermittelt. Kleine Datums-/Uhrzeit-Zeichen und mg/dL-Beschriftungen werden damit aus dem Auslesebereich entfernt.
- Die Gruppierung ignoriert Randartefakte (z. B. weißes Accu-Chek-Gehäuse, Pfeil, Statusanzeigen); eine widersprüchliche Zifferngruppe wird verworfen.
- Offline-Test anhand zweier existierender Screenshots: Beurer 135 mg/dL, Accu-Chek 112 mg/dL. Kein Android-Gerätetest durchgeführt.
- Es bleibt bei der bearbeitbaren Messwert-Bestätigung vor dem Speichern.
