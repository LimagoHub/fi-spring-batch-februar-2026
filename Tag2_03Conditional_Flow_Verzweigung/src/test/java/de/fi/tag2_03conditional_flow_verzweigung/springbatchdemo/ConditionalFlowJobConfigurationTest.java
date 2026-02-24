package de.fi.tag2_03conditional_flow_verzweigung.springbatchdemo;

import de.fi.tag2_03conditional_flow_verzweigung.Tag203ConditionalFlowVerzweigungApplication;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.JobInstance;
import org.springframework.batch.core.repository.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@SpringBootTest(
        classes = Tag203ConditionalFlowVerzweigungApplication.class,
        properties = {
                "spring.batch.job.name=jobAbc",
                "OK_ODER_FEHLER=ok",
                "spring.batch.job.enabled=true"
        }
)
class ConditionalFlowJobIntegrationTest {

    @Autowired
    private JobExplorer jobExplorer;

    @Test
    void testJobLiefErfolgreich() {
        // 1. Warten, bis der Job-Eintrag in der DB existiert
        await().atMost(5, TimeUnit.SECONDS).until(() ->
                jobExplorer.getLastJobInstance("jobAbc") != null
        );

        JobInstance lastInstance = jobExplorer.getLastJobInstance("jobAbc");

        // 2. Warten, bis die Execution nicht mehr den Status STARTING oder STARTED hat
        await().atMost(5, TimeUnit.SECONDS).until(() -> {
            JobExecution je = jobExplorer.getJobExecution(lastInstance.getInstanceId());
            return je != null && !je.isRunning();
        });

        JobExecution lastExecution = jobExplorer.getJobExecution(lastInstance.getInstanceId());

        System.out.println("Gefundener Job-Status: " + lastExecution.getStatus());
        Assertions.assertEquals(BatchStatus.COMPLETED, lastExecution.getStatus());
    }
}