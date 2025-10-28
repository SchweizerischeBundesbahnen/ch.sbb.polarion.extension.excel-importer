package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellData;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.HtmlTableParser;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleContext;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleUtil;
import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.util.List;

public class ExportService {

    @SneakyThrows
    public byte[] exportHtmlTable(String sheetName, String htmlTableContentBase64Encoded) {

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
                setCellValue(cell, data, workbook, sheet);

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

        return outputStream.toByteArray();
    }

    private void setCellValue(Cell cell, CellData data, XSSFWorkbook workbook, Sheet sheet) {
        switch (data.getType()) {
            case TEXT -> cell.setCellValue(data.getValue().getText());
            case LINK -> {
                cell.setCellValue(data.getValue().getText());
                Hyperlink hyperlink = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                hyperlink.setAddress(data.getValue().getLink());
                cell.setHyperlink(hyperlink);
            }
            case IMAGE -> {
                int pictureIdx = workbook.addPicture(data.getValue().getImage(), Workbook.PICTURE_TYPE_PNG);
                Drawing<?> drawing = sheet.createDrawingPatriarch();
                ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();

                // Set the image anchor to the top-left corner of the cell
                anchor.setCol1(cell.getColumnIndex());
                anchor.setRow1(cell.getRowIndex());
                // Set the anchor's bottom-right corner (for resizing purposes)
                anchor.setCol2(cell.getColumnIndex() + 1);
                anchor.setRow2(cell.getRowIndex() + 1);

                Picture pict = drawing.createPicture(anchor, pictureIdx);
                pict.resize(1); // Resize proportionally to the cell size
            }
        }
    }

}
