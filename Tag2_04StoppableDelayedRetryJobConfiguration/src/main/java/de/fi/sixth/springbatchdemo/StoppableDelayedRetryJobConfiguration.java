package de.fi.sixth.springbatchdemo;

import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Set;

@Configuration
public class StoppableDelayedRetryJobConfiguration {

    public static final String JOB_NAME = "jobStoppableDelayedRetry";
    public static final String OK_ODER_FEHLER = "OK_ODER_FEHLER";
    public static final String ANZ_WIEDERHOL = "ANZ_WIEDERHOL";
    public static final String ANZ_STEP_FEHL_OK = "ANZ_STEP_FEHL_OK";
    public static final String ERGEBNIS_OK = "Ergebnis: OK";
    public static final String ERGEBNIS_FEHLER = "Ergebnis: Fehler";
    public static final String ABBRUCH_WEIL_JOB_BEREITS_LAEUFT = "Abbruch weil Job bereits laeuft";
    public static final String STOPP_SIGNAL_DURCH_STOPPABLETASKLET = "Stopp-Signal durch StoppableTasklet";

    /**
     * In Spring Batch 5 werden die Infrastruktur-Beans als Parameter in die @Bean-Methoden gereicht.
     */

    @Bean
    public Step avoidDuplicateRun(JobRepository jobRepository, PlatformTransactionManager transactionManager, JobExplorer jobExplorer) {
        return new StepBuilder("avoidDuplicateRun", jobRepository)
                .tasklet((sc, cc) -> {
                    String jobName = cc.getStepContext().getJobName();
                    Set<JobExecution> jes = jobExplorer.findRunningJobExecutions(jobName);
                    if (jes.size() > 1) {
                        String exitDescription = ABBRUCH_WEIL_JOB_BEREITS_LAEUFT;
                        System.out.println("\n !!! " + exitDescription + " !!!");
                        sc.setExitStatus(new ExitStatus(ExitStatus.FAILED.getExitCode(), exitDescription));
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager).build();
    }

    // --- Tasklet Klassen (Logik bleibt weitgehend gleich) ---

    public class PrintTextTasklet implements Tasklet {
        final String text;
        public PrintTextTasklet(String text) { this.text = text; }

        @Override
        public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
            System.out.println(text);
            ExecutionContext ec = cc.getStepContext().getStepExecution().getJobExecution().getExecutionContext();
            String msg = ec.getString("Msg", "");
            ec.put("Msg", (msg.isEmpty() ? "" : msg + ", ") + text);
            return RepeatStatus.FINISHED;
        }
    }

    public class StoreTextAndPrintTextTasklet extends PrintTextTasklet {
        final String storeText;
        public StoreTextAndPrintTextTasklet(String printText, String storeText) {
            super(printText);
            this.storeText = storeText;
        }
        @Override
        public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
            super.execute(sc, cc);
            sc.setExitStatus(sc.getExitStatus().addExitDescription(storeText));
            return RepeatStatus.FINISHED;
        }
    }

    public class StoppablePrintTextTasklet extends PrintTextTasklet implements StoppableTasklet {
        volatile boolean stopped = false;
        private final JobExplorer jobExplorer;

        public StoppablePrintTextTasklet(String text, JobExplorer jobExplorer) {
            super(text);
            this.jobExplorer = jobExplorer;
        }

        @Override public void stop() { stopped = true; }
        public void resetStopped() { stopped = false; }

        public boolean isStopped(StepContribution sc, StepExecution se) {
            if (stopped) {
                stopped = false;
                sc.setExitStatus(sc.getExitStatus().addExitDescription(STOPP_SIGNAL_DURCH_STOPPABLETASKLET));
                return true;
            }
            // Datenbank-Abfrage via JobExplorer
            se = jobExplorer.getStepExecution(se.getJobExecutionId(), se.getId());
            if (se.getJobExecution().isStopping() || se.isTerminateOnly()) {
                sc.setExitStatus(sc.getExitStatus().addExitDescription("Stopp-Signal durch JobExecution"));
                return true;
            }
            return false;
        }
    }

    public class WarteTasklet extends StoppablePrintTextTasklet {
        public WarteTasklet(String text, JobExplorer jobExplorer) { super(text, jobExplorer); }

        @Override
        public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
            super.execute(sc, cc);
            resetStopped();
            StepExecution se = cc.getStepContext().getStepExecution();
            int anzahlStepExecutions = se.getJobExecution().getStepExecutions().size();
            long anzWiederhol = se.getJobParameters().getLong(ANZ_WIEDERHOL, 5L);

