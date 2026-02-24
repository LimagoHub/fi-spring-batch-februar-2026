package de.fi.sixth.springbatchdemo;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.support.SimpleJobOperator;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

@Configuration
public class JobOperatorConfig {

    @Bean(name = "asyncJobOperator")
    public JobOperator asyncJobOperator(
            JobExplorer jobExplorer,
            JobRepository jobRepository,
            JobRegistry jobRegistry,
            JobLauncher jobLauncher
    ) throws Exception {

        SimpleJobOperator op = new SimpleJobOperator();
        op.setJobExplorer(jobExplorer);
        op.setJobRepository(jobRepository);
        op.setJobRegistry(jobRegistry);
        op.setJobLauncher(jobLauncher);
        op.afterPropertiesSet();
        return op;
    }

    // Optional: Async JobLauncher falls du wirklich asynchron starten willst
    //@Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher launcher = new TaskExecutorJobLauncher();
        launcher.setJobRepository(jobRepository);
        launcher.setTaskExecutor(new SimpleAsyncTaskExecutor("batch-"));
        launcher.afterPropertiesSet();
        return launcher;
    }
}