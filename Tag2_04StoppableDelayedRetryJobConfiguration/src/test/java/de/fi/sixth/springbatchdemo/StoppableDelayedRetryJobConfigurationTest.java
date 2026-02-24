package de.fi.sixth.springbatchdemo;

import de.fi.sixth.SixthApplication;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = SixthApplication.class)
class StoppableDelayedRetryJobTest {

    @Autowired
    private JobExplorer jobExplorer;

    @Autowired
    @Qualifier("asyncJobOperator")
    private JobOperator asyncJobOperator;

    @Test
    void testWiederholungMitWartezeit() throws JobExecutionException {
        // 1. Job asynchron starten
        Long executionId = asyncJobOperator.start(
                StoppableDelayedRetryJobConfiguration.JOB_NAME,
                props("OK_ODER_FEHLER", "ok")
        );

        // 2. Mit Awaitility warten (ersetzt die problematische Hilfsmethode)
        await().atMost(10, TimeUnit.SECONDS).until(() -> {
            JobExecution je = jobExplorer.getJobExecution(executionId);
            return je != null && je.getStatus() == BatchStatus.COMPLETED;
        });

        // 3. Ergebnis prüfen
        JobExecution je = jobExplorer.getJobExecution(executionId);
        assertNotNull(je);
        assertEquals(BatchStatus.COMPLETED, je.getStatus());
        printStepExecutions(je);
    }

    @Test
    void testStoppable() throws Exception {
        // Job mit Fehlersimulation starten (damit er in den WarteStep geht)
        Long executionId = asyncJobOperator.start(
                StoppableDelayedRetryJobConfiguration.JOB_NAME,
                props("OK_ODER_FEHLER", "Fehler")
        );

        // Kurz warten, bis der Job läuft und im Warte-Loop ist
        Thread.sleep(500);

        // Stopp-Signal senden
        asyncJobOperator.stop(executionId);

        // Warten bis der Status STOPPED in der DB ankommt
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution je = jobExplorer.getJobExecution(executionId);
            return je != null && je.getStatus() == BatchStatus.STOPPED;
        });

        JobExecution je = jobExplorer.getJobExecution(executionId);
        assertEquals(BatchStatus.STOPPED, je.getStatus());
        System.out.println("Job erfolgreich gestoppt. Dauer: " +
                Duration.between(je.getStartTime(), je.getEndTime()).toMillis() + " ms");
    }

    // --- Hilfsmethoden ---

    /**
     * Erzeugt Properties aus Key-Value-Paaren für den JobOperator.
     */
    private static Properties props(String... kv) {
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("props braucht key/value Paare");
        }
        Properties p = new Properties();
        for (int i = 0; i < kv.length; i += 2) {
            p.setProperty(kv[i], kv[i + 1]);
        }
        return p;
    }

    private void printStepExecutions(JobExecution je) {
        for (StepExecution se : je.getStepExecutions()) {
            System.out.println("Step: " + se.getStepName() + " Status: " + se.getStatus());
        }
        System.out.println("Job-Status: " + je.getStatus());
    }
}