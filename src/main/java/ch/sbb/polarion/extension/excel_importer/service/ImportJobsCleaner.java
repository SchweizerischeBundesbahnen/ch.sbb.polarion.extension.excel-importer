package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.utils.PropertiesUtility;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ImportJobsCleaner {

    private static Future<?> cleaningJob;

    private ImportJobsCleaner() {
    }

    public static synchronized void startCleaningJob() {
        if (cleaningJob != null) {
            return;
        }

        int finishedJobTimeout = new PropertiesUtility().getFinishedJobTimeout();

        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        cleaningJob = executorService.scheduleWithFixedDelay(
                () -> ImportJobsService.cleanupExpiredJobs(finishedJobTimeout),
                0,
                finishedJobTimeout,
                TimeUnit.MINUTES);
    }
}
