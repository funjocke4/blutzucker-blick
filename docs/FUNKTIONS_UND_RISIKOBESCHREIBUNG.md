# Funktions- und Risikobeschreibung – Blutzucker-Blick, Build 46

**Version:** 1.3.4 / Build 46  
**Stand:** 11. Oktober 2026  
**Status:** Vorläufiger Arbeitsentwurf für die medizinprodukterechtliche Einstufung. **Keine** abgeschlossene Risikoanalyse nach ISO 14971, **keine** klinische/Leistungsbewertung und **keine** Freigabe.

**Bezug:** [Zweckbestimmung](ZWECKBESTIMMUNG.md), [Medizinprodukterechtliche Prüfung](MEDIZINPRODUKT-PRUEFUNG.md), [Testplan](TESTPLAN-BUILD45.md)

## 1. Vorgesehener Einsatz und Systemgrenzen

Blutzucker-Blick dient nach beabsichtigter Zweckbestimmung der persönlichen Dokumentation und beschreibenden Auswertung vorhandener Messwerte. Der Android-Code enthält lokale Speicherung, manuelle Eingabe, Bluetooth-Import, Offline-Livekameraziffernerkennung, Zeitraumdiagramme, individuell einstellbare Grenzwerte mit Symbolen und mathematische HbA1c-Näherungen.

Nicht zum System gehören ein eigener Messsensor, eine Medikamenten- oder Insulinsteuerung, eine klinisch validierte Alarmfunktion, ein Backend oder eine eigene Cloud-Synchronisation.

### Wesentliche Datenwege

1. Manuelle Eingabe → Nutzereingabe prüfen → lokaler Datensatz.
2. Bluetooth-Messgerät → empfangene BLE-Glucose-Daten → Dekodierung/Zuordnung → lokaler Datensatz.
3. Kamerabild → LCD-Ziffern-Vorschlag → Sichtprüfung und Bestätigung durch Nutzer → lokaler Datensatz.
4. Lokale Datensätze → Periodenfilter, Statistik, Diagramm, optische Grenzmarkierung, HbA1c-Näherung.

## 2. Risiken und erforderliche Nachweise

Die folgende Tabelle benennt **mögliche Fehler und Fehlverwendungen**. Häufigkeit, Schwere, Gesamtrisiko und Wirksamkeit der Maßnahmen sind **noch nicht bestimmt**. „Vorhanden“ bedeutet lediglich eine aus dem Quellcode ersichtliche Maßnahme, nicht eine validierte Risikoreduktion.

| ID | Funktion / Gefährdungssituation | Mögliche Folge | Bestehende Maßnahme / Prüfung | Offener Nachweis bzw. zusätzliche Maßnahme |
|---|---|---|---|---|
| R01 | OCR verwechselt LCD-Ziffern (z. B. 135 → 133) | Falsch gespeicherter Messwert, fehlerhafte Verlaufsauswertung | Prüfung/Bestätigung vor dem Speichern; wiederholte Ziffernerkennung | Testmatrix für Beurer GL49, Accu-Chek Instant und unterschiedliche Beleuchtung, Bewegung, Ziffernformen; Fehlerraten und Grenzfälle dokumentieren |
| R02 | Unscharfes Bild, blinkendes Bluetooth-Symbol, abgeschnittene Ziffern | Falscher oder fehlender Vorschlag | Nachgeführter Ziffernrahmen, Stabilitätsprüfungen, manueller Eingabepfad | Reale Hardwaretests und dokumentiertes Verhalten bei unzureichender Bildqualität |
| R03 | Bluetooth-Daten falsch dekodiert oder Gerät inkompatibel | Falscher Wert, falsche Einheit oder Datensatz | BLE-Importcode, Diagnosefunktion | Referenzvergleich Messgerät ↔ Datensatz; Einheiten, Uhrzeit, Kontext, Fehlercodes und Firmwarevarianten testen |
| R04 | Zeitstempel falsch, Daten doppelt oder Import unvollständig | Irreführende zeitliche Darstellung/Statistik | Lokale Speicherung und vorhandene Importlogik | Tests von Gerätesynchronisation, Zeitzonen, Sommerzeit, Dubletten und Verbindungsabbrüchen |
| R05 | Manuell falscher Wert oder falsche Messsituation | Unzutreffende Dokumentation | Eingabe-/Auswahldialog | Plausibilität, Korrekturmöglichkeiten und Bedienbarkeit prüfen |
| R06 | Benutzerdefinierte Grenzwerte oder Smiley werden als medizinische Risikoeinstufung verstanden | Falsches Sicherheitsgefühl oder unnötige Sorge | Hinweis „optische Darstellung“, manuell gesetzte Grenzen | Verständlichkeitstest und Prüfung, ob Funktion medizinische Überwachung/Entscheidung unterstützt |
| R07 | 🍬 bei niedrigem Wert als Aufforderung zur Zuckeraufnahme gedeutet | Ungeeignetes Verhalten aufgrund eines ungeprüften Werts | Symbol nur als Darstellung vorgesehen | Besondere regulatorische/usability-basierte Bewertung; gegebenenfalls neutrale Symbole statt Handlungsassoziationen |
| R08 | 🏃 bei hohem Wert als Aufforderung zu Sport gedeutet | Potenziell riskantes Verhalten je nach Gesundheitszustand | Symbol nur als Darstellung vorgesehen | Besondere regulatorische/usability-basierte Bewertung; gegebenenfalls neutrale Symbole statt Handlungsassoziationen |
| R09 | HbA1c aus unregelmäßigen Einzelmessungen weicht von Labor-HbA1c ab | Medizinische Fehlinterpretation | Als „geschätzt“ beschriftet; Warnhinweis | Algorithmus und Grenzen mit unabhängigen Beispieldatensätzen prüfen; regulatorischen Status klären |
| R10 | Softwarefehler, Absturz oder Datenverlust | Unvollständiges Tagebuch | Android-App mit lokalem Speicher | Test auf Neustart, Updates, Speicherfehler, Backup/Restore; Datenerhalt und Versionsmigration prüfen |
| R11 | Unbeabsichtigte Offenlegung sensibler Gesundheitsdaten | Verletzung der Vertraulichkeit | Lokaler App-Speicher, keine eigene Cloudfunktion | Berechtigungen, Android-Backups, Logs, Diagnoseausgaben und Drittbibliotheken im Release-Build prüfen |
| R12 | Nutzer verwechselt Tagebuch-App mit validiertem Medizinprodukt | Handeln aufgrund unzuverlässiger Ausgabe oder ausbleibender Warnung | Begrenzung der Zweckbestimmung und Hinweise | Gesamtdarstellung, App-Store-Text, Anleitung und UI fachlich prüfen; Einstufung nach MDR/IVDR klären |

