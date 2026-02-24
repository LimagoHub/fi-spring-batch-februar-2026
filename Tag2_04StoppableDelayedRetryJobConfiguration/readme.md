# Delayed Retry Flow mit manueller Stopp-Möglichkeit (Spring Batch 5.2)

## Überblick

Dieser Job implementiert einen **Retry-Mechanismus mit Verzögerung
(Delayed Retry)** sowie einer **manuellen Stop-Möglichkeit**.

Die Architektur basiert auf einem **zustandsbasierten Flow mit
ExitStatus-Routing**, nicht auf einer linearen Step-Kette.

------------------------------------------------------------------------

# 1. Architektur des Jobs

Der Job verwendet **konditionales Routing auf Basis von `ExitStatus`**.

## Logischer Ablauf

avoidDuplicateRun\
↓\
ersterStep\
├── SUCCESS (\*) → zweiterStep\
└── FAILED → warteStep\
      ├── SUCCESS → ersterStep (Retry!)\
      └── FAILED → fehlerbehandlungsStep

## Step-Beschreibung

Step                    Aufgabe
  ----------------------- ------------------------------------------------
avoidDuplicateRun       Prüft, ob der Job bereits läuft → ggf. Abbruch
ersterStep              Enthält die eigentliche Geschäftslogik
warteStep               Retry-Mechanismus mit Delay
zweiterStep             Weiterverarbeitung bei Erfolg
fehlerbehandlungsStep   Endgültige Fehlerbehandlung

------------------------------------------------------------------------

# 2. Zentrale Komponenten & Tasklets

## A. StoppableTasklet & isStopped()

Das Herzstück für die Reaktionsfähigkeit des Jobs.

Ein normales `Tasklet` kann nicht einfach unterbrochen werden.\
Deshalb wird `StoppableTasklet` verwendet.

### Zwei-Wege-Check zur Abbrucherkennung

1.  **Volatile Boolean**
    -   Wird vom `JobOperator` gesetzt
    -   Sofortige Reaktion im laufenden Thread
2.  **JobExplorer-Abfrage**
    -   Prüft, ob `JobExecution` den Status `STOPPING` hat
    -   Sicherste Variante (DB-basierte Prüfung)

------------------------------------------------------------------------

## B. WarteTasklet (Delayed Retry)

Das `WarteTasklet` implementiert die Retry-Verzögerung.

Statt:

``` java
Thread.sleep(1000);
```

wird verwendet:

10 × 100ms Intervalle

### Vorteil

-   In jedem Intervall wird `isStopped()` geprüft
-   Reaktion auf Stop-Signal innerhalb von \~100ms
-   Kein „Blockieren" für eine volle Sekunde

### Retry-Logik

-   Prüft `ANZ_WIEDERHOL`
-   Wenn max. Versuche erreicht → `FAILED`
-   Sonst → `COMPLETED` und Rücksprung zu `ersterStep`

------------------------------------------------------------------------

## C. JobExecutionListener

Funktion: **Zusammenfassungs-Generator**

Wird nach dem Job ausgeführt (`afterJob()`).

### Aufgaben:

-   Sammelt `ExitDescription` aller Steps
-   Konsolidiert Statusmeldungen
-   Setzt finale Beschreibung im globalen `JobExecution`

------------------------------------------------------------------------

# 3. Flow-Steuerung (DSL)

Die Steuerung erfolgt über `on()` und `from()` im `JobBuilder`.

``` java
.start(avoidDuplicateRun)
.next(ersterStep)
    .on("FAILED").to(warteStep)
        .from(warteStep).on("FAILED").to(fehlerbehandlungsStep)
        .from(warteStep).on("*").to(ersterStep)
```

## Verhalten

-   `FAILED` in `ersterStep` beendet **nicht** den Job
-   Stattdessen Wechsel in `warteStep`
-   Optionaler Rücksprung (Loop)
-   Ergebnis: Zustandsautomat mit Retry

------------------------------------------------------------------------

# 4. Besonderheiten in Spring Batch 5.2

## Infrastruktur

-   `JobRepository` und `TransactionManager` werden explizit injiziert
-   Builder-basierte Konfiguration ist Standard

## Typisierung

``` java
jobParameters.getLong("ANZ_WIEDERHOL");
```

## Spring Boot 3

-   `@EnableBatchProcessing` ist optional
-   Manuelle Bean-Definition per Builder empfohlen

------------------------------------------------------------------------

# 5. Test-Parameter (Steuerung von außen)

  -----------------------------------------------------------------------
Parameter                        Beschreibung
  -------------------------------- --------------------------------------
OK_ODER_FEHLER                   Wenn "fehler" → ersterStep liefert
absichtlich FAILED

ANZ_STEP_FEHL_OK                 Simulation: Nach X Fehlversuchen wird
Step erfolgreich

ANZ_WIEDERHOL                    Maximale Anzahl an Retry-Versuchen
-----------------------------------------------------------------------

------------------------------------------------------------------------

# 6. Infrastruktur-Hinweis

-   Datenbank muss StepExecution schnell schreiben
-   JobExplorer darf keine veralteten Daten lesen
-   Sonst verzögerte Stop-Erkennung möglich

------------------------------------------------------------------------

# Zusammenfassung

Dieser Job implementiert:

-   Zustandsbasierten Retry-Flow\
-   Verzögertes Wiederholen mit hoher Reaktionsfähigkeit\
-   Manuelle Stop-Möglichkeit\
-   Konsolidierte Status-Auswertung\
-   Steuerung über JobParameters

Geeignet für:

-   Externe Systemabhängigkeiten\
-   Temporäre Störungen\
-   Integrationsjobs mit instabilen Schnittstellen
    @startuml
    title Delayed Retry Flow mit manueller Stopp-Möglichkeit (Spring Batch)

skinparam backgroundColor white
skinparam shadowing false
skinparam roundcorner 12
skinparam ArrowColor #333333
skinparam ActivityBorderColor #333333
skinparam ActivityFontColor #111111
skinparam ActivityBackgroundColor #F7F7F7

start

:avoidDuplicateRun\n(prüft laufende JobExecution);
if (Job läuft bereits?) then (ja)
:Abbruch / Stop;\nExitStatus=STOPPED/FAILED;
stop
else (nein)
:ersterStep\n(eigentliche Arbeit);
endif

if (ersterStep ExitStatus == FAILED?) then (ja)
repeat
:warteStep\nDelayed Retry (10x100ms)\nprüft isStopped();
if (Stop-Signal erkannt?) then (ja)
:ExitStatus=STOPPED\nExitDescription += "Stop erkannt";
stop
endif

    :Retry-Zähler prüfen\nANZ_WIEDERHOL erreicht?;
repeat while (noch Versuche übrig?) is (ja)

:fehlerbehandlungsStep\n(zu viele Versuche);
stop
else (nein)
:zweiterStep\n(Weiterverarbeitung);
stop
endif

@enduml