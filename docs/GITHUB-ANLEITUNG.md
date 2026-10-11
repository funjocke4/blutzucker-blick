# GitHub – Schritte für die Veröffentlichung

1. Auf GitHub ein Repository mit dem Namen **blutzucker-blick** erstellen. Bei GitHub `Public` erst nach den Freigabeprüfungen wählen; andernfalls zunächst `Private` verwenden.
2. Keine zusätzliche README, `.gitignore` oder Lizenz generieren: alle drei sind bereits vorhanden.
3. Den **Inhalt des entpackten Projektordners** hochladen (nicht nur die ZIP-Datei). `.jks`, `.keystore`, Passwörter, APKs und Gesundheitsdaten nie hochladen.
4. `README.md`, `LICENSE` und `THIRD_PARTY_NOTICES.md` im Repository prüfen.
5. GitHub Security/Private Vulnerability Reporting aktivieren.
6. Nach Abschluss der Freigabechecks einen Tag `v1.3.4-build45` setzen und einen Release mit einer extern erstellten, signierten APK und SHA-256-Prüfsumme erstellen.

Wenn kein GitHub-Repository besteht, muss es zunächst von einem berechtigten Konto angelegt werden. Der Quellcode in diesem ZIP wurde noch **nicht** automatisch veröffentlicht.
