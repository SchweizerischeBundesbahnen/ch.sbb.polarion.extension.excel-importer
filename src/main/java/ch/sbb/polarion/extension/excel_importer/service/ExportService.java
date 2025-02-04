package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellData;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.HtmlTableParser;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleContext;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleUtil;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;

public class ExportService {

    @SneakyThrows
    public String exportHtmlTable(String sheetName, String htmlTableContentBase64Encoded) {

        List<List<CellData>> cellsData = new HtmlTableParser().parse(htmlTableContentBase64Encoded);

        XSSFWorkbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(StringUtils.isEmpty(sheetName) ? "Sheet1" : sheetName);

        StyleContext styleContext = new StyleContext(workbook);
        for (int i = 0; i < cellsData.size(); i++) {
            Row row = sheet.createRow(i);
            List<CellData> cells = cellsData.get(i);
            row.setHeight(StyleUtil.getRowHeightForCell(cells.get(0)));

            for (int j = 0; j < cells.size(); j++) {
                CellData data = cells.get(j);
                Cell cell = row.createCell(j);
                styleContext.applyStyle(cell, data);
                setCellValue(cell, data, workbook);

                if (data.isHeader()) {
                    StyleUtil.adjustColumnWidth(j, data, sheet);
                }
            }
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            workbook.write(outputStream);
        } finally {
            workbook.close();
        }

        byte[] excelBytes = outputStream.toByteArray();
        return Base64.getEncoder().encodeToString(excelBytes);
    }

    private void setCellValue(Cell cell, CellData data, XSSFWorkbook workbook) {
        switch (data.getType()) {
            case TEXT -> cell.setCellValue((String) data.getValue());
            case LINK -> {
                cell.setCellValue((String) data.getValue());
                Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(data.getLink());
                cell.setHyperlink(hyperlink);
            }
            case IMAGE -> throw new IllegalStateException("Not implemented: " + data.getType());
        }
    }

}
