package de.fi.first.batchprocessing;


import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.SystemCommandTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.MultiResourceItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.MultiResourceItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchConfig {

    private final JobRepository repository;
    private final PlatformTransactionManager transactionManager;

    public BatchConfig(final JobRepository repository, final PlatformTransactionManager transactionManager) {
        this.repository = repository;
        this.transactionManager = transactionManager;
    }

    @Bean
    @JobScope
    public Step step1(Tasklet task1) {

        return new StepBuilder("step1", repository).tasklet( task1, transactionManager ).build();
    }

    @Bean
    @StepScope
    public SystemCommandTasklet task1() {
        SystemCommandTasklet tasklet = new SystemCommandTasklet();

        
        tasklet.setCommand( "cmd","/c","copy-rename.bat");
        tasklet.setWorkingDirectory("./src/main/resources/script");
        tasklet.setTimeout(5000);

        return tasklet;
    }

    @Bean

    public Job meinTaskletJob(Step step1) throws Exception
    {


        return new JobBuilder("meinTaskJob", repository).incrementer( new RunIdIncrementer() )
                .start( step1 )

                .build();
    }


}
