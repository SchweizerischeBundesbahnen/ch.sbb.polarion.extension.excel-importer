package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CellConfigTest {

    @Test
    void testMergedForEmptyMaps() {
        CellConfig config = new CellConfig(Map.of(), Map.of(), Map.of());

        assertTrue(config.merged().isEmpty());
    }

    @Test
    void testMergedWithSingleMap() {
        CellConfig config = new CellConfig(Map.of("table1", "style1"), Map.of(), Map.of());

        Map<String, String> merged = config.merged();
        assertEquals(1, merged.size());
        assertEquals("style1", merged.get("table1"));
    }

    @Test
    void testMergedWithMultipleMaps() {
        CellConfig config = new CellConfig(Map.of("table1", "style1"), Map.of("row1", "style2"), Map.of("cell1", "style3"));

        Map<String, String> merged = config.merged();
        assertEquals(3, merged.size());
        assertEquals("style1", merged.get("table1"));
        assertEquals("style2", merged.get("row1"));
        assertEquals("style3", merged.get("cell1"));
    }

    @Test
    void testMergedStylesWithOverlappingKeys() {
        CellConfig config = new CellConfig(
                Map.of("table11", "style1", "table12", "style1"),
                Map.of("table11", "style2"),
                Map.of("table12", "style3")
        );

        Map<String, String> merged = config.merged();
        assertEquals(2, merged.size());
        assertEquals("style2", merged.get("table11"));
        assertEquals("style3", merged.get("table12"));
    }

}
