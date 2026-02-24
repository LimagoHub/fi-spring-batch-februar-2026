package de.fi.tag2_06partitionierer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class FileMergingTasklet implements Tasklet {

    private static final Logger log = LoggerFactory.getLogger(FileMergingTasklet.class);

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
        // Pfad zum Output-Ordner (relativ zum Projektwurzelverzeichnis)
        Path outputDirPath = Paths.get("outputs");
        File outputDir = outputDirPath.toFile();

        File finalFile = new File("all_persons_final.csv");
        Files.deleteIfExists(finalFile.toPath());
        Files.createFile(finalFile.toPath());

        if (!outputDir.exists() || !outputDir.isDirectory()) {
            log.error("Verzeichnis 'outputs' wurde nicht gefunden! Absoluter Pfad: {}", outputDir.getAbsolutePath());
            return RepeatStatus.FINISHED;
        }

        // Suche Dateien im 'outputs' Ordner
        File[] files = outputDir.listFiles((d, name) -> name.startsWith("persons_part_") && name.endsWith(".csv"));

        if (files == null || files.length == 0) {
            log.warn("Keine Dateien in {} gefunden!", outputDir.getAbsolutePath());
            return RepeatStatus.FINISHED;
        }

        log.info("Gefunden: {} Dateien im Ordner {}", files.length, outputDir.getName());
        Arrays.sort(files);

        for (File f : files) {
            log.info("Kopiere Inhalt von: {} ({} Bytes)", f.getName(), f.length());
            byte[] content = Files.readAllBytes(f.toPath());
            Files.write(finalFile.toPath(), content, StandardOpenOption.APPEND);
        }

        log.info("Zusammenführung erfolgreich. Ziel: {}", finalFile.getAbsolutePath());
        return RepeatStatus.FINISHED;
    }
}