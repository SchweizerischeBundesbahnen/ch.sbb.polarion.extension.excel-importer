package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.apache.poi.ss.usermodel.Sheet;
import org.jsoup.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class StyleUtil {

    public static final float CHARACTER_WIDTH_TO_PX_MULTIPLIER = 36.64f;
    public static final float POINT_TO_PX_MULTIPLIER = 15f;

    public static final String CSS_PROPERTY_COLOR = "color";
    public static final String CSS_PROPERTY_BG_COLOR = "background-color";
    public static final String CSS_PROPERTY_FONT_WEIGHT = "font-weight";

    public static final List<String> SUPPORTED_CSS_PROPERTIES = List.of(
            CSS_PROPERTY_COLOR,
            CSS_PROPERTY_BG_COLOR,
            CSS_PROPERTY_FONT_WEIGHT
    );

    public static final String CUSTOM_ATTR_COLUMN_WIDTH = "xlsx-width";
    public static final String CUSTOM_ATTR_ROW_HEIGHT = "xlsx-height";

    public static final short COLUMN_WIDTH_AUTO = -1; // we do not use this value, we call autoSizeColumn() method instead
    public static final short ROW_HEIGHT_AUTO = (short) -1; // '-1' leads to auto size height

    public static final List<String> CUSTOM_ATTRS = List.of(
            CUSTOM_ATTR_COLUMN_WIDTH,
            CUSTOM_ATTR_ROW_HEIGHT
    );

    public Map<String, String> parseStyleAttribute(String attrValue) {
        return Stream.of(StringUtils.getEmptyIfNull(attrValue).toLowerCase().split(";"))
                .map(String::trim)
                .filter(s -> s.contains(":"))
                .map(s -> s.split(":", 2))
                .filter(kv -> SUPPORTED_CSS_PROPERTIES.contains(kv[0].trim()))
                .collect(Collectors.toMap(
                        kv -> kv[0].trim(),
                        kv -> kv[1].trim(),
                        (existing, replacement) -> replacement // Handles duplicate keys
                ));
    }

    public Map<String, String> parseCustomAttributes(Node htmlNode) {
        Map<String, String> result = new HashMap<>();
        for (String attr : CUSTOM_ATTRS) {
            String attrValue = htmlNode.attr(attr);
            if (!StringUtils.isEmpty(attrValue)) {
                result.put(attr, attrValue);
            }
        }
        return result;
    }

    /**
     * Bold - it's when the value is either 'bold' or integer value >= 700
     */
    public boolean isBold(String fontWeight) {
        if ("bold".equals(fontWeight)) {
            return true;
        }
        try {
            int weight = Integer.parseInt(fontWeight);
            return weight >= 700;
        } catch (Exception e) {
            return false;
        }
    }

    public static void adjustColumnWidth(int columnIndex, CellData data, Sheet sheet) {
        int columnWidth = StyleUtil.getColumnWidthForCell(data);
        if (StyleUtil.COLUMN_WIDTH_AUTO == columnWidth) {
            sheet.autoSizeColumn(columnIndex);
        } else {
            sheet.setColumnWidth(columnIndex, columnWidth);
        }
    }

    public int getColumnWidthForCell(CellData data) {
        String columnWidthAttrValue = data.getAttrs().get().get(StyleUtil.CUSTOM_ATTR_COLUMN_WIDTH);
        return !StringUtils.isEmpty(columnWidthAttrValue) ? (int) (CHARACTER_WIDTH_TO_PX_MULTIPLIER * Integer.parseInt(columnWidthAttrValue)) : COLUMN_WIDTH_AUTO;
    }

    public short getRowHeightForCell(CellData data) {
        String rowHeightAttrValue = data.getAttrs().row().get(StyleUtil.CUSTOM_ATTR_ROW_HEIGHT);
        return (short) (!StringUtils.isEmpty(rowHeightAttrValue) ? (POINT_TO_PX_MULTIPLIER * Short.parseShort(rowHeightAttrValue)) : ROW_HEIGHT_AUTO);
    }

}
