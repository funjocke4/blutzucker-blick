# Blutzucker-Blick – Android

**Version 1.3.4 · Build 46 (Basis Build 45) · Quellcode unter GNU GPLv3 (GPL-3.0-only)**

Blutzucker-Blick ist eine deutschsprachige Android-App zur **lokalen Dokumentation und grafischen Darstellung** von Blutzuckermesswerten.

> **Status:** Quellcode zur Veröffentlichungsprüfung vorbereitet. Noch keine medizinische Validierung oder Freigabe als Medizinprodukt; derzeit **keine freigegebene öffentliche Release-APK**. Eine Veröffentlichung der App zur Nutzung erfordert zusätzliche Prüfung der regulatorischen Einordnung sowie der verbleibenden Freigabefragen.

## Funktionen

- Messwerte manuell eingeben und lokal auf dem Handy speichern.
- Über Bluetooth Messwerte geeigneter kompatibler Geräte importieren. **Nicht jedes Bluetooth-Messgerät wird unterstützt.** Erfolgreich getestete Geräte können je nach Firmware/Android-Version abweichen.
- Große LCD-Ziffern mit der Offline-Livekamera erkennen; der Scanrahmen folgt den Ziffern. **Jeder vorgeschlagene Wert muss vor dem Speichern am Messgerät kontrolliert werden.**
- Tages-, Wochen-, Monats- und Jahresdiagramme seitlich verschieben; Grenzwerte einstellen.
- Letzten Wert mit Smiley und zusätzlichen Symbolen markieren: Bonbon bei Unterschreitung, Läufer bei Überschreitung der eingestellten Grenzwerte. **Die Symbole sind keine medizinischen Handlungsempfehlungen.**
- Geschätzten HbA1c aus vorliegenden Einzelmessungen anzeigen. **Die Schätzung ersetzt keinen Labortest und kann erheblich abweichen.**
- Messzeitpunkte auswählen: „Vor dem Essen“, „Nach dem Essen“, „Zwischendurch“, „Morgens nach dem Schlaf“.

Der aktuelle Android-Projektstand enthält **keinen KDE-/WLAN-Abgleich**, keine Cloud-OCR und keinen Einzelfoto-Modus.

## Bauen und testen

1. Android Studio mit Android SDK 35 und JDK 17 installieren.
2. Über **File → Open** den Unterordner `android/` öffnen und Gradle synchronisieren.
3. Testgerät anschließen und über **Run** starten; alternativ Debug-APK über **Build → Build APK(s)** erzeugen.
4. Zur manuellen Funktionsprüfung siehe `docs/TESTPLAN-BUILD45.md`.

Für veröffentlichte Release-APKs muss ein **eigener, sicher verwahrter Signaturschlüssel** verwendet werden. Die Schlüsseldatei und Passwörter gehören **niemals** in das öffentliche Repository. Eine APK aus dem Projekt wurde hier nicht gebaut oder signiert.

Die meisten Tests unter `tests/` sind Kotlin-Logiktests ohne Android-Emulator. Sie ersetzen keine Integrationstests mit echten Messgeräten.

## Datenschutz und Sicherheit

Messwerte werden im lokalen privaten App-Speicher abgelegt. Die Ziffernerkennung arbeitet offline. Es ist keine eigene Server-Synchronisierung enthalten. Siehe `docs/DATENSCHUTZ-ENTWURF.md` und `SECURITY.md`.

Veröffentliche in GitHub-Issues **keine** echten Messwerte, Fotos mit Seriennummern, Bluetooth-Geräteadressen, PINs oder andere sensible Daten.

## Keine medizinische Freigabe

Die App ist nicht klinisch validiert. Blutzucker-Messwerte und OCR-Vorschläge dürfen **nicht ungeprüft für Therapie- oder Medikamentenentscheidungen** verwendet werden. Auch die HbA1c-Schätzung darf nicht als Laborwert behandelt werden. Die mögliche Einstufung als Medizinprodukt nach EU-Recht muss anhand der konkreten Zweckbestimmung vor öffentlicher App-Verteilung geklärt werden; ein Haftungsausschluss ersetzt diese Prüfung nicht. Siehe `docs/MEDIZINPRODUKT-PRUEFUNG.md`.

## Lizenz

Der eigenständig entwickelte Projektcode steht unter **GNU General Public License Version 3, nur Version 3** (`GPL-3.0-only`). Vollständiger Lizenztext: `LICENSE`. AndroidX/CameraX und weitere Drittbestandteile haben ihre eigenen Lizenzen; siehe `THIRD_PARTY_NOTICES.md`.

**Neues Logo:** Eigenständig erstellt, mit editierbarer SVG unter `branding/` und Lizenzdokumentation in `docs/GRAFIK-RECHTE.md`. Das zuvor extern kopierte Logo wurde ausgetauscht.

Mitwirken: siehe `CONTRIBUTING.md`. Änderungen: `CHANGELOG.md`.
