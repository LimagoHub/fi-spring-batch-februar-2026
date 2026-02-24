package de.fi.tag2_03conditional_flow_verzweigung.springbatchdemo;


import org.springframework.batch.core.*;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.parameters.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;

import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
// @EnableBatchProcessing sollte in Spring Boot 3 i.d.R. entfernt werden!
public class ConditionalFlowJobConfiguration {

    public static final String OK_ODER_FEHLER = "OK_ODER_FEHLER";


    /*
    // --- DECIDER LOGIK ---
    public static class MeinEntscheider implements JobExecutionDecider {
        @Override
        public FlowExecutionStatus decide(JobExecution jobExecution, StepExecution stepExecution) {
            String parameter = (String) jobExecution.getJobParameters().getParameters().get(OK_ODER_FEHLER).getValue();

            if ("ok".equalsIgnoreCase(parameter)) {
//                return new FlowExecutionStatus("GEHE_ZU_OK");
            } else {
                return new FlowExecutionStatus("GEHE_ZU_FEHLER");
            }
        }
    }

    @Bean
    public JobExecutionDecider decider() {
        return new MeinEntscheider();
    }

    // --- JOB MIT DECIDER ---
    @Bean
    public Job meinConditionalFlowJobAbc(JobRepository jobRepository,
                                         Step arbeitsStep,
                                         JobExecutionDecider decider,
                                         Step fehlerbehandlungsStep,
                                         Step okStep,
                                         Step abschliessenderStep) {
        return new JobBuilder("jobAbc", jobRepository)
                .start(arbeitsStep)
                .next(decider) // Nach dem Arbeitsstep kommt der Entscheider
                .on("GEHE_ZU_OK").to(okStep)
                .from(decider).on("GEHE_ZU_FEHLER").to(fehlerbehandlungsStep)
                .from(okStep).next(abschliessenderStep)
                .from(fehlerbehandlungsStep).next(abschliessenderStep)
                .end()
                .build();
    }
}

     */


    /**
     * Tasklet: Zeigt Text im Kommandozeilenfenster
     */
    public static class PrintTextTasklet implements Tasklet {
        final String text;

        public PrintTextTasklet(String text) {
            this.text = text;
        }

        @Override
        public RepeatStatus execute(StepContribution sc, org.springframework.batch.core.scope.context.ChunkContext cc) throws Exception {
            System.out.println(text);
            return RepeatStatus.FINISHED;
        }
    }

    /**
     * Tasklet: "Geschaeftsprozess", entweder erfolgreich oder mit Fehler
     */
    public static class ArbeitsTasklet extends PrintTextTasklet {
        public ArbeitsTasklet(String text) {
            super(text);
        }

        @Override
        public RepeatStatus execute(StepContribution sc, org.springframework.batch.core.scope.context.ChunkContext cc) throws Exception {
            var stepContext = cc.getStepContext();
            System.out.println("\n---- Job: " + stepContext.getJobName() + ", mit JobParametern: " + stepContext.getJobParameters());

            String okOderFehler = (String) stepContext.getJobParameters().get(OK_ODER_FEHLER);

            if ((okOderFehler != null) ? !okOderFehler.equalsIgnoreCase("ok") : (Math.random() < 0.5)) {
                System.out.println(this.text + ": mit Fehler");
                throw new Exception("-- Dieser Fehler ist korrekt! --");
            }
            System.out.println(this.text + ": ok");
            return RepeatStatus.FINISHED;
        }
    }

    @Bean
    public Step arbeitsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("arbeitsStep", jobRepository)
                .tasklet(new ArbeitsTasklet("ArbeitsStep"), transactionManager)
                .build();
    }

    @Bean
    public Step fehlerbehandlungsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fehlerbehandlungsStep", jobRepository)
                .tasklet(new PrintTextTasklet("FehlerbehandlungsStep"), transactionManager)
                .build();
    }

    @Bean
    public Step okStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("okStep", jobRepository)
                .tasklet(new PrintTextTasklet("OkStep"), transactionManager)
                .build();
    }

    @Bean
    public Step abschliessenderStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("abschliessenderStep", jobRepository)
                .tasklet(new PrintTextTasklet("AbschliessenderStep"), transactionManager)
                .build();
    }

    /**
     * Job Abc: führt Steps im "Conditional Flow" aus
     */
    @Bean
    public Job meinConditionalFlowJobAbc(JobRepository jobRepository,
                                         Step arbeitsStep,
                                         Step fehlerbehandlungsStep,
                                         Step okStep,
                                         Step abschliessenderStep) {
        return new JobBuilder("jobAbc", jobRepository)
                // In Spring Batch 5 ist der RunIdIncrementer weiterhin nutzbar
                .incrementer(new RunIdIncrementer())
                .flow(arbeitsStep)
                .on(ExitStatus.FAILED.getExitCode()).to(fehlerbehandlungsStep).next(abschliessenderStep)
                .from(arbeitsStep).on("*").to(okStep).next(abschliessenderStep)
                .end()
                .build();
    }
}