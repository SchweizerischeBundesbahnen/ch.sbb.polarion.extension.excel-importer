package ch.sbb.polarion.extension.excel_importer.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
    }

}
