package de.fi.tag2_03conditional_flow_verzweigung.springbatchdemo;


import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
// mvn spring-boot:run -Dspring-boot.run.arguments=--okOderFehler=ok
@Component
public class JobAbcCommandLineRunner implements CommandLineRunner {

    private final JobLauncher jobLauncher;
    private final Job meinConditionalFlowJobAbc;

    public JobAbcCommandLineRunner(
            JobLauncher jobLauncher,
            Job meinConditionalFlowJobAbc
    ) {
        this.jobLauncher = jobLauncher;
        this.meinConditionalFlowJobAbc = meinConditionalFlowJobAbc;
    }

    @Override
    public void run(String... args) throws Exception {

        String okOderFehler = null;

        // Optional: Parameter aus der Command Line lesen
        for (String arg : args) {
            if (arg.startsWith("--okOderFehler=")) {
                okOderFehler = arg.substring("--okOderFehler=".length());
            }
        }

        JobParametersBuilder builder = new JobParametersBuilder()
                // Wichtig für neue JobInstance bei jedem Start
                .addLong("ts", System.currentTimeMillis());

        if (okOderFehler != null) {
            builder.addString(
                    ConditionalFlowJobConfiguration.OK_ODER_FEHLER,
                    okOderFehler
            );
        }

        JobParameters parameters = builder.toJobParameters();

        System.out.println("\n=== Starte JobAbc mit Parametern: " + parameters + " ===");

        JobExecution execution =
                jobLauncher.run(meinConditionalFlowJobAbc, parameters);

        System.out.println("=== Job beendet ===");
        System.out.println("Status:     " + execution.getStatus());
        System.out.println("ExitStatus: " + execution.getExitStatus());
    }
}