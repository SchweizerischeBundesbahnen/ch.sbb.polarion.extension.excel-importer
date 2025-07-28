package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class StyleContext {
    private final XSSFWorkbook workbook;
    private final short textFormat;
    private final Map<String, XSSFCellStyle> styleCache = new HashMap<>();

    public StyleContext(XSSFWorkbook workbook) {
        this.workbook = workbook;
        this.textFormat = workbook.createDataFormat().getFormat("@");
    }

    public void applyStyle(Cell cell, CellData cellData) {
        Map<String, String> styles = cellData.getStyles().merged();
        String styleKey = getCacheKey(styles, cellData);
        CellStyle cellStyle = styleCache.computeIfAbsent(styleKey, key -> constructStyle(styles, cellData));
        cell.setCellStyle(cellStyle);
    }

    private XSSFCellStyle constructStyle(Map<String, String> styles, CellData cellData) {
        XSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setWrapText(true);

        // disable Excel's auto-formatting, e.g. '001' -> '1'
        if (CellData.DataType.TEXT.equals(cellData.getType())) {
            cellStyle.setDataFormat(textFormat);
        }

        String bgColor = styles.get(StyleUtil.CSS_PROPERTY_BG_COLOR);
        if (bgColor != null) {
            if (bgColor.matches("^#[0-9A-Fa-f]{6}$")) {
                cellStyle.setFillForegroundColor(new XSSFColor(Color.decode(bgColor), new DefaultIndexedColorMap()));
            }
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }

        if (styles.containsKey(StyleUtil.CSS_PROPERTY_FONT_WEIGHT) || styles.containsKey(StyleUtil.CSS_PROPERTY_COLOR)) {
            XSSFFont font = workbook.createFont();
            if (styles.getOrDefault(StyleUtil.CSS_PROPERTY_COLOR, "").matches("^#[0-9A-Fa-f]{6}$")) {
                font.setColor(new XSSFColor(Color.decode(styles.get(StyleUtil.CSS_PROPERTY_COLOR)), new DefaultIndexedColorMap()));
            }
            font.setBold(StyleUtil.isBold(styles.get(StyleUtil.CSS_PROPERTY_FONT_WEIGHT)));
            cellStyle.setFont(font);
        }

        return cellStyle;
    }

    private String getCacheKey(Map<String, String> styles, CellData cellData) {
        return cellData.getType().name() + ":" + styles.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())  // sort keys
                .map(entry -> "%s=%s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining(","));
    }


}
