# Zweckbestimmung – Blutzucker-Blick

**Produkt:** Blutzucker-Blick  
**Software:** Android, Version 1.3.4, Build 46  
**Lizenz des eigenen Projektcodes:** GNU GPLv3 (GPL-3.0-only)  
**Stand:** 11. Oktober 2026  
**Status:** Entwurf zur fachlichen und regulatorischen Prüfung; keine behördliche Einstufung

## 1. Beabsichtigter Zweck

Blutzucker-Blick ist als persönliche Anwendung zur **Dokumentation, lokalen Speicherung, übersichtlichen Darstellung und beschreibenden statistischen Auswertung** bereits ermittelter Blutzuckermesswerte vorgesehen.

Nutzer sollen ihre Messwerte eigenständig erfassen, zeitlich ordnen und persönliche Verläufe nachvollziehen können. Die Anwendung misst den Blutzucker nicht selbst.

**Nicht beabsichtigt** ist eine Verwendung zur medizinischen Diagnose, klinischen Patientenüberwachung, Erkennung oder Behandlung eines medizinischen Zustands oder zur Unterstützung therapeutischer Entscheidungen. Insbesondere werden keine Insulindosierungen oder Medikamentenanpassungen berechnet oder empfohlen.

Diese Erklärung beschreibt die Absicht des Projekts. Ob die vorhandenen Funktionen dennoch unter MDR oder IVDR fallen, ist gesondert zu prüfen.

## 2. Zielgruppe und Nutzungskontext

Vorgesehen sind Privatpersonen, die eigene Blutzuckermesswerte als digitales Tagebuch dokumentieren möchten. Ein Einsatz als professionelles klinisches Überwachungs- oder Alarmierungssystem ist nicht vorgesehen.

Die Anwendung setzt ein Android-Gerät voraus. Bluetooth-Import ist auf technisch kompatible Messgeräte begrenzt; nicht jedes Gerät ist unterstützt.

## 3. Tatsächlich vorhandene Funktionen in Build 46

### 3.1 Erfassung und Speicherung
- Manuelle Eingabe von Messwerten in **mg/dL**.
- Import gespeicherter Werte über Bluetooth von kompatiblen Blutzuckermessgeräten.
- Offline-Livekamera zur Erkennung großer LCD-Ziffern einschließlich automatisch nachgeführtem Suchrahmen; vor der Übernahme müssen vorgeschlagene Werte am Messgerät geprüft und bestätigt werden.
- Zuordnung von Datum, Uhrzeit und Messsituation: **Vor dem Essen**, **Nach dem Essen**, **Zwischendurch** und **Morgens nach dem Schlaf**.
- Speicherung im privaten lokalen Anwendungsbereich des Android-Geräts; keine eigene Cloud- oder KDE-/WLAN-Synchronisation.

### 3.2 Darstellung und persönliche Auswertung
- Tages-, Wochen-, Monats- und Jahresansichten mit horizontal verschiebbaren Verlaufsgrafiken.
- Statistische Angaben zum gewählten Zeitraum: Anzahl, Durchschnitt, Minimum und Maximum.
- Anzeige der letzten Messwerte und Hervorhebung des zuletzt erfassten Werts.
- Frei einstellbare untere und obere Grenzen zur **optischen Kennzeichnung** in Diagramm und Statusanzeige.
- Smiley-Symbol für die Lage eines Werts relativ zu diesen Grenzen. Unter der Untergrenze wird zusätzlich ein **Bonbon (🍬)**, über der Obergrenze zusätzlich ein **Läufer (🏃)** dargestellt. Diese Symbole sind **keine Aufforderung**, Zucker einzunehmen oder körperlich aktiv zu werden. Ihr mögliches Missverständnis ist Gegenstand der Risikoprüfung.

### 3.3 Rechnerische HbA1c-Schätzung
- Anzeige einer mathematischen HbA1c-Näherung aus verfügbaren Einzelmessungen der letzten 90 Tage.
- Ergänzende Anzeige der Datenbasis, Mess- und Messtage, Durchschnitt und monatlicher Näherungen.
- Die Schätzung ist **kein gemessener Labor-HbA1c**; bei unvollständigen oder ungleichmäßig verteilten Messungen können erheblich abweichende Ergebnisse entstehen.

