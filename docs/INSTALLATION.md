# Installations- und Build-Anleitung

## Entwicklung / privater Test

1. Android Studio starten, Unterordner `android/` öffnen.
2. Android SDK 35 und JDK 17 bereitstellen; Gradle synchronisieren.
3. Android-Gerät anschließen und **Run** wählen oder über **Build → Build APK(s)** eine Debug-APK erzeugen.

## Release-APK

1. **Build → Generate Signed Bundle / APK → APK** wählen.
2. Eigenen Release-Keystore an einem sicheren, nicht mit dem Projekt synchronisierten Speicherort erzeugen/verwahren.
3. `release` wählen und APK signieren.
4. APK auf einem Testgerät installieren, berechtigungsbezogene Funktionen testen und einen SHA-256-Hash berechnen.
5. Vor öffentlicher Verteilung alle Freigabeprüfungen in `VEROEFFENTLICHUNG-CHECKLISTE.md` erledigen.

**Update:** Android akzeptiert Updates nur bei übereinstimmender Signatur und geeignetem Versionscode. Eine Deinstallation löscht normalerweise die lokalen Messwerte; vorher sichern.

Keinesfalls `.jks`, Passwörter, private Diagnosedaten oder Patientenmesswerte veröffentlichen.
