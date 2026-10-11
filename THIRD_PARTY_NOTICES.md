# Hinweise zu Drittanbieter-Komponenten

Laut `android/app/build.gradle.kts` verwendet das Projekt:

- AndroidX Core 1.13.1
- AndroidX Activity 1.9.3
- AndroidX CameraX (Core, Camera2, Lifecycle, View) 1.5.3

Diese Bibliotheken werden über Google Maven/Maven Central geladen und stehen grundsätzlich unter Apache License 2.0. Eine Kopie des Lizenztexts liegt in `licenses/Apache-2.0.txt`.

**Vor öffentlicher Binärverteilung** müssen die konkret aufgelösten Versionsartefakte (einschließlich transitiver Abhängigkeiten) und ihre Notices anhand des tatsächlichen Gradle-Builds überprüft werden. Diese Liste ist keine vollständige SBOM.

Die aktuelle Ziffernerkennung ist selbst geschriebener Kotlin-Code. Frühere Abhängigkeiten zu ML Kit/Tesseract sind in dieser Variante nicht enthalten.
