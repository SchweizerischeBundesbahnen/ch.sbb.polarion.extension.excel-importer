package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import ch.sbb.polarion.extension.excel_importer.service.CellValue;
import com.polarion.core.util.StringUtils;
import lombok.Data;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Data
public class HtmlTableParser {

    public List<List<CellData>> parse(String htmlTableContentBase64Encoded) {
        if (StringUtils.isEmpty(htmlTableContentBase64Encoded)) {
            throw new IllegalArgumentException("html table content is empty");
        }

        String tableHtml = new String(Base64.getDecoder().decode(htmlTableContentBase64Encoded), StandardCharsets.UTF_8);

        Document doc = Jsoup.parse(tableHtml);
        Element tableElement = doc.selectFirst("table");
        if (tableElement == null) {
            throw new IllegalArgumentException("html table not found in the provided html");
        }

        Map<String, String> tableStyles = StyleUtil.parseStyleAttribute(tableElement.attr("style"));
        Map<String, String> tableAttrs = StyleUtil.parseCustomAttributes(tableElement);

        Elements rows = tableElement.select("tr");
        List<List<CellData>> cellData = new ArrayList<>();
        for (Element rowElement : rows) {
            Map<String, String> rowStyles = StyleUtil.parseStyleAttribute(rowElement.attr("style"));
            Map<String, String> rowAttrs = StyleUtil.parseCustomAttributes(rowElement);
            ArrayList<CellData> rowData = new ArrayList<>();
            cellData.add(rowData);
            Elements cells = rowElement.select("th, td");

            for (Element cellElement : cells) {
                CellValue cellValue = extractTextWithLineBreaks(cellElement);
                rowData.add(CellData.builder()
                        .header(cellElement.is("th"))
                        .type(cellValue.getLink() != null ? CellData.DataType.LINK : CellData.DataType.TEXT)
                        .value(cellValue.getText())
                        .link(cellValue.getLink())
                        .styles(new CellConfig(tableStyles, rowStyles, StyleUtil.parseStyleAttribute(cellElement.attr("style"))))
                        .attrs(new CellConfig(tableAttrs, rowAttrs, StyleUtil.parseCustomAttributes(cellElement)))
                        .build()
                );
            }
        }

        return cellData;
    }

    @VisibleForTesting
    CellValue extractTextWithLineBreaks(Element element) {
        CellValue result = new CellValue();

        if (element.nodeName().equals("a")) {
            String href = element.attr("href");
            result.setLink(href);
            result.setText(element.text());
            return result;
        }

        StringBuilder cellText = new StringBuilder();
        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                cellText.append(textNode.text());
            } else if (node.nodeName().equals("br")) {
                cellText.append("\n"); // Convert <br> to newline
            } else if (node instanceof Element childElement) {
                CellValue childCellValue = extractTextWithLineBreaks(childElement); // Recursively handle nested elements
                cellText.append(childCellValue.getText());
                if (childCellValue.getLink() != null) {
                    result.setLink(childCellValue.getLink());
                }
            }
        }
        result.setText(cellText.toString().trim());
        return result;
    }

}
