package ch.sbb.polarion.extension.excel_importer.utils;

import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertiesUtility {
    public static final String JOBS_PROPERTIES_FILE = "/import-jobs.properties";
    private static final String TIMEOUT_FINISHED_JOBS = "jobs.timeout.finished.minutes";
    private static final String TIMEOUT_IN_PROGRESS_JOBS = "jobs.timeout.in-progress.minutes";

    private final Properties jobsProps;

    public PropertiesUtility() {
        jobsProps = loadProperties(JOBS_PROPERTIES_FILE);
    }

    public int getInProgressJobTimeout() {
        return getIntProperty(TIMEOUT_IN_PROGRESS_JOBS);
    }

    public int getFinishedJobTimeout() {
        return getIntProperty(TIMEOUT_FINISHED_JOBS);
    }

    @VisibleForTesting
    int getIntProperty(String propName) {
        String propValue = jobsProps.getProperty(propName);
        if (propValue == null) {
            throw new IllegalArgumentException("Missing property: " + propName);
        }
        return Integer.parseInt(propValue);
    }

    @VisibleForTesting
    Properties loadProperties(String resourcePath) {
        try (InputStream propsInputStream = PropertiesUtility.class.getResourceAsStream(resourcePath)) {
            if (propsInputStream == null) {
                throw new IllegalArgumentException("Properties file is not found: " + resourcePath);
            }
            Properties props = new Properties();
            props.load(propsInputStream);
            return props;
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot load properties file: " + JOBS_PROPERTIES_FILE, e);
        }
    }
}
