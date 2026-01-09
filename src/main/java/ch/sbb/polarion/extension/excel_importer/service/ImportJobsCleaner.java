package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.utils.PropertiesUtility;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class ImportJobsCleaner {

    private static ScheduledExecutorService executorService;

    private ImportJobsCleaner() {
    }

    public static synchronized void startCleaningJob() {
        if (executorService != null) {
            return;
        }

        int finishedJobTimeout = new PropertiesUtility().getFinishedJobTimeout();

        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleWithFixedDelay(
                () -> ImportJobsService.cleanupExpiredJobs(finishedJobTimeout),
                0,
                finishedJobTimeout,
                TimeUnit.MINUTES);
    }

    public static synchronized void stopCleaningJob() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }
}
