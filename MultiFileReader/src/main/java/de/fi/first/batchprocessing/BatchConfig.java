package de.fi.first.batchprocessing;

import de.fi.first.entity.Person;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
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





    @Bean
    @StepScope
    public FlatFileItemReader<Person> reader() {
        return new FlatFileItemReaderBuilder<Person>()
                .name("personItemReader")

                .delimited()
                .names("firstName", "lastName", "age")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
                    setTargetType(Person.class);
                }})
                .build();


    }

    @Bean
    public MultiResourceItemReader<Person> multiResourceReader(@Value("classpath:input/*.csv") Resource[] inputResources ) {
        return new MultiResourceItemReaderBuilder<Person>()
                .name("multiResourceReader")
                .delegate(reader())
                .resources(inputResources)
                .build();
    }
    // Dummy Processor
    public ItemProcessor<Person, Person> processor() {
        return person -> {
            System.out.println("Verarbeite: " + person.getFirstName() + " " + person.getLastName());
            return person;
        };
    }

    // Dummy Writer
    public ItemWriter<Person> writer() {
        return items -> {
            for (Person p : items) {
                System.out.println("Schreibe: " + p);
            }
        };
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      MultiResourceItemReader<Person> multiResourceReader) {
        return new StepBuilder("step1", jobRepository)
                .<Person, Person>chunk(2, transactionManager)
                .reader(multiResourceReader)

                .processor(processor())
                .writer(writer())
                .build();
    }
    @Bean
    public Job importUserJob(JobRepository jobRepository, Step step1, Step clearTableStep) {
        return new JobBuilder("importUserJob", jobRepository)


                .start(step1)
                //.end()
                .build();
    }





}
