package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import ch.sbb.polarion.extension.excel_importer.service.CellValue;
import ch.sbb.polarion.extension.excel_importer.utils.PolarionUtils;
import com.polarion.core.util.StringUtils;
import lombok.Data;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Data
public class HtmlTableParser {

    private static final String HTML_TAG_A = "a";
    private static final String HTML_TAG_BR = "br";
    private static final String HTML_TAG_TABLE = "table";
    private static final String HTML_TAG_TH = "th";
    private static final String HTML_TAG_TR = "tr";
    private static final String HTML_TAG_TD = "td";
    private static final String HTML_ATTR_STYLE = "style";
    private static final String HTML_ATTR_HREF = "href";
    private static final String HTML_TAG_IMG = "img";
    private static final String HTML_ATTR_SRC = "src";

    public List<List<CellData>> parse(String htmlTableContentBase64Encoded) {
        if (StringUtils.isEmpty(htmlTableContentBase64Encoded)) {
            throw new IllegalArgumentException("html table content is empty");
        }

        String tableHtml = new String(Base64.getDecoder().decode(htmlTableContentBase64Encoded), StandardCharsets.UTF_8);

        Document doc = Jsoup.parse(tableHtml);
        Element tableElement = doc.selectFirst(HTML_TAG_TABLE);
        if (tableElement == null) {
            throw new IllegalArgumentException("html table not found in the provided html");
        }

        Map<String, String> tableStyles = StyleUtil.parseStyleAttribute(tableElement.attr(HTML_ATTR_STYLE));
        Map<String, String> tableAttrs = StyleUtil.parseCustomAttributes(tableElement);

        Elements rows = tableElement.select(HTML_TAG_TR);
        List<List<CellData>> cellData = new ArrayList<>();
        for (Element rowElement : rows) {
            Map<String, String> rowStyles = StyleUtil.parseStyleAttribute(rowElement.attr(HTML_ATTR_STYLE));
            Map<String, String> rowAttrs = StyleUtil.parseCustomAttributes(rowElement);
            ArrayList<CellData> rowData = new ArrayList<>();
            cellData.add(rowData);
            Elements cells = rowElement.select("%s, %s".formatted(HTML_TAG_TH, HTML_TAG_TD));

            for (Element cellElement : cells) {
                CellValue cellValue = extractTextWithLineBreaks(cellElement);
                rowData.add(CellData.builder()
                        .header(cellElement.is(HTML_TAG_TH))
                        .type(cellValue.getType())
                        .value(cellValue.getValue())
                        .link(cellValue.getLink())
                        .styles(new CellConfig(tableStyles, rowStyles, StyleUtil.parseStyleAttribute(cellElement.attr(HTML_ATTR_STYLE))))
                        .attrs(new CellConfig(tableAttrs, rowAttrs, StyleUtil.parseCustomAttributes(cellElement)))
                        .build()
                );
            }
        }

        return cellData;
    }

    @SneakyThrows
    @VisibleForTesting
    CellValue extractTextWithLineBreaks(Element element) {
        CellValue result = new CellValue();

        String nodeName = element.nodeName();
        switch (nodeName) {
            case HTML_TAG_A:
                handleAnchorElement(element, result);
                break;
            case HTML_TAG_IMG:
                handleImageElement(element, result);
                break;
            default:
                processChildNodes(element, result);
                break;
        }

        return result;
    }

    private void handleAnchorElement(Element element, CellValue result) {
        String href = element.attr(HTML_ATTR_HREF);
        result.setLink(href);
        result.setText(element.text());
    }

    private void handleImageElement(Element element, CellValue result) throws IOException {
        String src = element.attr(HTML_ATTR_SRC);
        URL url = PolarionUtils.getAbsoluteUrl(src);
        byte[] image = IOUtils.toByteArray(url.openStream());
        result.setImage(image);
    }

    private void processChildNodes(Element element, CellValue result) {
        StringBuilder cellText = new StringBuilder();

        for (Node node : element.childNodes()) {
            if (node instanceof TextNode textNode) {
                cellText.append(textNode.text());
            } else if (node.nodeName().equals(HTML_TAG_BR)) {
                cellText.append("\n"); // Convert <br> to newline
            } else if (node instanceof Element childElement) {
                CellValue childCellValue = extractTextWithLineBreaks(childElement); // Recursively handle nested elements
                updateWithChildValues(childCellValue, result, cellText);
            }
        }

        result.setText(cellText.toString());
    }

    private void updateWithChildValues(CellValue childCellValue, CellValue result, StringBuilder cellText) {
        if (childCellValue.getText() != null) {
            cellText.append(childCellValue.getText());
        }
        if (childCellValue.getLink() != null) {
            result.setLink(childCellValue.getLink());
        }
        if (childCellValue.getImage() != null) {
            result.setImage(childCellValue.getImage());
        }
    }
}
