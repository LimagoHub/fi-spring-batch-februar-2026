package de.fi.second;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component

public class AppRunner implements CommandLineRunner {

    public AppRunner(final JobLauncher jobLauncher, final Job job) {
        this.jobLauncher = jobLauncher;
        this.job = job;
    }

    private final JobLauncher jobLauncher;
    private final Job job;

    @Override
    public void run(final String... args) throws Exception {
        final int anzahlSteps = 7;
        //boolean b = false;

        System.out.println( "\nJoblauf mit Job-Parameter anzahlSteps=" + anzahlSteps + ":" );
        JobExecution je = jobLauncher.run( job,
                new JobParametersBuilder()

                        .addString(
                            TaskletJobConfiguration.ANZAHLSTEPS_KEY, "" + anzahlSteps )
                        .toJobParameters() );
        for( StepExecution se : je.getStepExecutions() ) {
            System.out.println("StepExecution " + se.getId() + ": StepName = " + se.getStepName() +
                    ", CommitCount = " + se.getCommitCount());
        }
    }
}
