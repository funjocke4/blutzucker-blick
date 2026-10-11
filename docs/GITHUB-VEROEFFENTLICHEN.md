# Blutzucker-Blick Build 46 auf GitHub veröffentlichen

## 1. Neues GitHub-Repository anlegen

Auf https://github.com/new ein neues Repository mit dem Namen `blutzucker-blick` anlegen.

- Für einen späteren öffentlichen Quellcode-Stand `Public` wählen. Für die noch ausstehende rechtliche Freigabe kann das Repository zunächst `Private` bleiben.
- **Keine** zusätzliche README, `.gitignore` oder Lizenz von GitHub erstellen; diese Dateien liegen im Projekt bereits vor.
- Die Repository-Adresse `https://github.com/DEIN-BENUTZERNAME/blutzucker-blick.git` kopieren.

## 2. Quelldateien unter openSUSE hochladen

Dieses ZIP vollständig entpacken und eine Konsole im Ordner `Blutzucker-Blick-Build46-GitHub-Publikation` öffnen.

```bash
git init -b main
git add .
git status --short
git commit -m "Blutzucker-Blick 1.3.4 Build 46 – GPLv3 source"
git remote add origin https://github.com/DEIN-BENUTZERNAME/blutzucker-blick.git
git push -u origin main
```

`DEIN-BENUTZERNAME` durch den eigenen GitHub-Namen ersetzen. Für den Push bei GitHub anmelden; bei HTTPS authentifiziert man sich nicht mit dem normalen GitHub-Kontopasswort. Falls `git commit` nach einer Identität fragt, lokal `git config user.name ...` und `git config user.email ...` verwenden; diese Informationen sind später in Git-Commits öffentlich sichtbar.

**Vor `git push` `git status --short` prüfen.** Niemals private `.jks`-, `.keystore`-, APK- oder Messwertdateien hinzufügen. Der `.gitignore` schützt vor typischen Artefakten, ersetzt aber keine Kontrolle.

## 3. Repository prüfen

Im Browser kontrollieren: `README.md`, `LICENSE`, `android/`, `branding/` und `docs/` müssen sichtbar sein. In GitHub unter Settings > General ist ggf. der Repository-Sichtbarkeitsstatus anzupassen.

## WICHTIG: Noch keine APK veröffentlichen

Der Quellcode ist eine nicht klinisch validierte Entwicklungsfassung. Die medizinprodukterechtliche Einstufung und die weiteren Prüfungen aus `docs/VEROEFFENTLICHUNG-CHECKLISTE.md` sind offen. Eine öffentliche APK, medizinische Freigabe oder CE-Kennzeichnung darf daraus nicht ohne die nötigen Schritte abgeleitet werden. Öffentliche GitHub-Issues dürfen keine Gesundheitsdaten enthalten. Der private Signaturschlüssel darf niemals ins Repository gelangen.
