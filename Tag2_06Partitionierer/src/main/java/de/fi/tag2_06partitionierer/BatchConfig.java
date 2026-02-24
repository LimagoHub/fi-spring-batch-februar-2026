package de.fi.tag2_06partitionierer;




import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.H2PagingQueryProvider;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.BeanWrapperFieldExtractor;
import org.springframework.batch.item.file.transform.DelimitedLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.Map;

@Configuration
public class BatchConfig {

    @Bean
    public ColumnRangePartitioner partitioner(JdbcOperations jdbcTemplate,@Value("tbl_personen") String tableName, @Value("id") String column ) {
        return new ColumnRangePartitioner(jdbcTemplate, tableName, column);
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Person> reader(DataSource dataSource,
                                               @Value("#{stepExecutionContext['minValue']}") Long minValue,
                                               @Value("#{stepExecutionContext['maxValue']}") Long maxValue) {

        JdbcPagingItemReader<Person> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setFetchSize(100);
        reader.setRowMapper((rs, i) -> new Person(rs.getInt("id"), rs.getString("first_name"), rs.getString("last_name")));

        H2PagingQueryProvider queryProvider = new H2PagingQueryProvider();
        queryProvider.setSelectClause("id, first_name, last_name");
        queryProvider.setFromClause("tbl_personen");
        queryProvider.setWhereClause("id >= " + minValue + " AND id <= " + maxValue);
        queryProvider.setSortKeys(Map.of("id", Order.ASCENDING));

        reader.setQueryProvider(queryProvider);
        return reader;
    }

    @Bean
    @StepScope
    public FlatFileItemWriter<Person> writer(@Value("#{stepExecutionContext['minValue']}") String min) {
        FlatFileItemWriter<Person> writer = new FlatFileItemWriter<>();
        writer.setResource(new FileSystemResource("outputs/persons_part_" + min + ".csv"));
        writer.setAppendAllowed(false);
        writer.setLineAggregator(new DelimitedLineAggregator<>() {{
            setDelimiter(",");
            setFieldExtractor(new BeanWrapperFieldExtractor<>() {{
                setNames(new String[]{"id", "firstName", "lastName"});
            }});
        }});
        return writer;
    }

    @Bean
    public ItemProcessor<Person, Person> identityProcessor() {
        return item -> item;   // Identity
    }

    @Bean
    public Step workerStep(JobRepository jobRepository, PlatformTransactionManager txManager, DataSource ds, ItemProcessor<Person, Person> identityProcessor   ) {
        return new StepBuilder("workerStep", jobRepository)
                .<Person, Person>chunk(100, txManager)
                .reader(reader(ds, null, null))
                .processor(identityProcessor())
                .writer(writer(null))
                .build();
    }

    @Bean
    public Step masterStep(JobRepository jobRepository, PlatformTransactionManager txManager, Step workerStep, ColumnRangePartitioner partitioner) {
        return new StepBuilder("masterStep", jobRepository)
                .partitioner(workerStep.getName(), partitioner)
                .step(workerStep)
                .gridSize(4) // Erzeugt 4 parallele Partitionen
                .taskExecutor(new SimpleAsyncTaskExecutor())
                .build();
    }



    @Bean
    public Step mergeStep(JobRepository jobRepository, PlatformTransactionManager txManager) {
        return new StepBuilder("mergeStep", jobRepository)
                .tasklet(new FileMergingTasklet(), txManager)
                .build();
    }

    @Bean
    public Job partitionJob(JobRepository jobRepository, Step masterStep, Step mergeStep) {
        return new JobBuilder("partitionJob", jobRepository)
                .start(masterStep) // Zuerst parallel arbeiten
                .next(mergeStep)   // Dann Dateien zusammenführen
                .build();
    }
/*
    @Bean
    public Job partitionJob(JobRepository jobRepository, Step masterStep) {
        return new JobBuilder("partitionJob", jobRepository)
                .start(masterStep)
                .build();
    }
    */

}