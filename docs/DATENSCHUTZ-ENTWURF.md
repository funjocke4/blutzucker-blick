# Datenschutz – Entwurf, vor öffentlicher Bereitstellung zu prüfen

- Messwerte werden lokal in der privaten Android-App-Speicherung gespeichert (SharedPreferences/JSON).
- Kameraaufnahmen der Live-Ziffernerkennung werden lokal im Arbeitsspeicher analysiert. Das aktuelle Projekt enthält keine externe OCR- oder Cloud-Synchronisierung.
- Bluetooth-Zugriff erfolgt für das Auffinden und Auslesen kompatibler Messgeräte.
- Die Quelldateien und die App-Konfiguration enthalten keine von uns beabsichtigte Analyse-/Werbe-Telemetrie.
- Es ist noch zu prüfen, welche Daten Android, eingebundene Bibliotheken, Betriebssystem-Sicherung und Gerätehersteller ggf. verarbeiten. Eine umfassende Datenschutzerklärung muss anhand eines fertigen Release-Builds und aller Berechtigungen erarbeitet werden.

**Datensicherheit:** Messwerte sind Gesundheitsdaten. Nicht ungeprüft in öffentliche Fehlerberichte, Bildschirmfotos oder Cloud-Sicherungen übermitteln. Kein Medizinprodukt-Zulassungs- oder Datenschutzgutachten wird durch diesen Entwurf ersetzt.
