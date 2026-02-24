package de.fi.fifth.batch;

import org.junit.jupiter.api.Test;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // Lädt den vollständigen ApplicationContext
@SpringBatchTest // Stellt JobLauncherTestUtils zur Verfügung
@ActiveProfiles("test")
class SplitFlowJobConfigurationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    @Test
    public void testSplitFlowJob() throws Exception {


        System.out.println("\nDie drei Steps step3, step4 und step5 werden im 'Split Flow' parallel ausgefuehrt:");

        // Job ausführen (JobLauncherTestUtils nutzt den injizierten Job automatisch)
        JobExecution je = jobLauncherTestUtils.launchJob(new JobParameters());

        System.out.println("");

        // Überprüfe Job-Ergebnis mit AssertJ
        assertThat(je.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(je.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);
        assertThat(je.getFailureExceptions()).isEmpty();

        Collection<StepExecution> stepExecutions = je.getStepExecutions();
        assertThat(stepExecutions).hasSize(7);

        double dauerSum = 0.0;

        for (StepExecution se : stepExecutions) {
            assertThat(se.getStatus()).isEqualTo(BatchStatus.COMPLETED);
            assertThat(se.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

            // Zeitberechnung mit Java 8+ Duration (Spring Batch 5 nutzt LocalDateTime)
            long millis = Duration.between(se.getStartTime(), se.getEndTime()).toMillis();
            double dauer = millis / 1000.0;
            dauerSum += dauer;

            System.out.printf("%s: von %s bis %s, Dauer: %.1f s%n",
                    se.getStepName(),
                    se.getStartTime().format(df),
                    se.getEndTime().format(df),
                    dauer);
        }

        System.out.printf("Summe der Steps: %.1f s%n", dauerSum);

        long jobMillis = Duration.between(je.getStartTime(), je.getEndTime()).toMillis();
        double dauerJob = jobMillis / 1000.0;
        System.out.printf("Gesamtdauer Job: %.1f s%n", dauerJob);

        // Beweis für Parallelität: Die Summe der Step-Dauern muss größer sein als die tatsächliche Zeit
        assertThat(dauerSum).isGreaterThan(dauerJob);
    }
}