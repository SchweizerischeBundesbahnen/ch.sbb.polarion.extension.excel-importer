package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StyleContextTest {

    @Mock
    XSSFWorkbook workbook;
    @Mock
    XSSFCellStyle cellStyle;
    @Mock
    XSSFFont font;
    @Mock
    Cell cell;
    @Mock
    XSSFDataFormat dataFormat;

    @BeforeEach
    void setup() {
        lenient().when(workbook.createCellStyle()).thenReturn(cellStyle);
        lenient().when(workbook.createFont()).thenReturn(font);
        lenient().when(workbook.createDataFormat()).thenReturn(dataFormat);
        lenient().when(dataFormat.getFormat("@")).thenReturn((short) 42);
    }

    @Test
    void applyStyle_setsCellStyle() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(cell).setCellStyle(any());
    }

    @Test
    void applyStyle_cachesStyle() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);
        ctx.applyStyle(cell, cellData);
        ctx.applyStyle(cell, cellData);
        ctx.applyStyle(cell, cellData);
        ctx.applyStyle(cell, cellData);

        verify(workbook, times(1)).createCellStyle();
    }

    @Test
    void constructStyle_setsTextFormat() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(cellStyle).setDataFormat((short) 42);
    }

    @Test
    void constructStyle_setsBackgroundColor() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(StyleUtil.CSS_PROPERTY_BG_COLOR, "#FF0000"), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(cellStyle).setFillForegroundColor(any(XSSFColor.class));
        verify(cellStyle).setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    @Test
    void constructStyle_setsFontColorAndBold() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(
                Map.of(StyleUtil.CSS_PROPERTY_COLOR, "#00FF00"),
                Map.of(StyleUtil.CSS_PROPERTY_FONT_WEIGHT, "bold"),
                Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(font).setColor(any(XSSFColor.class));
        verify(font).setBold(true);
        verify(cellStyle).setFont(font);
    }

    @Test
    void constructStyle_setsBackgroundColorWithNamedColor() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(StyleUtil.CSS_PROPERTY_BG_COLOR, "red"), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(cellStyle).setFillForegroundColor(any(XSSFColor.class));
        verify(cellStyle).setFillPattern(FillPatternType.SOLID_FOREGROUND);
    }

    @Test
    void constructStyle_setsFontColorWithNamedColor() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(
                Map.of(StyleUtil.CSS_PROPERTY_COLOR, "blue"),
                Map.of(),
                Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(font).setColor(any(XSSFColor.class));
        verify(cellStyle).setFont(font);
    }

    @Test
    void constructStyle_handlesInvalidNamedColor() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(Map.of(StyleUtil.CSS_PROPERTY_BG_COLOR, "notacolor"), Map.of(), Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        // Should not set fill color for invalid color
        verify(cellStyle, never()).setFillForegroundColor(any(XSSFColor.class));
        verify(cellStyle, never()).setFillPattern(any());
    }

    @Test
    void constructStyle_handlesCaseInsensitiveNamedColors() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(
                Map.of(StyleUtil.CSS_PROPERTY_COLOR, "DarkSlateGray"),
                Map.of(),
                Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        verify(font).setColor(any(XSSFColor.class));
        verify(cellStyle).setFont(font);
    }

    @Test
    void constructStyle_setsFontWithOnlyBoldNoColor() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(
                Map.of(),
                Map.of(StyleUtil.CSS_PROPERTY_FONT_WEIGHT, "bold"),
                Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        // Font color should not be set when no color is provided
        verify(font, never()).setColor(any(XSSFColor.class));
        verify(font).setBold(true);
        verify(cellStyle).setFont(font);
    }

    @Test
    void constructStyle_setsFontWithInvalidColorAndBold() {
        StyleContext ctx = new StyleContext(workbook);
        CellData cellData = mock(CellData.class);
        when(cellData.getStyles()).thenReturn(new CellConfig(
                Map.of(StyleUtil.CSS_PROPERTY_COLOR, "notacolor"),
                Map.of(StyleUtil.CSS_PROPERTY_FONT_WEIGHT, "bold"),
                Map.of()));
        when(cellData.getType()).thenReturn(CellData.DataType.TEXT);

        ctx.applyStyle(cell, cellData);

        // Font color should not be set when color is invalid
        verify(font, never()).setColor(any(XSSFColor.class));
        verify(font).setBold(true);
        verify(cellStyle).setFont(font);
    }
}