            if (anzahlStepExecutions > anzWiederhol) {
                sc.setExitStatus(ExitStatus.FAILED);
            } else {
                for (int i = 0; i < 10 && !isStopped(sc, se); i++) {
                    System.out.print(". ");
                    Thread.sleep(100);
                }
                System.out.println();
            }
            return RepeatStatus.FINISHED;
        }
    }

    // --- Steps ---

    @Bean
    public Step ersterStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("ersterStep", jobRepository)
                .tasklet(new ErstesTasklet("ErstesTasklet"), transactionManager).build();
    }

    @Bean
    public Step zweiterStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("zweiterStep", jobRepository)
                .tasklet(new ZweitesTasklet("ZweitesTasklet"), transactionManager).build();
    }

    @Bean
    public Step warteStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, JobExplorer jobExplorer) {
        return new StepBuilder("warteStep", jobRepository)
                .tasklet(new WarteTasklet("WarteTasklet", jobExplorer), transactionManager).build();
    }

    @Bean
    public Step fehlerbehandlungsStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fehlerbehandlungsStep", jobRepository)
                .tasklet(new StoreTextAndPrintTextTasklet("FehlerbehandlungsStep", ERGEBNIS_FEHLER), transactionManager).build();
    }

    @Bean
    public Step okStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("okStep", jobRepository)
                .tasklet(new StoreTextAndPrintTextTasklet("OkStep", ERGEBNIS_OK), transactionManager).build();
    }

    @Bean
    public Step abschliessenderStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("abschliessenderStep", jobRepository)
                .tasklet(new PrintTextTasklet("AbschliessenderStep"), transactionManager).build();
    }

    // --- Job ---

    @Bean
    public Job meinStoppableDelayedRetryJob(JobRepository jobRepository, JobExecutionListener listener,
                                            Step avoidDuplicateRun, Step ersterStep, Step zweiterStep,
                                            Step warteStep, Step fehlerbehandlungsStep, Step okStep, Step abschliessenderStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .start(avoidDuplicateRun)
                .next(ersterStep)
                .on(ExitStatus.FAILED.getExitCode()).to(warteStep)
                .from(warteStep).on(ExitStatus.FAILED.getExitCode()).to(fehlerbehandlungsStep).next(abschliessenderStep)
                .from(warteStep).on("*").to(ersterStep)
                .from(ersterStep).on("*").to(zweiterStep)
                .from(zweiterStep).on(ExitStatus.FAILED.getExitCode()).to(fehlerbehandlungsStep).next(abschliessenderStep)
                .from(zweiterStep).on("*").to(okStep).next(abschliessenderStep)
                .end()
                .build();
    }

    @Bean
    public JobExecutionListener meinJobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void afterJob(JobExecution je) {
                for (StepExecution se : je.getStepExecutions()) {
                    String sesd = se.getExitStatus().getExitDescription();
                    if (sesd != null && !sesd.isEmpty()) {
                        String jesd = je.getExitStatus().getExitDescription();
                        if (jesd == null || !jesd.contains(sesd)) {
                            je.setExitStatus(je.getExitStatus().addExitDescription(sesd));
                        }
                    }
                }
            }
        };
    }

    // Hilfsklassen für die Tasklets
    public class ErstesTasklet extends PrintTextTasklet {
        public ErstesTasklet(String text) { super(text); }
        @Override
        public RepeatStatus execute(StepContribution sc, ChunkContext cc) throws Exception {
            super.execute(sc, cc);
            StepContext stpCtx = cc.getStepContext();
            int anzahl = stpCtx.getStepExecution().getJobExecution().getStepExecutions().size();
            String okOderFehler = (String) stpCtx.getJobParameters().get(OK_ODER_FEHLER);
            Long anzStepFehlOk = (Long) stpCtx.getJobParameters().get(ANZ_STEP_FEHL_OK);

            boolean fehlerErzwingen = (okOderFehler != null) && !"ok".equalsIgnoreCase(okOderFehler);
            boolean erfolgDurchWiederholung = (anzStepFehlOk != null) && anzahl > anzStepFehlOk;

            if (!fehlerErzwingen || erfolgDurchWiederholung) {
                System.out.println(text + ": ok");
            } else {
                System.out.println(text + ": mit Fehler");
                sc.setExitStatus(ExitStatus.FAILED);
            }
            return RepeatStatus.FINISHED;
        }
    }

    public class ZweitesTasklet extends ErstesTasklet {
        public ZweitesTasklet(String text) { super(text); }
    }
}