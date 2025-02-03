package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import com.polarion.core.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jsoup.nodes.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class StyleUtil {

    public static final float CHARACTER_WIDTH_TO_PX_MULTIPLIER = 36.64f;

    public static final String CSS_PROPERTY_COLOR = "color";
    public static final String CSS_PROPERTY_BG_COLOR = "background-color";
    public static final String CSS_PROPERTY_FONT_WEIGHT = "font-weight";

    public static final List<String> SUPPORTED_CSS_PROPERTIES = List.of(
            CSS_PROPERTY_COLOR,
            CSS_PROPERTY_BG_COLOR,
            CSS_PROPERTY_FONT_WEIGHT
    );

    public static final String CUSTOM_ATTR_COLUMN_WIDTH = "xlsx_width";

    public static final List<String> CUSTOM_ATTRS = List.of(
            CUSTOM_ATTR_COLUMN_WIDTH
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

}
