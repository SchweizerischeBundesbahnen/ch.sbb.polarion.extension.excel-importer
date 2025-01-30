package ch.sbb.polarion.extension.excel_importer.service;

import com.polarion.core.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class ExportService {

    @SneakyThrows
    public String exportHtmlTable(String sheetName, String htmlTableContentBase64Encoded) {

        if (StringUtils.isEmpty(htmlTableContentBase64Encoded)) {
            throw new IllegalArgumentException("html table content is empty");
        }

        String tableHtml = new String(Base64.getDecoder().decode(htmlTableContentBase64Encoded), StandardCharsets.UTF_8);

        // Parse the HTML string
        Document doc = Jsoup.parse(tableHtml);

        // Select the inner table
        Element table = doc.selectFirst("table");

        if (table == null) {
            throw new IllegalArgumentException("html table not found in the provided html");
        }

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(StringUtils.isEmpty(sheetName) ? "Sheet1" : sheetName);

        Font headerFont = workbook.createFont();
        headerFont.setColor(IndexedColors.BLACK.index);
        headerFont.setBold(true);
        CellStyle headerCellStyle = sheet.getWorkbook().createCellStyle();
        headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.index);
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCellStyle.setFont(headerFont);
        CellStyle regularCellStyle = sheet.getWorkbook().createCellStyle();
        regularCellStyle.setWrapText(true);

        int columnsCount = 0;
        Elements rows = table.select("tr");
        for (int i = 0; i < rows.size(); i++) {
            Row row = sheet.createRow(i);
            row.setHeight((short) -1); // workaround to auto size height
            Element rowElement = rows.get(i);
            Elements cells = rowElement.select("th, td"); // Select both header and data cells
            columnsCount = Math.max(columnsCount, cells.size());

            for (int j = 0; j < cells.size(); j++) {
                Element cellElement = cells.get(j);
                Cell cell = row.createCell(j);
                cell.setCellStyle(cellElement.is("th") ? headerCellStyle : regularCellStyle);
                cell.setCellValue(extractTextWithLineBreaks(cellElement));
            }
        }

        for (int i = 0; i < columnsCount; i++) {
            sheet.setColumnWidth(i, 25 * 300);
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

    @VisibleForTesting
    String extractTextWithLineBreaks(Element element) {
        StringBuilder text = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                text.append(textNode.text());
            } else if (node.nodeName().equals("br")) {
                text.append("\n"); // Convert <br> to newline
            } else if (node instanceof Element childElement) {
                text.append(extractTextWithLineBreaks(childElement)); // Recursively handle nested elements
            }
        }
        return text.toString().trim();
    }
}