## 3. Vorläufige Kontroll- und Verifikationsplanung

- **OCR:** Repräsentative Testbilder und Live-Kameratests auf mehreren Android-Geräten, vor allem unter Bewegung, Reflexionen und unterschiedlichen Displays; erkannte Werte immer mit Originalanzeige abgleichen.
- **Bluetooth:** Unterstützte Messgeräte samt Firmware/Android-Version dokumentieren; Referenzwerte, Zeitstempel, Einheit, Duplikate und Wiederanläufe prüfen.
- **Statistik und Diagramm:** Randfälle ohne Messwerte, einzelne Messwerte, Zeitzonen, Tages-/Wochen-/Monats-/Jahresgrenzen und Grenzwertänderungen testen.
- **HbA1c:** Formel, Datenauswahl, Rundung, Aussagegrenzen und Beispiele unabhängig nachrechnen; verhindern, dass das Ergebnis als Laborwert dargestellt wird.
- **Symbole:** Gezielt testen, ob Nutzer 🍬 oder 🏃 als Verhaltensempfehlung auffassen; Bewertung vor öffentlicher APK-Verteilung abschließen.
- **Datenschutz/Sicherheit:** AndroidManifest, native/Java-Bibliotheken, Netzwerkzugriffe, Android-Systembackups, Logs und lokale Speicherung am tatsächlich signierten Release-Build überprüfen.
- **Release:** Reproduzierbarer Build, Signierung, Installations- und Updatetest, GPL-/Drittanbieter-Lizenzen, vollständige Nutzerhinweise und nachvollziehbare Testergebnisse.

Alle Tests müssen mit **synthetischen** oder zuvor anonymisierten Daten dokumentiert werden. Echte Nutzer-Messwerte, Adressen, Geräte-PINs und Seriennummern dürfen nicht in öffentliche Testprotokolle gelangen.

## 4. Ausstehende regulatorische Bewertung

Die app-spezifische Einstufung bleibt offen. Für die Anfrage werden benötigt:

1. Die [Zweckbestimmung](ZWECKBESTIMMUNG.md) in einer vom Entwickler bestätigten Endfassung.
2. Diese Funktions- und Risikobeschreibung, die App-Oberfläche und ggf. ein kurzes Demonstrationsvideo mit Testdaten.
3. Die genaue Erläuterung von Grenzsymbolen, HbA1c-Formel und deren vorhersehbarer Verwendung.
4. Eine begründete Ersteinschätzung zur Abgrenzung von bloßer Dokumentation gegenüber medizinischer Entscheidungs- oder Überwachungssoftware sowie MDR versus IVDR.
5. Gegebenenfalls schriftliche fachliche Stellungnahme und behördliches Einstufungsverfahren.

Weder die gewünschte Dokumentations-Zweckbestimmung noch Warnhinweise ersetzen die Einstufung. Bis zu deren Klärung und der erforderlichen Freigabeprüfung **keine öffentliche Release-APK als regulär nutzbares Gesundheitsprodukt ausgeben**.

**Regulatorische Orientierung:** [EU-MDCG-Leitlinien](https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en), insbesondere MDCG 2019-11 Rev. 1 (Software Qualification and Classification).

**Hinweis:** Das Dokument ist bewusst ein **Risikoinventar ohne errechnete Risikoklassen**, kein Nachweis eines vollständig durchgeführten Risikomanagement-Prozesses.