### 3.4 Bedienung und Unterstützung
- Hilfe, allgemeine Bluetooth-Verbindungsanleitung, Suche und Verbindung kompatibler Geräte, erneuter Abruf von Messwerten sowie ein Bluetooth-Diagnoseprotokoll.
- Keine Einzelfoto-Funktion und keine Internet-basierte Texterkennung im aktuellen Build.

## 4. Ausdrücklich nicht vorgesehene Verwendungen

Nicht vorgesehen sind:
- Diagnosen oder medizinische Bewertung akuter Blutzuckerzustände;
- Überwachung oder Alarmierung in medizinisch kritischen Situationen;
- Therapieplanung, Insulindosierung, Medikamentenänderung oder Empfehlungen zur Zuckeraufnahme beziehungsweise körperlichen Aktivität;
- Steuerung eines Blutzuckermessgeräts, einer Insulinpumpe oder anderer Therapiegeräte;
- Ersatz ärztlicher Bewertung, zugelassener Messgeräte oder von Laboruntersuchungen.

## 5. Grenzen, Fehlerrisiken und Schutzmaßnahmen

Die Qualität der Dokumentation hängt von den zugrunde liegenden Messungen und der korrekten Datenübernahme ab. Insbesondere können Kameraerkennung, manuelle Eingabe, Bluetooth-Import, Gerätezeit, Zuordnung und Auswertungsalgorithmen fehlerhafte oder unvollständige Daten liefern.

Die Bestätigung kamerabasiert erkannter Werte reduziert das Risiko, beseitigt es jedoch nicht. Optische Grenzkennzeichnungen und HbA1c-Schätzungen können medizinisch missverstanden werden. Die Darstellung von 🍬 und 🏃 erfordert deshalb eine besondere Prüfung auf unbeabsichtigte Behandlungshinweise.

Medizinische Entscheidungen dürfen nicht auf diese Dokumentations- und Schätzergebnisse gestützt werden.

## 6. Datenschutz und Systemgrenzen

Build 46 speichert Messwerte lokal und analysiert Kamerabilder offline. Er enthält keine selbst entwickelte Cloud-Synchronisierung und keine eigene Telemetrie. Mögliche Verarbeitungen oder Sicherungen durch Betriebssystem, Plattformdienste und eingebundene Komponenten sind separat zu untersuchen. Bei Fehlerberichten dürfen keine echten Gesundheitsdaten, Bluetooth-Adressen, Seriennummern oder PINs veröffentlicht werden.

## 7. Stand der regulatorischen Prüfung

Die Einstufung als **Nicht-Medizinprodukt**, Software nach **MDR (EU) 2017/745** oder Software im Umfeld von **IVDR (EU) 2017/746** ist **offen**. Entscheidend sind die tatsächlichen Funktionen, die vorgesehene und vorhersehbare Nutzung sowie die Gesamtdarstellung.

Die Ausweisung eines ausschließlich dokumentarischen Zwecks und Warnhinweise reichen **nicht allein** aus, um regulatorische Anforderungen auszuschließen. Insbesondere sind HbA1c-Berechnung, Grenzwertbewertung und die zusätzlichen Bonbon-/Läufer-Symbole unabhängig fachlich zu beurteilen.

Dieses Dokument ist ein **Arbeitsentwurf für eine Einstufungsanfrage**, keine CE-Zertifizierung und keine Freigabe zur allgemeinen Verteilung einer installierbaren APK. Siehe ergänzend `docs/FUNKTIONS_UND_RISIKOBESCHREIBUNG.md` und `docs/MEDIZINPRODUKT-PRUEFUNG.md`.

**Orientierung:** MDCG 2019-11 Rev. 1 (Juni 2025), offizielle EU-Leitlinien: https://health.ec.europa.eu/medical-devices-sector/new-regulations/guidance-mdcg-endorsed-documents-and-other-guidance_en
