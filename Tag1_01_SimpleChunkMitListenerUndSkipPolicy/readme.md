# Seminar: Modern Spring Batch (Spring Boot 3.4+)
## Projekt 1: "First" – CSV-Import & Fault Tolerance

Dieses Dokument dient als Referenz für das erste Praxismodul. Es behandelt den modernen Standard der Batch-Verarbeitung unter Java 21.

---

## 1. Konfiguration (`application.properties`)

In Spring Boot 3 ist die Konfiguration über Properties zentral. Hier sind die batch-spezifischen Einträge:

| Property | Bedeutung |
| :--- | :--- |
| `spring.batch.jdbc.initialize-schema=always` | Erstellt beim Start automatisch die Framework-Tabellen (`BATCH_...`). In der Produktion steht dies meist auf `never`. |
| `spring.batch.job.enabled=false` | Verhindert den automatischen Start aller Jobs beim Booten. Ermöglicht gezieltes Triggern. |
| `spring.datasource.url=...;AUTO_SERVER=TRUE` | Erlaubt den gleichzeitigen Zugriff der App und der H2-Konsole auf die Datenbankdatei. |
| `spring.h2.console.enabled=true` | Aktiviert das Web-Interface zur DB unter `/h2`. |

---

## 2. Die Infrastruktur (Das Repository)

Das **JobRepository** ist das "Gedächtnis" von Spring Batch. Es sichert den Zustand der Verarbeitung persistent in der Datenbank.

* **Zustandsüberwachung:** Es speichert Startzeit, Endzeit, Status (COMPLETED, FAILED) und die Anzahl der gelesenen/geschriebenen Items.
* **Restart-Logik:** Tritt ein Fehler auf, nutzt Spring Batch die Metadaten im Repository, um beim nächsten Start exakt am letzten erfolgreichen Commit (Chunk) anzusetzen.
* **Framework-Tabellen:** Alle Tabellen beginnen mit dem Präfix `BATCH_` (z.B. `BATCH_JOB_EXECUTION`).

---

## 3. Die Kernkomponenten im Code



### Das Domänen-Modell (`Person`)
Wir nutzen POJOs oder moderne **Java Records**. Records sind ideal für Batch-Items, da sie kompakt und unveränderlich sind.

### Der Processor (`PersonItemProcessor`)
Hier findet die **Transformation** statt. 
* Implementiert `ItemProcessor<I, O>`.
* Ermöglicht die Filterung von Daten: Wird `null` zurückgegeben, wird das Item im aktuellen Step verworfen.

### Die Konfiguration (`BatchConfig`)
Ab Spring Batch 5.x ist die Konfiguration explizit und typsicher:

* **StepBuilder:** Erzeugt einen Arbeitsschritt. Er benötigt zwingend das `JobRepository` und einen `PlatformTransactionManager`.
* **Chunk-Prinzip:** Daten werden in Transaktionsblöcken verarbeitet (z.B. 10 Items). Schlägt ein Item fehl, rollt die gesamte Transaktion für diesen Chunk zurück.
* **Fault Tolerance (Fehlertoleranz):**
    * `.skip(FlatFileParseException.class)`: Definierte Fehler führen nicht zum Abbruch, sondern zum Überspringen des Datensatzes.
    * `.skipLimit(2)`: Maximale Anzahl erlaubter Fehler vor dem Job-Abbruch.

### Der Listener (`JobCompletionNotificationListener`)
Ein Hook-Mechanismus, um auf das Ende eines Jobs zu reagieren.
* **afterJob:** Wird aufgerufen, wenn der Job-Status feststeht. Ideal für Benachrichtigungen oder (wie in unserem Projekt) zur finalen Validierung der Datenbankinhalte via `JdbcTemplate`.

---

## 4. Wichtige Neuerungen in der Paketstruktur
Beim Refactoring von altem Code auf Spring Boot 3/5 müssen die Importe angepasst werden:

* **Step & Job:** Liegen nun flach unter `org.springframework.batch.core`.
* **Infrastruktur:** Das Paketsegment `.infrastructure` wurde in vielen Import-Pfaden entfernt (z.B. bei Datenbank-Providern).
* **Namespace:** Wechsel von `javax.*` zu `jakarta.*`.

---

## 5. Zusammenfassung für die Teilnehmer
1. Spring Batch benötigt zwingend eine Datenbank für seine Metadaten.
2. Chunk-Verarbeitung ist effizienter als Einzelverarbeitung, erfordert aber ein sauberes Transaktionsmanagement.
3. Die neue API verzichtet auf "Magie" (Factories) und setzt auf klare Injektion der Infrastruktur-Beans.