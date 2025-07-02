package ch.sbb.polarion.extension.excel_importer.utils;

import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static ch.sbb.polarion.extension.excel_importer.utils.PropertiesUtility.JOBS_PROPERTIES_FILE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockConstruction;

class PropertiesUtilityTest {

    @Test
    void testGetProperties() {
        PropertiesUtility propertiesUtility = new PropertiesUtility();
        assertThat(propertiesUtility.getFinishedJobTimeout()).isEqualTo(30);
        assertThat(propertiesUtility.getInProgressJobTimeout()).isEqualTo(60);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> propertiesUtility.getIntProperty("non.existent.property")
        );
        assertEquals("Missing property: non.existent.property", exception.getMessage());
    }

    @Test
    void testLoadPropertiesFile() {
        PropertiesUtility propertiesUtility = new PropertiesUtility();
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> propertiesUtility.loadProperties("non.existent.file.properties")
        );
        assertEquals("Properties file is not found: non.existent.file.properties", exception.getMessage());

        try (MockedConstruction<Properties> propertiesMockedConstruction = mockConstruction(Properties.class,
                (mock, context) -> doThrow(new IOException()).when(mock).load(any(InputStream.class)))) {
            exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> propertiesUtility.loadProperties(JOBS_PROPERTIES_FILE)
            );
            assertEquals("Cannot load properties file: /import-jobs.properties", exception.getMessage());
        }
    }

}
