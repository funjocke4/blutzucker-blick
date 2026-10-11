# Freigabe-Checkliste: Blutzucker-Blick Build 46 (auf Basis Build 45)

## Quellcode
- [x] Version 1.3.4, interner `versionCode = 46` für Update; App-Code stammt aus bestätigtem Build 45, nur Logo und Bildbeschreibung verändert.
- [x] GNU GPLv3-Lizenztext, README und Versionshinweise vorhanden.
- [x] Quellpaket auf bekannte Zugangsschlüssel, persönliche Messdaten und Signaturdateien untersucht.
- [x] Scan-Code läuft ohne Google-ML-Kit/Tesseract-Bibliothek.
- [x] Extern kopiertes Projektlogo entfernt und durch neu erstelltes GPLv3-Logo mit SVG-Quelldatei ersetzt.
- [ ] Autorenrechte/GPL-Freigabe und Notices aller Bibliotheken abschließend klären.

## Release für Dritte (APK)
- [ ] Vollständiger Build 46 (auf Basis Build 45) in Android Studio und reproduzierbare Build-Anleitung validiert.
- [ ] Funktionale und Sicherheitstests an unterschiedlichen Android-Geräten bestanden.
- [ ] Datenschutzbeschreibung am fertigen Release verifiziert.
- [ ] Medizinprodukterechtliche Einstufung und mögliche Anforderungen nach MDR/IVDR fachkundig geprüft.
- [ ] Release-APK mit privatem Schlüssel signiert (Schlüssel bleibt privat).
- [ ] SHA-256 der APK, Versionsnummer und Teststand dokumentiert.

**Status:** Nicht als öffentlich freigegebene APK kennzeichnen, bevor offene Punkte erledigt sind. Keine medizinische Eignung/CE behaupten.
